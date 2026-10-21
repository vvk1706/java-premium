# DLIS on OpenShift (x86 and IBM Z) — Ansible pipeline

Builds, packages and rolls out the three DLIS Liberty microservices
(`dlis-candidate-service`, `dlis-licensing-service`, `dlis-payment-service`) plus an nginx API gateway
to one or more OpenShift clusters — x86, IBM Z (s390x) or one mixed cluster.

> **Status:** syntax-checked, `ansible-lint` (production profile) clean, and every rendered manifest
> passes `kubernetes-validate`. It has **not** been run against a live OpenShift/Z cluster or Db2.
> See `reports/05-liberty-openshift-modernization.html`.

## Stages (`site.yml`)

| # | Play / role | What it does |
|---|---|---|
| 1 | `dlis_preflight` | checks tools on the pipeline host, then cluster access and variables |
| 2 | `dlis_build` | `mvn verify` (or `package -DskipTests`) once; the WARs are architecture independent |
| 3 | `dlis_images` | `podman build --platform linux/amd64,linux/s390x --manifest`, push, verify the manifest list |
| 4 | `dlis_database` | optional, additive DDL via the Db2 CLP (host group `db2_admin`) |
| 5 | `dlis_openshift` | render, apply, roll out candidate → licensing → payment, gateway, HPA/PDB, NetworkPolicies, Route; roll back on failure |
| 6 | `dlis_verify` | Deployments ready, pod architectures, `/health/ready`, gateway routing; optional end-to-end smoke test |

Clusters are Ansible hosts (`serial: 1`): the first one listed in the inventory is the canary (x86), then Z.

## Prerequisites

Pipeline host: Ansible ≥ 2.15, `ansible-galaxy collection install -r requirements.yml`,
Python `kubernetes`, JDK 17 + Maven, `podman` (with qemu-user-static for the foreign architecture, or a
remote builder) and `skopeo`. `db2jcc4.jar` must be resolvable by Maven (see the service POMs).
Clusters: OpenShift 4.12+, a namespace you may create, a reachable Db2 for LUW or z/OS.

## Usage

```bash
cd deploy/ansible
ansible-galaxy collection install -r requirements.yml

# 1. inventory: copy inventories/example to inventories/<env>; edit hosts.yml, host_vars/<cluster>.yml, group_vars/all/main.yml
# 2. secrets: copy vault.example.yml to vault.yml and encrypt it
cp inventories/example/group_vars/all/vault.example.yml inventories/<env>/group_vars/all/vault.yml
ansible-vault encrypt inventories/<env>/group_vars/all/vault.yml

# offline: render the manifests into deploy/.rendered/ (no cluster, no build) — contains secrets, git-ignored
ansible-playbook -i inventories/example render.yml

# the full pipeline
ansible-playbook -i inventories/<env> site.yml --ask-vault-pass

# one cluster / one stage
ansible-playbook -i inventories/<env> site.yml --limit ocp-z --tags deploy,verify
ansible-playbook -i inventories/<env> site.yml -e dlis_build_images=false -e dlis_image_tag=1.0.0

# rollback to the previous ReplicaSet
ansible-playbook -i inventories/<env> rollback.yml --limit ocp-z
```

`inventories/example/group_vars/all/vault.yml` holds **dummy** values so the example inventory renders. Never reuse it.

## Switches (`-e`)

| Variable | Default | Effect |
|---|---|---|
| `dlis_deployment_mode` | `unified` | `unified`: one multi-arch Deployment per service (node affinity + topology spread over `kubernetes.io/arch`). `per-arch`: one Deployment per service and architecture, scaled independently via `dlis_replicas_by_arch` |
| `dlis_node_arches` | `[amd64, s390x]` | architectures a cluster may schedule on (override per cluster in `host_vars`) |
| `dlis_run_unit_tests` | `true` | `mvn verify` vs `mvn package -DskipTests` |
| `dlis_build_images` | `true` | `false` reuses images already pushed for `dlis_image_tag` |
| `dlis_run_db_migration` | `false` | apply the DDL (not idempotent — first install or a new additive script only) |
| `dlis_rollback_on_failure` | `true` | roll back Deployments that existed before the run if the rollout fails |
| `dlis_render_only` | `false` | render and stop |
| `dlis_create_servicemonitor` | `false` | needs the Prometheus Operator CRDs |
| `dlis_e2e_smoke` | `false` | writes a test candidate/application/payment into the **real** database |

## Limitations

* Not exercised against a live cluster: the `kubernetes.core` apply/wait/rollback behaviour, and the
  `dlis_images`, `dlis_database` and `dlis_verify` roles.
* `basicRegistry` users come from a Secret; replace with LDAP (or SAF on Z) for production.
* The legacy monolith, the CICS bridge and the shared `DLIS` schema are untouched — they remain the rollback path.
* Rendered manifests contain secrets and are deleted after apply unless `dlis_keep_rendered=true`.
