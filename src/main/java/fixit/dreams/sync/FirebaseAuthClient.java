package fixit.dreams.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

// Alle metoder er BLOKERENDE netværkskald (java.net.http.HttpClient) - skal kaldes fra en
// baggrundstråd, aldrig direkte fra FX-tråden. Følger samme mønster som GITHUBUpdater.
public class FirebaseAuthClient {
    private static final String SIGNUP_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + FirebaseConfig.WEB_API_KEY;
    private static final String SIGNIN_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + FirebaseConfig.WEB_API_KEY;
    private static final String REFRESH_URL =
            "https://securetoken.googleapis.com/v1/token?key=" + FirebaseConfig.WEB_API_KEY;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public AuthResult signUp(String email, String password) throws FirebaseAuthException {
        return callIdentityToolkit(SIGNUP_URL, email, password);
    }

    public AuthResult signIn(String email, String password) throws FirebaseAuthException {
        return callIdentityToolkit(SIGNIN_URL, email, password);
    }

    public AuthResult refreshToken(String refreshToken) throws FirebaseAuthException {
        try {
            String body = "grant_type=refresh_token&refresh_token="
                    + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(REFRESH_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());

            if (response.statusCode() != 200) {
                throw new FirebaseAuthException(extractErrorCode(json));
            }

            return new AuthResult(
                    json.path("id_token").asText(),
                    json.path("refresh_token").asText(),
                    json.path("user_id").asText(),
                    json.path("expires_in").asLong(3600)
            );
        } catch (IOException e) {
            throw new FirebaseAuthException("NETWORK_ERROR", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirebaseAuthException("NETWORK_ERROR", e);
        }
    }

    private AuthResult callIdentityToolkit(String url, String email, String password) throws FirebaseAuthException {
        try {
            ObjectNode bodyNode = mapper.createObjectNode();
            bodyNode.put("email", email);
            bodyNode.put("password", password);
            bodyNode.put("returnSecureToken", true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyNode.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());

            if (response.statusCode() != 200) {
                throw new FirebaseAuthException(extractErrorCode(json));
            }

            return new AuthResult(
                    json.path("idToken").asText(),
                    json.path("refreshToken").asText(),
                    json.path("localId").asText(),
                    json.path("expiresIn").asLong(3600)
            );
        } catch (IOException e) {
            throw new FirebaseAuthException("NETWORK_ERROR", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirebaseAuthException("NETWORK_ERROR", e);
        }
    }

    private String extractErrorCode(JsonNode json) {
        return json.path("error").path("message").asText("UKENDT_FEJL");
    }
}
