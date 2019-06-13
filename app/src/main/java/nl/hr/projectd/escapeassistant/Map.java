package nl.hr.projectd.escapeassistant;

import android.content.Context;
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

        ArrayList<Tile> arrowTiles = new ArrayList<>();
        ArrayList<ArrayList<Byte>> map = new ArrayList<>();
        map.add(new ArrayList<Byte>()); // For the first row

        BufferedReader reader = null;

        reader = new BufferedReader(new FileReader(file));

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
            // Als we een deel van de route vinden, voegen we dit punt toe
            else if (currentByte == MapSymbols.ROUTE) {
                arrowTiles.add(new Tile(x, y, (byte) currentByte));
            }

            map.get(y).add((byte)currentByte);

            x++; // Volgende kolom
        }

        reader.close();

        int xx = startX;
        int yy = startY;

        // Iterate through map find map
        while(true) {
            Tile nextTile = getNextTile(map, xx, yy);

            if (nextTile != null) {
                arrowTiles.add(nextTile);
                xx = nextTile.x;
                yy = nextTile.y;
            }
            else {
                break;
            }
        }

        // Stel de coordinaten relatief aan de positie van de speler in
        // (We gaan er van uit dat de speler naar het noorden kijkt)
        for (Tile arrowTile : arrowTiles) {
            arrowTile.x = arrowTile.x - startX;
            arrowTile.y = arrowTile.y - startY;
        }

        Log.d("MAP", "generate: " + arrowTiles.size() + " arrows.");

        return arrowTiles;
    }

    public static Tile getNextTile(ArrayList<ArrayList<Byte>> byteMaps, int x, int y) {

        // Check if index is within range
        boolean xmfree = x - 1 > 0,
                xpfree = x + 1 < byteMaps.get(0).size(),
                ymfree = y - 1 > 0,
                ypfree = y + 1 < byteMaps.size();

        if (xmfree && byteMaps.get(y).get(x-1) == MapSymbols.ROUTE) {
            return new Tile(x-1, y, ArrowSymbol.WEST);
        }
        else if (xpfree && byteMaps.get(y).get(x+1) == MapSymbols.ROUTE) {
            return new Tile(x+1, y, ArrowSymbol.EAST);
        }

        if (xmfree) {
            if (byteMaps.get(y).get(x-1) == MapSymbols.ROUTE) {
                return new Tile(x-1, y, ArrowSymbol.WEST);
            }
            else if (ymfree && byteMaps.get(y-1).get(x-1) == MapSymbols.ROUTE) {
                return new Tile(x-1, y-1, ArrowSymbol.NORTHWEST);
            }
        }
        else if (xpfree) {
            if (byteMaps.get(y).get(x+1) == MapSymbols.ROUTE) {
                return new Tile(x+1, y, ArrowSymbol.EAST);
            }
            else if (ymfree && byteMaps.get(y-1).get(x+1) == MapSymbols.ROUTE) {
                return new Tile(x+1, y-1, ArrowSymbol.NORTHEAST);
            }
        }

        return null;
    }

}
