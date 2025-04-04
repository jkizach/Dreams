package fixit.dreams;

import javafx.scene.paint.Color;
import java.util.HashMap;

public class TemaDTO {
    public Color baggrundA;
    public Color baggrundB;
    public Color baggrundC;
    public Color baggrundD;
    public Color tekstA;
    public Color tekstB;
    public Color tekstC;
    public Color kant;
    public String font;

    public HashMap<String,String> getTemaForCSSUpdater() {
        HashMap<String,String> outMap = new HashMap<>();
        outMap.put("-fx-hovedbg-background",baggrundA.toString());
        outMap.put("-fx-alternativbg-background",baggrundB.toString());
        outMap.put("-fx-andenalternativbg-background",baggrundC.toString());
        outMap.put("-fx-textfelt-background",baggrundD.toString());
        outMap.put("-fx-hovedtxt-text",tekstA.toString());
        outMap.put("-fx-alttext-text",tekstB.toString());
        outMap.put("-fx-alternativtxt-text",tekstC.toString());
        outMap.put("-fx-border-border",kant.toString());
        outMap.put("-fx-font-family",font);
        return outMap;
    }
}
