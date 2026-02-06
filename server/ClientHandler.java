import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Handles one client connection: sends initialization data, then processes commands
 * until DISCONNECT or socket close. All board operations go through synchronized BoardState.
 */
public class ClientHandler implements Runnable {
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private final Socket socket;
    private final BoardState board;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket, BoardState board) {
        this.socket = socket;
        this.board = board;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            sendInit();

            String line;
            while ((line = readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (processCommand(line)) {
                    break; // DISCONNECT
                }
            }
        } catch (IOException e) {
            // Client disconnected or I/O error
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private String readLine() throws IOException {
        try {
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private void sendInit() {
        out.println("BOARD " + board.getBoardW() + " " + board.getBoardH());
        out.println("NOTE " + board.getNoteW() + " " + board.getNoteH());
        Set<String> colors = board.getValidColors();
        StringBuilder sb = new StringBuilder("COLORS");
        for (String c : colors) {
            sb.append(" ").append(c);
        }
        out.println(sb.toString());
        out.println("OK");
    }

    private void sendOk() {
        out.println("OK");
    }

    private void sendOk(String code) {
        out.println("OK " + code);
    }

    private void sendOk(int count) {
        out.println("OK " + count);
    }

    private void sendError(String code, String description) {
        out.println("ERROR " + code + " " + description);
    }

    /**
     * Process one command line. Returns true if client requested DISCONNECT.
     */
    private boolean processCommand(String line) {
        String[] tokens = SPACES.split(line, 2);
        String cmd = tokens[0];
        String rest = tokens.length > 1 ? tokens[1].trim() : "";

        try {
            switch (cmd.toUpperCase()) {
                case "POST":
                    handlePost(rest);
                    return false;
                case "GET":
                    handleGet(rest);
                    return false;
                case "PIN":
                    handlePin(rest);
                    return false;
                case "UNPIN":
                    handleUnpin(rest);
                    return false;
                case "SHAKE":
                    handleShake(rest);
                    return false;
                case "CLEAR":
                    handleClear(rest);
                    return false;
                case "DISCONNECT":
                    handleDisconnect();
                    return true;
                default:
                    sendError("INVALID_FORMAT", "Unrecognized command: " + cmd);
                    return false;
            }
        } catch (Exception e) {
            sendError("INVALID_FORMAT", "Processing failed: " + e.getMessage());
            return false;
        }
    }

    private void handlePost(String rest) {
        // POST <x> <y> <colour> <message>
        if (rest.isEmpty()) {
            sendError("INVALID_FORMAT", "Expected format: POST <x> <y> <colour> <message>");
            return;
        }
        String[] parts = rest.split("\\s+", 4);
        if (parts.length < 4) {
            sendError("INVALID_FORMAT", "Expected format: POST <x> <y> <colour> <message>");
            return;
        }
        int x = parseInt(parts[0]);
        int y = parseInt(parts[1]);
        if (x < 0 || y < 0) {
            sendError("INVALID_FORMAT", "Coordinates must be non-negative integers");
            return;
        }
        String color = parts[2];
        String message = parts[3].trim();

        String err = board.validateAndAddNote(x, y, color, message);
        if (err != null) {
            switch (err) {
                case "OUT_OF_BOUNDS":
                    sendError("OUT_OF_BOUNDS", "Note at (" + x + ", " + y + ") exceeds board dimensions (" + board.getBoardW() + "x" + board.getBoardH() + ")");
                    break;
                case "COLOUR_NOT_SUPPORTED":
                    sendError("COLOUR_NOT_SUPPORTED", "The colour \"" + color + "\" is not supported. Supported colours: " + board.getValidColors());
                    break;
                case "COMPLETE_OVERLAP":
                    sendError("COMPLETE_OVERLAP", "A note already exists at position (" + x + ", " + y + ")");
                    break;
                default:
                    sendError(err, "Post failed");
            }
            return;
        }
        sendOk("NOTE_POSTED");
    }

    private void handleGet(String rest) {
        if (rest.equalsIgnoreCase("PINS")) {
            List<int[]> pins = board.getAllPins();
            for (int[] p : pins) {
                out.println("PIN " + p[0] + " " + p[1]);
            }
            sendOk(pins.size());
            return;
        }

        // GET [colour=<c>] [contains=<x> <y>] [refersTo=<text>]
        String colorFilter = null;
        int cx = 0, cy = 0;
        boolean useContains = false;
        String refersTo = null;

        String remaining = rest;
        while (!remaining.isEmpty()) {
            remaining = remaining.trim();
            if (remaining.toLowerCase().startsWith("colour=")) {
                int eq = remaining.indexOf('=');
                int next = findNextFilterStart(remaining, eq + 1);
                colorFilter = next < 0 ? remaining.substring(eq + 1).trim() : remaining.substring(eq + 1, next).trim();
                remaining = next < 0 ? "" : remaining.substring(next);
            } else if (remaining.toLowerCase().startsWith("color=")) {
                int eq = remaining.indexOf('=');
                int next = findNextFilterStart(remaining, eq + 1);
                colorFilter = next < 0 ? remaining.substring(eq + 1).trim() : remaining.substring(eq + 1, next).trim();
                remaining = next < 0 ? "" : remaining.substring(next);
            } else if (remaining.toLowerCase().startsWith("contains=")) {
                int eq = remaining.indexOf('=');
                String after = remaining.substring(eq + 1).trim();

                String[] nums = after.split("\\s+", 3);
                if (nums.length < 2) {
                    sendError("INVALID_FORMAT", "Expected contains=<x> <y>");
                    return;
                }

                cx = parseInt(nums[0]);
                cy = parseInt(nums[1]);
                if (cx < 0 || cy < 0) {
                    sendError("INVALID_FORMAT", "Contains coordinates must be non-negative");
                    return;
                }

                useContains = true;

                remaining = (nums.length == 3) ? nums[2].trim() : "";
            } else if (remaining.toLowerCase().startsWith("refersto=")) {
                int eq = remaining.indexOf('=');
                refersTo = remaining.substring(eq + 1).trim();
                remaining = "";
            } else {
                sendError("INVALID_FORMAT", "Invalid GET filter: " + remaining);
                return;
            }
        }

        List<Note> notes = board.getNotes(colorFilter, cx, cy, useContains, refersTo);
        for (Note n : notes) {
            out.println("NOTE " + n.getX() + " " + n.getY() + " " + n.getColor() + " " + n.getMessage());
        }
        sendOk(notes.size());
    }

    /** Finds the start index of the next filter (space + key=) in s at or after position from. */
    private int findNextFilterStart(String s, int from) {
        if (from >= s.length()) return -1;
        String rest = s.substring(from).toLowerCase();
        int best = -1;
        for (String key : new String[]{" colour=", " color=", " contains=", " refersto="}) {
            int idx = rest.indexOf(key);
            if (idx >= 0 && (best < 0 || idx < best)) best = idx;
        }
        return best < 0 ? -1 : from + best + 1; // +1 to skip the leading space
    }

    private void handlePin(String rest) {
        String[] parts = rest.split("\\s+");
        if (parts.length != 2) {
            sendError("INVALID_FORMAT", "Expected format: PIN <x> <y>");
            return;
        }
        int x = parseInt(parts[0]);
        int y = parseInt(parts[1]);
        if (x < 0 || y < 0) {
            sendError("INVALID_FORMAT", "Coordinates must be non-negative integers");
            return;
        }
        String err = board.pin(x, y);
        if (err != null) {
            sendError("NO_NOTE_AT_COORDINATE", "No note contains the coordinate (" + x + ", " + y + ")");
            return;
        }
        sendOk("PIN_ADDED");
    }

    private void handleUnpin(String rest) {
        String[] parts = rest.split("\\s+");
        if (parts.length != 2) {
            sendError("INVALID_FORMAT", "Expected format: UNPIN <x> <y>");
            return;
        }
        int x = parseInt(parts[0]);
        int y = parseInt(parts[1]);
        if (x < 0 || y < 0) {
            sendError("INVALID_FORMAT", "Coordinates must be non-negative integers");
            return;
        }
        String err = board.unpin(x, y);
        if (err != null) {
            sendError("PIN_NOT_FOUND", "No pin exists at coordinate (" + x + ", " + y + ")");
            return;
        }
        sendOk("PIN_REMOVED");
    }

    private void handleShake(String rest) {
        if (!rest.isEmpty()) {
            sendError("INVALID_FORMAT", "SHAKE takes no arguments");
            return;
        }
        board.shake();
        sendOk("BOARD_SHAKEN");
    }

    private void handleClear(String rest) {
        if (!rest.isEmpty()) {
            sendError("INVALID_FORMAT", "CLEAR takes no arguments");
            return;
        }
        board.clear();
        sendOk("BOARD_CLEARED");
    }

    private void handleDisconnect() {
        out.println("OK bye");
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
