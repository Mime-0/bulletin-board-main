public class ClientNote {
    public final int x, y;
    public final String color;
    public final String message;

    private int pinCount;

    public ClientNote(int x, int y, String color, String message) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.message = message;
        this.pinCount = 0;
    }

    public boolean isPinned() {
        return pinCount > 0;
    }

    public void addPin() {
        pinCount++;
    }

    public boolean removePin() {
        if (pinCount > 0) {
            pinCount--;
            return true;
        }
        return false;
    }

    public void clearPins() {
        pinCount = 0;
    }
}



