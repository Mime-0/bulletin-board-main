/**
 * Represents a single note on the bulletin board.
 * Position (x,y) is the upper-left corner; dimensions are fixed at server level.
 */
public class Note {
    private final int x;
    private final int y;
    private final String color;
    private final String message;

    public Note(int x, int y, String color, String message) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.message = message;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public String getColor() { return color; }
    public String getMessage() { return message; }

    /**
     * Returns true if the point (px, py) lies inside this note's rectangle.
     */
    public boolean contains(int px, int py, int noteW, int noteH) {
        return px >= x && px < x + noteW && py >= y && py < y + noteH;
    }

    /**
     * Returns true if this note completely overlaps another (same upper-left and same dimensions).
     */
    public boolean completelyOverlaps(Note other) {
        return this.x == other.x && this.y == other.y;
    }
}
