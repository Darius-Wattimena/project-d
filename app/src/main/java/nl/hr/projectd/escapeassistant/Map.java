package nl.hr.projectd.escapeassistant;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Map {

    public static final String fileName = "plattegrond.csv";

    private static Context context;

    public static ArrayList<Tile> generate(Context context, String fileName) throws IOException {

        ArrayList<Tile> tiles = new ArrayList<>();

        BufferedReader reader = null;

        reader = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName)));

        String line;
        String[] lineSplit;

        int y = 0;

        // For the users' start position
        int startX = 0, startY = 0;
        int endX = 0, endY = 0;

        while ((line = reader.readLine()) != null) {
            lineSplit = line.split(",");

            for(int x=0; x<lineSplit.length; ++x) {
                switch(lineSplit[x]) {
                    // Position will always be set to the last occurrence of a start symbol
                    case MapSymbols.START: {
                        startX = x;
                        startY = y;
                    } break;

                    // In the case of arrows (any directional tile) add it to the arrow collection
                    case MapSymbols.NORTH:
                    case MapSymbols.EAST:
                    case MapSymbols.SOUTH:
                    case MapSymbols.WEST:
                    case MapSymbols.NORTHEAST:
                    case MapSymbols.NORTHWEST:
                    case MapSymbols.SOUTHEAST:
                    case MapSymbols.SOUTHWEST: {
                        tiles.add(new Tile(x,y,lineSplit[x]));
                    } break;

                    // End tile
                    case MapSymbols.END: {
                        endX = x;
                        endY = y;
                    } break;
                }
            }

            // Set the coordinates relative to the players' position
            for (Tile arrowTile : tiles) {
                arrowTile.x = arrowTile.x - startX;
                arrowTile.y = arrowTile.y - startY;
            }

            y+=1;
        }

        reader.close();

        return tiles;
    }

}
