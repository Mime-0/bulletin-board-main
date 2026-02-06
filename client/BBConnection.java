import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BBConnection {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private boolean connected = false;

    private int boardW = -1;
    private int boardH = -1;
    private int noteW = -1;
    private int noteH = -1;
    private final Set<String> colors = new LinkedHashSet<>();

    public void connect(String host, int port) throws IOException {
        disconnectQuietly();

        boardW = boardH = noteW = noteH = -1;
        colors.clear();
        connected = false;

        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        readHandshake();
        connected = true;
    }

    private void readHandshake() throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("BOARD ")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    boardW = parseInt(parts[1]);
                    boardH = parseInt(parts[2]);
                }
            } else if (line.startsWith("NOTE ")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    noteW = parseInt(parts[1]);
                    noteH = parseInt(parts[2]);
                }
            } else if (line.startsWith("COLORS")) {
                String[] parts = line.split("\\s+");
                for (int i = 1; i < parts.length; i++) {
                    if (!parts[i].isBlank()) colors.add(parts[i].trim());
                }
            } else if (line.startsWith("OK")) {
                break;
            } else if (line.startsWith("ERROR")) {
                throw new IOException("Server error during handshake: " + line);
            }
        }

        if (boardW <= 0 || boardH <= 0 || noteW <= 0 || noteH <= 0) {
            throw new IOException("Handshake missing/invalid BOARD/NOTE dimensions.");
        }
        if (colors.isEmpty()) {
            throw new IOException("Handshake missing COLORS.");
        }
    }

    public List<String> sendCommand(String command) throws IOException {
        if (!connected || socket == null || socket.isClosed()) {
            throw new IOException("Not connected");
        }

        List<String> response = new ArrayList<>();
        out.println(command);

        String line;
        while ((line = in.readLine()) != null) {
            response.add(line);
            if (line.startsWith("OK") || line.startsWith("ERROR")) {
                break;
            }
        }

        return response;
    }

    public void disconnect() throws IOException {
        connected = false;
        if (socket != null) socket.close();
        socket = null;
        in = null;
        out = null;
    }

    private void disconnectQuietly() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        } finally {
            socket = null;
            in = null;
            out = null;
            connected = false;
        }
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Getters used by ClientGUI
    public int getBoardW() { return boardW; }
    public int getBoardH() { return boardH; }
    public int getNoteW() { return noteW; }
    public int getNoteH() { return noteH; }
    public Set<String> getColors() { return new LinkedHashSet<>(colors); }
}
