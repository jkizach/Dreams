package fixit.dreams.sync;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

// Delt ObjectMapper for hele sync-pakken - bevidst adskilt fra IOutils' egen. Den lokale
// filformat-mapper (IOutils) skriver datoer i array-format og må ikke ændres (rører hundredvis
// af eksisterende brugerfiler); denne skriver ISO-8601-strenge, som er pænere at have liggende
// i Firestore og lettere at sammenligne direkte i SyncService's last-write-wins-logik.
public final class SyncObjectMapper {
    public static final ObjectMapper INSTANCE = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // Ukendte felter ignoreres, fordi dette er et TRÅDFORMAT vi ikke selv styrer alderen
            // på: skyen kan indeholde dokumenter skrevet af en nyere version af appen. Med streng
            // parsing ville et enkelt ukendt felt kaste en exception, og pullNewerDreams' catch
            // ville stille og roligt springe drømmen over - den ville aldrig blive hentet ned,
            // uden en eneste fejlmeddelelse. Gravstenens deletedAt er det første ekstra felt.
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private SyncObjectMapper() {}
}
