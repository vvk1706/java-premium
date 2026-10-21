package com.dlis.licensing.client;

import com.dlis.common.RemoteServiceException;
import com.dlis.common.ServiceClients;
import com.dlis.licensing.domain.CandidateRef;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.Optional;

/** REST implementation; base URL comes from CANDIDATE_SERVICE_URL (ConfigMap). */
@ApplicationScoped
public class RestCandidateGateway implements CandidateGateway {

    static final String ENV_URL = "CANDIDATE_SERVICE_URL";
    static final String DEFAULT_URL = "http://dlis-candidate-service:9080/dlis/api/v1";

    @Override
    public Optional<CandidateRef> findById(Long candidateId) {
        String url = ServiceClients.baseUrl(ENV_URL, DEFAULT_URL) + "/candidates/" + candidateId;
        Client client = ServiceClients.newClient();
        try (Response r = client.target(url).request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, ServiceClients.authHeader())
                .get()) {
            if (r.getStatus() == 404) {
                return Optional.empty();
            }
            if (r.getStatus() != 200) {
                throw new RemoteServiceException(
                        "dlis-candidate-service answered HTTP " + r.getStatus());
            }
            return Optional.of(parse(r.readEntity(String.class)));
        } catch (ProcessingException e) {
            throw new RemoteServiceException("dlis-candidate-service unreachable", e);
        } finally {
            client.close();
        }
    }

    static CandidateRef parse(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject o = reader.readObject();
            LocalDate dob = o.isNull("dateOfBirth") ? null : LocalDate.parse(o.getString("dateOfBirth"));
            return new CandidateRef(
                    (long) o.getJsonNumber("candidateId").longValue(),
                    dob,
                    o.getBoolean("active", false));
        }
    }
}
