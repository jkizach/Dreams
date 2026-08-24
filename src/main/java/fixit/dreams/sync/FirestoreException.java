package fixit.dreams.sync;

public class FirestoreException extends Exception {
    private final int statusCode;

    public FirestoreException(String message) {
        this(message, -1);
    }

    public FirestoreException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public FirestoreException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
