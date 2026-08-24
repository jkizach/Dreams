package fixit.dreams.sync;

import java.time.Instant;

// Persisteret form af login-/sync-tilstanden, gemt separat fra user.json (i sync.json) så
// "log ud" er en enkelt fil-sletning der aldrig rører selve drømme-/kategoridata.
public class SyncDTO {
    public String email;
    public String refreshToken;
    public String uid;
    public boolean syncEnabled;
    public Instant lastSyncedAt;

    public SyncDTO() {}
}
