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
}
