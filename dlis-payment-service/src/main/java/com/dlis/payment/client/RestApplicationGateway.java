package com.dlis.payment.client;

import com.dlis.common.RemoteServiceException;
import com.dlis.common.ServiceClients;
import com.dlis.common.ServiceResult;
import com.dlis.payment.domain.ApplicationRef;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.util.Optional;

/** REST implementation; base URL comes from LICENSING_SERVICE_URL (ConfigMap). */
@ApplicationScoped
public class RestApplicationGateway implements ApplicationGateway {

    static final String ENV_URL = "LICENSING_SERVICE_URL";
    static final String DEFAULT_URL = "http://dlis-licensing-service:9080/dlis/api/v1";

    @Override
    public Optional<ApplicationRef> findById(Long applicationId) {
        String url = ServiceClients.baseUrl(ENV_URL, DEFAULT_URL) + "/applications/" + applicationId;
        Client client = ServiceClients.newClient();
        try (Response r = client.target(url).request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, ServiceClients.authHeader())
                .get()) {
            if (r.getStatus() == 404) {
                return Optional.empty();
            }
            if (r.getStatus() != 200) {
                throw new RemoteServiceException("dlis-licensing-service answered HTTP " + r.getStatus());
            }
            return Optional.of(parse(r.readEntity(String.class)));
        } catch (ProcessingException e) {
            throw new RemoteServiceException("dlis-licensing-service unreachable", e);
        } finally {
            client.close();
        }
    }

    @Override
    public ServiceResult<Void> paymentRecorded(Long applicationId, String paymentReference, String recordedBy) {
        String url = ServiceClients.baseUrl(ENV_URL, DEFAULT_URL)
                + "/applications/" + applicationId + "/payment-recorded";
        String body = Json.createObjectBuilder()
                .add("paymentReference", paymentReference)
                .add("recordedBy", recordedBy == null ? "API" : recordedBy)
                .build().toString();
        Client client = ServiceClients.newClient();
        try (Response r = client.target(url).request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, ServiceClients.authHeader())
                .post(Entity.json(body))) {
            if (r.getStatus() == 200) {
                return ServiceResult.ok(null, "OK");
            }
            if (r.getStatus() == 404 || r.getStatus() == 422) {
                return parseError(r.readEntity(String.class), r.getStatus());
            }
            throw new RemoteServiceException("dlis-licensing-service answered HTTP " + r.getStatus());
        } catch (ProcessingException e) {
            throw new RemoteServiceException("dlis-licensing-service unreachable", e);
        } finally {
            client.close();
        }
    }

    static ApplicationRef parse(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject o = reader.readObject();
            return new ApplicationRef(
                    o.getJsonNumber("applicationId").longValue(),
                    o.getString("licenseType", null),
                    "P".equals(o.getString("historyChkStatus", "U")),
                    "P".equals(o.getString("paymentStatus", "U")));
        }
    }

    static ServiceResult<Void> parseError(String json, int httpStatus) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject o = reader.readObject();
            int rc = o.getInt("returnCode", httpStatus == 404 ? ServiceResult.RC_NOT_FOUND : ServiceResult.RC_ERROR);
            return ServiceResult.error(rc, o.getString("message", "REJECTED BY LICENSING SERVICE"));
        } catch (RuntimeException e) {
            return ServiceResult.error(httpStatus == 404 ? ServiceResult.RC_NOT_FOUND : ServiceResult.RC_ERROR,
                    "REJECTED BY LICENSING SERVICE");
        }
    }
}
