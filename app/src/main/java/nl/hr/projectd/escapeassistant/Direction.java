package nl.hr.projectd.escapeassistant;

import static nl.hr.projectd.escapeassistant.ArrowSymbol.*;

public enum Direction {

    // Cardinal
    NORTH() {
      public int getDegrees() { return 0; }
    },
    EAST() {
        public int getDegrees() { return 90; }
    },
    SOUTH () {
        public int getDegrees() { return 180; }
    },
    WEST () {
        public int getDegrees() { return 270; }
    },

    // Intercardinal
    NORTHEAST () {
        public int getDegrees() {
            return 45;
        }
    },
    SOUTHEAST () {
        public int getDegrees() {
            return SOUTH.getDegrees() - 45;
        }
    },
    NORTHWEST () {
        public int getDegrees() {
            return WEST.getDegrees() + 45;
        }
    },
    SOUTHWEST () {
        public int getDegrees() {
            return SOUTH.getDegrees() + 45;
        }
    };

    public abstract int getDegrees();

    public static Direction fromArrowSymbol(byte symbol) {
        switch (symbol) {
            case ArrowSymbol.NORTH: return Direction.NORTH;
            case ArrowSymbol.EAST: return Direction.EAST;
            case ArrowSymbol.SOUTH: return Direction.SOUTH;
            case ArrowSymbol.WEST: return Direction.WEST;
            case ArrowSymbol.NORTHEAST: return Direction.NORTHEAST;
            case ArrowSymbol.NORTHWEST: return Direction.NORTHWEST;
            case ArrowSymbol.SOUTHEAST: return Direction.SOUTHEAST;
            case ArrowSymbol.SOUTHWEST: return Direction.SOUTHWEST;
            default: return Direction.NORTH;
        }
    }
}
