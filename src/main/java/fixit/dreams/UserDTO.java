package fixit.dreams;

import java.time.LocalDate;

public class UserDTO {
    public String foretrukneTema;
    public boolean visAdvarsel;
    public boolean visKollektiv;
    public boolean visHolografisk;
    public LocalDate startFromThisDate;
    public int schemaVersion;

    public UserDTO() {}

    public UserDTO(User user) {
        this.foretrukneTema = user.getForetrukneTemaNavn();
        this.visAdvarsel = user.isVisAdvarsel();
        this.visKollektiv = user.isVisKollektiv();
        this.visHolografisk = user.isVisHolografisk();
        this.startFromThisDate = user.getStartFromThisDate();
        this.schemaVersion = SchemaMigrator.CURRENT_SCHEMA_VERSION;

    }

    public User toUser() {
        User user = User.getInstance();
        user.setForetrukneTema(this.foretrukneTema);
        user.setVisAdvarsel(this.visAdvarsel);
        user.setVisKollektiv(this.visKollektiv);
        user.setVisHolografisk(this.visHolografisk);
        user.setStartFromThisDate(this.startFromThisDate);
        return user;
    }

}
