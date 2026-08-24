package fixit.dreams.sync;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

// Alle metoder er BLOKERENDE netværkskald - skal kaldes fra en baggrundstråd, aldrig fra
// FX-tråden. Oversætter mellem almindelig JSON (via FirestoreJson) og Firestores REST-format.
public class FirestoreClient {
    private static final String BASE_URL =
            "https://firestore.googleapis.com/v1/projects/" + FirebaseConfig.PROJECT_ID + "/databases/(default)/documents/";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Henter ét dokument. Tomt Optional hvis det ikke findes (404) - det er ikke en fejl i sig selv.
    public Optional<JsonNode> getDocument(String idToken, String docPath) throws FirestoreException {
        try {
            HttpRequest request = authorizedRequest(idToken, BASE_URL + docPath).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            JsonNode json = SyncObjectMapper.INSTANCE.readTree(response.body());
            if (response.statusCode() != 200) {
                throw new FirestoreException(extractErrorMessage(json), response.statusCode());
            }
            return Optional.of(FirestoreJson.fromDocument(json));
        } catch (IOException e) {
            throw new FirestoreException("NETVÆRKSFEJL", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("NETVÆRKSFEJL", e);
        }
    }

    // Opretter eller overskriver (upsert) ét dokument fuldstændigt med de givne felter.
    public void patchDocument(String idToken, String docPath, JsonNode plainFields) throws FirestoreException {
        try {
            String body = FirestoreJson.toDocument(plainFields).toString();
            HttpRequest request = authorizedRequest(idToken, BASE_URL + docPath)
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                JsonNode json = SyncObjectMapper.INSTANCE.readTree(response.body());
                throw new FirestoreException(extractErrorMessage(json), response.statusCode());
            }
        } catch (IOException e) {
            throw new FirestoreException("NETVÆRKSFEJL", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("NETVÆRKSFEJL", e);
        }
    }

    // Henter ALLE dokumenter i en samling (fx users/{uid}/dreams), nøglet på deres dokument-id
    // (sidste led i Firestores fulde ressourcenavn - svarer til vores lokale dream-id).
    // Sider gennem resultatet med nextPageToken, da denne bruger reelt har 800+ drømme, hvilket
    // overstiger Firestores standard sidestørrelse for documents:list.
    public Map<String, JsonNode> listDocuments(String idToken, String collectionPath) throws FirestoreException {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        String pageToken = null;
        try {
            do {
                String url = BASE_URL + collectionPath + "?pageSize=300"
                        + (pageToken != null ? "&pageToken=" + pageToken : "");
                HttpRequest request = authorizedRequest(idToken, url).GET().build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode json = SyncObjectMapper.INSTANCE.readTree(response.body());

                if (response.statusCode() != 200) {
                    throw new FirestoreException(extractErrorMessage(json), response.statusCode());
                }

                for (JsonNode doc : json.path("documents")) {
                    String fullName = doc.path("name").asText();
                    String id = fullName.substring(fullName.lastIndexOf('/') + 1);
                    result.put(id, FirestoreJson.fromDocument(doc));
                }

                JsonNode nextToken = json.get("nextPageToken");
                pageToken = (nextToken != null) ? nextToken.asText() : null;
            } while (pageToken != null);
        } catch (IOException e) {
            throw new FirestoreException("NETVÆRKSFEJL", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("NETVÆRKSFEJL", e);
        }
        return result;
    }

    private HttpRequest.Builder authorizedRequest(String idToken, String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + idToken)
                .header("Content-Type", "application/json");
    }

    private String extractErrorMessage(JsonNode json) {
        return json.path("error").path("message").asText("UKENDT_FEJL");
    }
}
