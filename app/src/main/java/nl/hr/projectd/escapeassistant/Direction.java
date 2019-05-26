package nl.hr.projectd.escapeassistant;

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

    public static Direction fromMapSymbol(String symbol) {
        switch (symbol) {
            case MapSymbols.NORTH: return Direction.NORTH;
            case MapSymbols.EAST: return Direction.EAST;
            case MapSymbols.SOUTH: return Direction.SOUTH;
            case MapSymbols.WEST: return Direction.WEST;
            case MapSymbols.NORTHEAST: return Direction.NORTHEAST;
            case MapSymbols.NORTHWEST: return Direction.NORTHWEST;
            case MapSymbols.SOUTHEAST: return Direction.SOUTHEAST;
            case MapSymbols.SOUTHWEST: return Direction.SOUTHWEST;
            default: return Direction.NORTH;
        }
    }
}
