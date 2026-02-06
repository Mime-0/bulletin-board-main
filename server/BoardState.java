import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Thread-safe shared state for the bulletin board: dimensions, notes, and pins.
 * All public methods are synchronized to ensure atomic operations under concurrency.
 */
public class BoardState {
    private final int boardW;
    private final int boardH;
    private final int noteW;
    private final int noteH;
    private final Set<String> validColors;

    private final List<Note> notes = new ArrayList<>();
    /** Pins as [x,y] pairs; multiple pins at same coordinate are allowed. */
    private final List<int[]> pins = new ArrayList<>();

    public BoardState(int boardW, int boardH, int noteW, int noteH, Set<String> validColors) {
        this.boardW = boardW;
        this.boardH = boardH;
        this.noteW = noteW;
        this.noteH = noteH;
        this.validColors = new HashSet<>(validColors);
    }

    public int getBoardW() { return boardW; }
    public int getBoardH() { return boardH; }
    public int getNoteW() { return noteW; }
    public int getNoteH() { return noteH; }
    public Set<String> getValidColors() { return new HashSet<>(validColors); }

    /**
     * Returns true if note at (x,y) is fully within board bounds.
     */
    public synchronized boolean isInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x + noteW <= boardW && y + noteH <= boardH;
    }

    /**
     * Returns true if the given color is valid.
     */
    public synchronized boolean isValidColor(String color) {
        return color != null && validColors.contains(color);
    }

    /**
     * Returns true if any note completely overlaps the position (x, y).
     */
    private boolean hasCompleteOverlapAt(int x, int y) {
        for (Note n : notes) {
            if (n.getX() == x && n.getY() == y) return true;
        }
        return false;
    }

    /**
     * Adds a note. Caller must hold lock and have already validated bounds, color, overlap.
     */
    public synchronized void addNote(Note note) {
        notes.add(note);
    }

    /**
     * Validates and adds a note. Returns null on success; otherwise error code (OUT_OF_BOUNDS, etc.).
     */
    public synchronized String validateAndAddNote(int x, int y, String color, String message) {
        if (!isInBounds(x, y)) {
            return "OUT_OF_BOUNDS";
        }
        if (!isValidColor(color)) {
            return "COLOUR_NOT_SUPPORTED";
        }
        if (hasCompleteOverlapAt(x, y)) {
            return "COMPLETE_OVERLAP";
        }
        addNote(new Note(x, y, color, message));
        return null;
    }

    /**
     * Returns true if at least one note contains the point (px, py).
     */
    public synchronized boolean anyNoteContains(int px, int py) {
        for (Note n : notes) {
            if (n.contains(px, py, noteW, noteH)) return true;
        }
        return false;
    }

    /**
     * Adds one pin at (x, y). Returns null on success, "NO_NOTE_AT_COORDINATE" if no note contains (x,y).
     */
    public synchronized String pin(int x, int y) {
        if (!anyNoteContains(x, y)) {
            return "NO_NOTE_AT_COORDINATE";
        }
        pins.add(new int[]{x, y});
        return null;
    }

    /**
     * Removes one pin at (x, y). Returns null on success, "PIN_NOT_FOUND" if no pin at that coordinate.
     */
    public synchronized String unpin(int x, int y) {
        for (int i = 0; i < pins.size(); i++) {
            int[] p = pins.get(i);
            if (p[0] == x && p[1] == y) {
                pins.remove(i);
                return null;
            }
        }
        return "PIN_NOT_FOUND";
    }

    /**
     * Returns true if the note has at least one pin inside its rectangle.
     */
    private boolean isPinned(Note note) {
        for (int[] p : pins) {
            if (note.contains(p[0], p[1], noteW, noteH)) return true;
        }
        return false;
    }

    /**
     * Removes all unpinned notes and pins that no longer lie in any note. Atomic.
     */
    public synchronized void shake() {
        notes.removeIf(n -> !isPinned(n));
        pins.removeIf(p -> !anyNoteContains(p[0], p[1]));
    }

    /**
     * Removes all notes and all pins. Atomic.
     */
    public synchronized void clear() {
        notes.clear();
        pins.clear();
    }

    /**
     * Returns a copy of all pins as [x,y] pairs.
     */
    public synchronized List<int[]> getAllPins() {
        return new ArrayList<>(pins);
    }

    /**
     * Returns notes matching all criteria. null color/contains/refersTo means "match all".
     */
    public synchronized List<Note> getNotes(String colorFilter, int cx, int cy, boolean useContains, String refersTo) {
        return notes.stream().filter(n ->
            (colorFilter == null || n.getColor().equals(colorFilter))
                && (!useContains || n.contains(cx, cy, noteW, noteH))
                && (refersTo == null || n.getMessage().contains(refersTo))
        ).collect(Collectors.toList());
    }
}
