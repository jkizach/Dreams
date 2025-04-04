package fixit.dreams;

import javafx.scene.paint.Color;

import java.util.HashMap;

public class Tema {
    private Color baggrundA;
    private Color baggrundB;
    private Color baggrundC;
    private Color baggrundD;
    private Color tekstA;
    private Color tekstB;
    private Color tekstC;
    private Color kant;
    private String temaName;
    private String font;

    public Tema(Color baggrundA, Color baggrundB, Color baggrundC, Color baggrundD, Color tekstA, Color tekstB, Color tekstC, Color kant, String temaName, String font) {
        this.baggrundA = baggrundA;
        this.baggrundB = baggrundB;
        this.baggrundC = baggrundC;
        this.baggrundD = baggrundD;
        this.tekstA = tekstA;
        this.tekstB = tekstB;
        this.tekstC = tekstC;
        this.kant = kant;
        this.temaName = temaName;
        this.font = font;
    }

    public Tema(TemaDTO data, String temaNavn) {
        this.baggrundA = data.baggrundA;
        this.baggrundB = data.baggrundB;
        this.baggrundC = data.baggrundC;
        this.baggrundD = data.baggrundD;
        this.tekstA = data.tekstA;
        this.tekstB = data.tekstB;
        this.tekstC = data.tekstC;
        this.kant = data.kant;
        this.temaName = temaNavn;
        this.font = data.font;
    }

    public Tema(HashMap<String,String> loadedMap) {
        this.baggrundA = hexTilFarve(loadedMap.get("baggrundA"));
        this.baggrundB = hexTilFarve(loadedMap.get("baggrundB"));
        this.baggrundC = hexTilFarve(loadedMap.get("baggrundC"));
        this.baggrundD = hexTilFarve(loadedMap.get("baggrundD"));
        this.tekstA = hexTilFarve(loadedMap.get("tekstA"));
        this.tekstB = hexTilFarve(loadedMap.get("tekstB"));
        this.tekstC = hexTilFarve(loadedMap.get("tekstC"));
        this.kant = hexTilFarve(loadedMap.get("kant"));
        this.temaName = loadedMap.get("temaName");
        this.font = loadedMap.get("font");
    }

    public Color getBaggrundA() {
        return baggrundA;
    }

    public Color getBaggrundB() {
        return baggrundB;
    }

    public Color getBaggrundC() {
        return baggrundC;
    }
    public Color getBaggrundD() {
        return baggrundD;
    }

    public Color getTekstA() {
        return tekstA;
    }

    public Color getTekstB() {
        return tekstB;
    }

    public Color getTekstC() {
        return tekstC;
    }

    public Color getKant() {
        return kant;
    }

    public String getTemaName() {
        return temaName;
    }
    public String getFont() {
        return font;
    }

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

    public void setBaggrundA(Color baggrundA) {
        this.baggrundA = baggrundA;
    }

    public void setBaggrundB(Color baggrundB) {
        this.baggrundB = baggrundB;
    }

    public void setBaggrundC(Color baggrundC) {
        this.baggrundC = baggrundC;
    }

    public void setBaggrundD(Color baggrundD) {
        this.baggrundD = baggrundD;
    }

    public void setTekstA(Color tekstA) {
        this.tekstA = tekstA;
    }

    public void setTekstB(Color tekstB) {
        this.tekstB = tekstB;
    }

    public void setTekstC(Color tekstC) {
        this.tekstC = tekstC;
    }

    public void setKant(Color kant) {
        this.kant = kant;
    }

    public void setTemaName(String temaName) {
        this.temaName = temaName;
    }

    public void setFont(String font) {
        this.font = font;
    }

    private Color hexTilFarve(String hexFarve) {
        Color out = Color.web(hexFarve);
        return out;
    }

    private String toHexColor(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    public HashMap<String,String> getTemaForSaving() {
        HashMap<String,String> outMap = new HashMap<>();
        outMap.put("baggrundA",toHexColor(baggrundA));
        outMap.put("baggrundB",toHexColor(baggrundB));
        outMap.put("baggrundC",toHexColor(baggrundC));
        outMap.put("baggrundD",toHexColor(baggrundD));
        outMap.put("tekstA",toHexColor(tekstA));
        outMap.put("tekstB",toHexColor(tekstB));
        outMap.put("tekstC",toHexColor(tekstC));
        outMap.put("kant",toHexColor(kant));
        outMap.put("temaName",temaName);
        outMap.put("font",font);
        return outMap;
    }

}
