package nl.hr.projectd.escapeassistant;

public class Tile {

    public Tile(int x, int y, String symbol) {
        this.x = x;
        this.y = y;
        this.symbol = symbol;
    }

    public int x;
    public int y;
    public String symbol = MapSymbols.NONE;
}
