# ===========================================================================
# DLIS Licensing workflow service — Liberty image
#
# One Dockerfile, three architectures. The WAR and the DB2 JCC jar are architecture
# independent, so the Maven build runs once (see deploy/ansible, role "build") and this
# file only layers them on the architecture-specific Liberty base image:
#
#   podman build --platform linux/amd64,linux/s390x --manifest <registry>/dlis-licensing-service:<tag> .
#
# Build context: dlis-licensing-service/  (after `mvn package`).
# ===========================================================================
ARG LIBERTY_IMAGE=icr.io/appcafe/websphere-liberty:25.0.0.12-full-java17-openj9-ubi
FROM ${LIBERTY_IMAGE}

ARG SERVICE=dlis-licensing-service
ARG VERSION=1.0.0

LABEL org.opencontainers.image.title="${SERVICE}" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.description="DLIS Licensing workflow service on WebSphere Liberty"

COPY --chown=1001:0 src/main/liberty/config/server.xml  /config/server.xml
COPY --chown=1001:0 src/main/liberty/config/jvm.options /config/jvm.options
COPY --chown=1001:0 target/liberty-shared/db2jcc4.jar   /opt/ibm/wlp/usr/shared/resources/db2jcc/db2jcc4.jar
COPY --chown=1001:0 target/${SERVICE}.war               /config/apps/${SERVICE}.war

# Pre-populate the OpenJ9 shared class cache and fix group permissions for the
# arbitrary UID that OpenShift assigns.
RUN configure.sh

USER 1001
EXPOSE 9080

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -sf http://localhost:9080/health/ready || exit 1
