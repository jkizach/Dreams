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

    // Et tilfældigt id for DENNE installation, sat første gang der synkroniseres. Det er
    // nøglen til at slippe for at læse hele skyen ved hver sync: står vores eget id i skyens
    // meta/state-dokument, har ingen anden maskine skrevet siden sidst, og så kan vi nøjes med
    // vores lokale billede af hvad der ligger deroppe (se SyncService og cloudindex.json).
    //
    // Det følger sync.json og forsvinder derfor ved "log ud". Det er med vilje: efter et
    // kontoskift ved vi ikke længere hvad skyen indeholder, og en frisk id tvinger den næste
    // sync ud ad den dyre, men altid korrekte vej.
    public String machineId;

    public SyncDTO() {}
}
