package nl.hr.projectd.escapeassistant;

import java.util.BitSet;

public class MapSymbols {

    public static final byte

        NONE  = 0b0000,
        SOLID = 0b0001,
        START = 0b0010,
        END   = 0b0011,

        NORTH = 0b0100,
        EAST  = 0b0101,
        SOUTH = 0b0110,
        WEST  = 0b0111,

        NORTHEAST = 0b1000,
        NORTHWEST = 0b1001,

        SOUTHEAST = 0b1010,
        SOUTHWEST = 0b1011,

        END_OF_LINE = 0b1111;
}
