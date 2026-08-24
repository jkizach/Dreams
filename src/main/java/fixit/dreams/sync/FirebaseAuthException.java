package fixit.dreams.sync;

public class FirebaseAuthException extends Exception {
    private final String errorCode;

    public FirebaseAuthException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public FirebaseAuthException(String errorCode, Throwable cause) {
        super(errorCode, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // Oversætter Firebases fejlkoder til en kort, dansk brugervendt tekst til UI'et.
    public String toDanishMessage() {
        if (errorCode.startsWith("WEAK_PASSWORD")) {
            return "Adgangskoden skal være mindst 6 tegn.";
        }
        return switch (errorCode) {
            case "EMAIL_EXISTS" -> "Der findes allerede en konto med denne email.";
            case "EMAIL_NOT_FOUND", "INVALID_PASSWORD", "INVALID_LOGIN_CREDENTIALS" -> "Forkert email eller adgangskode.";
            case "INVALID_EMAIL" -> "Ugyldig email-adresse.";
            case "USER_DISABLED" -> "Denne konto er deaktiveret.";
            case "TOO_MANY_ATTEMPTS_TRY_LATER" -> "For mange forsøg - prøv igen senere.";
            case "NETWORK_ERROR" -> "Kunne ikke oprette forbindelse - tjek din internetforbindelse.";
            default -> "Der opstod en fejl (" + errorCode + ").";
        };
    }
}
