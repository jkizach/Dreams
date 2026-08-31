package fixit.dreams;

import java.util.ArrayList;
import java.util.TreeSet;

/**
 * Bruges to steder: som kategoriens definition i cats.json (id + navn + symboler), og som en
 * drøms tag i dreams.json (id + de valgte symboler).
 *
 * I en drøm er "name" med vilje tom fra og med skema v4. Et tag peger på id'et, så en omdøbning
 * ikke skal skrives ind i hver eneste drøm der bruger kategorien - og så en drøm ikke kan komme
 * til at pege på en kategori der ikke findes, fordi omdøbningen kun er nået halvvejs gennem
 * synkroniseringen. Navnet slås op i kategorilisten når det skal vises.
 */
public class CategoryDTO {
    public String id;
    public String name;
    public TreeSet<String> symbols;
    public ArrayList<String> customOrder;
}
