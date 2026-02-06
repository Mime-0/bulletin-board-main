import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * Bulletin Board Server (CP372 Assignment 01).
 * Usage: java BBoard <port> <board_width> <board_height> <note_width> <note_height> <color1> ... <colorN>
 * Example: java BBoard 4554 200 100 20 10 red white green yellow
 */
public class BBoard {
    public static void main(String[] args) {
        if (args.length < 6) {
            System.err.println("Usage: java BBoard <port> <board_width> <board_height> <note_width> <note_height> <color1> ... <colorN>");
            System.exit(1);
        }

        int port = parseInt(args[0], "port");
        int boardW = parseInt(args[1], "board_width");
        int boardH = parseInt(args[2], "board_height");
        int noteW = parseInt(args[3], "note_width");
        int noteH = parseInt(args[4], "note_height");

        if (port <= 0 || port > 65535) {
            System.err.println("Error: port must be between 1 and 65535");
            System.exit(1);
        }
        if (boardW <= 0 || boardH <= 0 || noteW <= 0 || noteH <= 0) {
            System.err.println("Error: dimensions must be positive");
            System.exit(1);
        }
        if (noteW > boardW || noteH > boardH) {
            System.err.println("Error: note dimensions cannot exceed board dimensions");
            System.exit(1);
        }

        Set<String> colors = new HashSet<>();
        for (int i = 5; i < args.length; i++) {
            colors.add(args[i].trim());
        }
        if (colors.isEmpty()) {
            System.err.println("Error: at least one color is required");
            System.exit(1);
        }

        BoardState board = new BoardState(boardW, boardH, noteW, noteH, colors);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Bulletin Board server listening on port " + port);
            System.out.println("Board: " + boardW + "x" + boardH + ", Note: " + noteW + "x" + noteH + ", Colors: " + colors);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, board);
                Thread t = new Thread(handler);
                t.setDaemon(false);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static int parseInt(String s, String name) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            System.err.println("Error: invalid " + name + " '" + s + "'");
            System.exit(1);
            return 0;
        }
    }
}
