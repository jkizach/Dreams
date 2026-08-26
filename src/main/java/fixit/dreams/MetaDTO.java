package fixit.dreams;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

// Tidsstempler for de datafiler der IKKE har deres eget updatedAt-felt: kategori-DEFINITIONERNE
// (cats.json), temaerne (temaer.json) og de generelle indstillinger (user.json). Drømme har
// deres eget updatedAt pr. drøm og hører derfor ikke hjemme her.
//
// Stemplerne ligger i deres egen fil (meta.json) frem for inde i de tre datafiler, fordi
// cats.json og temaer.json er rå JSON-ARRAYS: der er ingen plads til et tidsstempel uden at
// ændre deres format - og et formatskifte på eksisterende brugeres data er præcis den slags
// migration der kan koste data. meta.json er rent additivt: den kan tilføjes, mistes og
// genskabes uden at nogen af de rigtige datafiler røres.
//
// Tidsstemplet bumpes KUN når indholdet faktisk har ændret sig - derfor hash'en. Blindt at
// stemple ved hver appluk ville betyde, at den maskine der lukkede sidst altid vandt en
// kommende cloud-sammenkøring, også selvom den intet havde ændret.
public class MetaDTO {
    public Stamp categories = new Stamp();
    public Stamp temaer = new Stamp();
    public Stamp settings = new Stamp();

    public static class Stamp {
        public Instant updatedAt;
        public String hash;

        // Returnerer true hvis stemplet blev ændret - altså om meta.json skal skrives igen.
        //
        // Første gang et indhold ses (hash == null) registreres hash'en UDEN at sætte updatedAt.
        // Det er med vilje: en frisk installations standardkategorier/-temaer er ikke "redigeret
        // lige nu", og må ikke kunne vinde over en rigtig udgave i skyen. Kun en ægte ændring -
        // eller SchemaMigrator, på vegne af en eksisterende brugers data - sætter et tidspunkt.
        public boolean stampIfChanged(String nyHash) {
            if (nyHash == null || nyHash.equals(hash)) {
                return false;
            }
            boolean førsteGang = (hash == null);
            hash = nyHash;
            if (!førsteGang) {
                updatedAt = Instant.now();
            }
            return true;
        }
    }

    // SHA-256 af præcis det JSON der gemmes på disk. Hash'en er kun et "er det her det samme
    // indhold som sidst"-fingeraftryk - den forlader aldrig maskinen og indgår ikke i syncen.
    public static String hashOf(String indhold) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(indhold.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 findes ikke i denne JVM", e);
        }
    }
}
