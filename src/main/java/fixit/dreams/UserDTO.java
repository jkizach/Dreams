package fixit.dreams;

import java.time.LocalDate;

public class UserDTO {
    public String foretrukneTema;
    public boolean visAdvarsel;
    public boolean visKollektiv;
    public LocalDate startFromThisDate;

    public UserDTO() {}

    public UserDTO(User user) {
        this.foretrukneTema = user.getForetrukneTemaNavn();
        this.visAdvarsel = user.isVisAdvarsel();
        this.visKollektiv = user.isVisKollektiv();
        this.startFromThisDate = user.getStartFromThisDate();

    }

    public User toUser() {
        User user = User.getInstance();
        user.setForetrukneTema(this.foretrukneTema);
        user.setVisAdvarsel(this.visAdvarsel);
        user.setVisKollektiv(this.visKollektiv);
        user.setStartFromThisDate(this.startFromThisDate);
        return user;
    }

}
