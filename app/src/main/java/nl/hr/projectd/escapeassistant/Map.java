package nl.hr.projectd.escapeassistant;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class Map {

    public static ArrayList<Tile> generate(Context context, File file) throws IOException {

        ArrayList<Tile> mapTiles = new ArrayList<>();
        ArrayList<ArrayList<Byte>> map = new ArrayList<>();
        map.add(new ArrayList<Byte>()); // For the first row

        BufferedReader reader = null;

        reader = new BufferedReader(new FileReader(file));
        //reader = new BufferedReader(new InputStreamReader(context.getAssets().open("plattegrond_pf.bin")));

        String line;
        String[] lineSplit;

        // Huidige positie in bestand bijhouden
        int x = 0, y = 0;

        // Voor de start- en eindpositie
        int startX = 0, startY = 0;
        int endX = 0, endY = 0;

        int currentByte;

        // Read map
        while ((currentByte = reader.read()) != -1) {

            if (currentByte == MapSymbols.END_OF_LINE) {
                // Next row
                y += 1;
                map.add(new ArrayList<Byte>());
                x = 0;
                continue;
            }

            // Start positie van de speler
            if (currentByte == MapSymbols.START) {
                startX = x;
                startY = y;
            }
            else if (currentByte == MapSymbols.END) {
                endX = x;
                endY = y;
            }

            map.get(y).add((byte)currentByte);

            x++; // Volgende kolom
        }

        reader.close();

        int xx = startX;
        int yy = startY;

        mapTiles.add(new Tile(xx,yy,MapSymbols.START));

        // Iterate through map find map
        while(true) {
            Tile nextTile = getNextTile(map, xx, yy, mapTiles);

            if (nextTile != null) {
                // Continue route
                mapTiles.add(nextTile);

                xx = nextTile.x;
                yy = nextTile.y;
            }
            else {
                break;
            }
        }

        // Stel de coordinaten relatief aan de positie van de speler in
        for (Tile t : mapTiles) {
            t.x = t.x - startX;
            t.y = t.y - startY;
            Log.d("MAP", "generate: " + t.x + "/" + t.y + ":" + t.symbol);
        }

        // Neem de richting van de opvolgende pijl aan
        for (int i=0; i<mapTiles.size() - 1; ++i) {
            // if might not be needed
            if (mapTiles.get(i).symbol >= ArrowSymbol.NORTH && mapTiles.get(i).symbol <= ArrowSymbol.SOUTHWEST &&
                    mapTiles.get(i+1).symbol >= ArrowSymbol.NORTH && mapTiles.get(i+1).symbol <= ArrowSymbol.SOUTHWEST)
            mapTiles.get(i).symbol = mapTiles.get(i+1).symbol;
        }

        Log.d("MAP", "END: " + endX + "/" + endY);

        mapTiles.add(new Tile(endX - startX,endY - startY,MapSymbols.END));

        Log.d("MAP", "generate: " + mapTiles.size() + " arrows.");

        return mapTiles;
    }

    public static Tile getNextTile(ArrayList<ArrayList<Byte>> byteMaps, int x, int y, ArrayList<Tile> traveledTiles) {

        // Check if index is within range
        boolean xmfree = x - 1 >= 0,
                xpfree = x + 1 < byteMaps.get(0).size(),
                ymfree = y - 1 >= 0,
                ypfree = y + 1 < byteMaps.size();

        // VERTICAL
        if (ymfree && byteMaps.get(y-1).get(x) == MapSymbols.ROUTE && !checkTraveled(x, y-1, traveledTiles)) {
            Log.d("MAP", "getNextTile: N");
            return new Tile(x,y-1, ArrowSymbol.NORTH);
        }
        if (ypfree && byteMaps.get(y+1).get(x) == MapSymbols.ROUTE && !checkTraveled(x, y+1, traveledTiles)) {
            Log.d("MAP", "getNextTile: S");
            return new Tile(x,y-1, ArrowSymbol.SOUTH);
        }

        // HORIZONTAL AND HEXAGONAL
        if (xmfree) {
            if (byteMaps.get(y).get(x-1) == MapSymbols.ROUTE && !checkTraveled(x-1, y, traveledTiles)) {
                Log.d("MAP", "getNextTile: W");
                return new Tile(x-1, y, ArrowSymbol.WEST);
            }
            else if (ymfree && byteMaps.get(y-1).get(x-1) == MapSymbols.ROUTE && !checkTraveled(x-1, y-1, traveledTiles)) {
                Log.d("MAP", "getNextTile: NW");
                return new Tile(x-1, y-1, ArrowSymbol.NORTHWEST);
            }
            else if (ypfree && byteMaps.get(y+1).get(x-1) == MapSymbols.ROUTE && !checkTraveled(x-1, y+1, traveledTiles)) {
                Log.d("MAP", "getNextTile: SW");
                return new Tile(x-1, y+1, ArrowSymbol.SOUTHWEST);
            }
        }
        if (xpfree) {
            if (byteMaps.get(y).get(x+1) == MapSymbols.ROUTE && !checkTraveled(x+1, y, traveledTiles)) {
                Log.d("MAP", "getNextTile: E");
                return new Tile(x+1, y, ArrowSymbol.EAST);
            }
            else if (ymfree && byteMaps.get(y-1).get(x+1) == MapSymbols.ROUTE && !checkTraveled(x+1, y-1, traveledTiles)) {
                Log.d("MAP", "getNextTile: NE");
                return new Tile(x+1, y-1, ArrowSymbol.NORTHEAST);
            }
            else if (ypfree && byteMaps.get(y+1).get(x+1) == MapSymbols.ROUTE && !checkTraveled(x+1, y+1, traveledTiles)) {
                Log.d("MAP", "getNextTile: SE");
                return new Tile(x+1, y+1, ArrowSymbol.SOUTHEAST);
            }
        }

        return null;
    }

    public static boolean checkTraveled(int x, int y, ArrayList<Tile> traveledTiles) {
        for(Tile t : traveledTiles) {
            if (t.x == x && t.y == y) return true;
        }
        return false;
    }

}
