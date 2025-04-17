package fixit.dreams;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class UserDTO {
    public String foretrukneTema;
    public boolean visAdvarsel;
    public boolean visKollektiv;


    public UserDTO() {}

    public UserDTO(User user) {
        this.foretrukneTema = user.getForetrukneTemaNavn();
        this.visAdvarsel = user.isVisAdvarsel();
        this.visKollektiv = user.isVisKollektiv();

    }

    public User toUser() {
        User user = User.getInstance();
        user.setForetrukneTema(this.foretrukneTema);
        user.setVisAdvarsel(this.visAdvarsel);
        user.setVisKollektiv(this.visKollektiv);
        return user;
    }

}
