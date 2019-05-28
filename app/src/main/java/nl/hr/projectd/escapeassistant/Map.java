package nl.hr.projectd.escapeassistant;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Map {

    public static ArrayList<Tile> generate(Context context, String fileName) throws IOException {

        ArrayList<Tile> tiles = new ArrayList<>();

        BufferedReader reader = null;

        reader = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName)));

        String line;
        String[] lineSplit;

        // Huidige positie in bestand bijhouden
        int x = 0, y = 0;

        // Voor de start- en eindpositie
        int startX = 0, startY = 0;
        int endX = 0, endY = 0;

        int currentByte;

        while ((currentByte = reader.read()) != -1) {

            if (currentByte == MapSymbols.END_OF_LINE) {
                y += 1;
                x = 0;
                continue;
            }

            // Start positie van de speler
            if (currentByte == MapSymbols.START) {
                startX = x;
                startY = y;
            }

            // Als we een pijl vinden, voegen we deze toe aan de verzameling van tiles
            if (currentByte >= MapSymbols.NORTH && currentByte <= MapSymbols.SOUTHWEST) {
                tiles.add(new Tile(x, y, (byte) currentByte));
            }

            // Deze tile geeft aan waar het einde is
            if (currentByte == MapSymbols.END) {
                endX = x;
                endY = y;
            }

            x++; // Volgende kolom
        }
            // Stel de coordinaten relatief aan de positie van de speler in
            // (We gaan er van uit dat de speler naar het noorden kijkt)
            for (Tile arrowTile : tiles) {
                arrowTile.x = arrowTile.x - startX;
                arrowTile.y = arrowTile.y - startY;
            }




        reader.close();

        return tiles;
    }

}
