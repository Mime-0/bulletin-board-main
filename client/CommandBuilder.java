public class CommandBuilder {

    public static String buildPost(
            int x, int y, String color, String message) {
        return "POST " + x + " " + y + " " + color + " " + message;
    }

    public static String buildPin(int x, int y) {
        return "PIN " + x + " " + y;
    }

    public static String buildUnpin(int x, int y) {
        return "UNPIN " + x + " " + y;
    }

    public static String buildShake() {
        return "SHAKE";
    }

    public static String buildClear() {
        return "CLEAR";
    }

    public static String buildGetPins() {
        return "GET PINS";
    }

    public static String buildGet(
            String color, Integer cx, Integer cy, String refersTo) {

        String cmd = "GET";

        if (color != null && !color.isEmpty())
            cmd += " color=" + color;

        if (cx != null && cy != null)
            cmd += " contains=" + cx + " " + cy;

        if (refersTo != null && !refersTo.isEmpty())
            cmd += " refersTo=" + refersTo;

        return cmd;
    }
}

