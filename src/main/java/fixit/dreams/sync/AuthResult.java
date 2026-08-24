package fixit.dreams.sync;

public record AuthResult(String idToken, String refreshToken, String uid, long expiresInSeconds) {}
