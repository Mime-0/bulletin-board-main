import java.awt.*;
import javax.swing.*;

public class ClientGUI {
    private final BBConnection connection = new BBConnection();

    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField("4554");
    private final JTextArea outputArea = new JTextArea();
    private final BoardPanel boardPanel = new BoardPanel();

    private JButton connectBtn;
    private JButton disconnectBtn;
    private JButton postBtn;
    private JButton getBtn;
    private JButton pinBtn;
    private JButton unpinBtn;
    private JButton shakeBtn;
    private JButton clearBtn;

    private boolean connected = false;

    public ClientGUI() {
        JFrame frame = new JFrame("Bulletin Board Client");
        frame.setSize(700, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel top = new JPanel(new GridLayout(1, 5));
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(120, 0));

        connectBtn = new JButton("Connect");
        disconnectBtn = new JButton("Disconnect");

        postBtn = new JButton("POST");
        getBtn = new JButton("GET");
        pinBtn = new JButton("PIN");
        unpinBtn = new JButton("UNPIN");
        shakeBtn = new JButton("SHAKE");
        clearBtn = new JButton("CLEAR");

        top.add(new JLabel("Host:"));
        top.add(hostField);
        top.add(new JLabel("Port:"));
        top.add(portField);
        top.add(connectBtn);

        left.add(postBtn);
        left.add(getBtn);
        left.add(pinBtn);
        left.add(unpinBtn);
        left.add(shakeBtn);
        left.add(clearBtn);
        left.add(disconnectBtn);

        outputArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(outputArea);

        frame.setLayout(new BorderLayout());
        frame.add(top, BorderLayout.NORTH);
        frame.add(left, BorderLayout.WEST);
        frame.add(boardPanel, BorderLayout.CENTER);
        frame.add(scroll, BorderLayout.SOUTH);

        setConnectedState(false);

        connectBtn.addActionListener(e -> connect());
        disconnectBtn.addActionListener(e -> disconnect());

        postBtn.addActionListener(e -> doPost());
        getBtn.addActionListener(e -> doGet());

        pinBtn.addActionListener(e -> doPin());
        unpinBtn.addActionListener(e -> doUnpin());

        shakeBtn.addActionListener(e -> runCommandAndRefresh(CommandBuilder.buildShake()));
        clearBtn.addActionListener(e -> runCommandAndRefresh(CommandBuilder.buildClear()));

        frame.setVisible(true);
    }

    private void setConnectedState(boolean isConnected) {
        connected = isConnected;

        connectBtn.setEnabled(!isConnected);

        postBtn.setEnabled(isConnected);
        getBtn.setEnabled(isConnected);
        pinBtn.setEnabled(isConnected);
        unpinBtn.setEnabled(isConnected);
        shakeBtn.setEnabled(isConnected);
        clearBtn.setEnabled(isConnected);
        disconnectBtn.setEnabled(isConnected);
    }

    private void connect() {
        try {
            connection.connect(hostField.getText(), Integer.parseInt(portField.getText()));
            outputArea.append("Connected to server\n");

            setConnectedState(true);

            boardPanel.setNoteSize(connection.getNoteW(), connection.getNoteH());

            refreshBoard(false, "GET", true);
        } catch (Exception ex) {
            outputArea.append("Connection failed: " + ex.getMessage() + "\n");
            setConnectedState(false);
        }
    }

    private void disconnect() {
        if (connected) {
            try {
                outputArea.append("> DISCONNECT\n");
                java.util.List<String> resp = connection.sendCommand("DISCONNECT");
                for (String line : resp) outputArea.append(line + "\n");
            } catch (Exception ex) {
                outputArea.append("DISCONNECT send failed: " + ex.getMessage() + "\n");
            }
        }

        try {
            connection.disconnect();
            outputArea.append("Disconnected\n");
        } catch (Exception ex) {
            outputArea.append("Disconnect error: " + ex.getMessage() + "\n");
        }

        setConnectedState(false);
        boardPanel.clearNotes();
    }

    private void doPost() {
        if (!connected) return;

        try {
            String xs = JOptionPane.showInputDialog(null, "x coordinate:");
            if (xs == null) return;
            String ys = JOptionPane.showInputDialog(null, "y coordinate:");
            if (ys == null) return;
            String color = JOptionPane.showInputDialog(null, "color (e.g., red, green, blue, white):");
            if (color == null) return;
            String msg = JOptionPane.showInputDialog(null, "message:");
            if (msg == null) return;

            int x = Integer.parseInt(xs.trim());
            int y = Integer.parseInt(ys.trim());

            if (x < 0 || y < 0) {
                outputArea.append("Client validation: coordinates must be non-negative\n");
                return;
            }
            if (msg.trim().isEmpty()) {
                outputArea.append("Client validation: message cannot be empty\n");
                return;
            }

            String cmd = CommandBuilder.buildPost(x, y, color.trim(), msg.trim());
            runCommandAndRefresh(cmd);

        } catch (NumberFormatException ex) {
            outputArea.append("Client validation: x and y must be integers\n");
        }
    }

    private void doPin() {
        if (!connected) return;

        try {
            String xs = JOptionPane.showInputDialog(null, "PIN x:");
            if (xs == null) return;
            String ys = JOptionPane.showInputDialog(null, "PIN y:");
            if (ys == null) return;

            int x = Integer.parseInt(xs.trim());
            int y = Integer.parseInt(ys.trim());

            if (x < 0 || y < 0) {
                outputArea.append("Client validation: coordinates must be non-negative\n");
                return;
            }

            runCommandAndRefresh(CommandBuilder.buildPin(x, y));

        } catch (NumberFormatException ex) {
            outputArea.append("Client validation: x and y must be integers\n");
        }
    }

    private void doUnpin() {
        if (!connected) return;

        try {
            String xs = JOptionPane.showInputDialog(null, "UNPIN x:");
            if (xs == null) return;
            String ys = JOptionPane.showInputDialog(null, "UNPIN y:");
            if (ys == null) return;

            int x = Integer.parseInt(xs.trim());
            int y = Integer.parseInt(ys.trim());

            if (x < 0 || y < 0) {
                outputArea.append("Client validation: coordinates must be non-negative\n");
                return;
            }

            runCommandAndRefresh(CommandBuilder.buildUnpin(x, y));

        } catch (NumberFormatException ex) {
            outputArea.append("Client validation: x and y must be integers\n");
        }
    }

    private void doGet() {
        if (!connected) return;

        String pinsOnlyStr = JOptionPane.showInputDialog(null, "Search pins? (y/n)");
        if (pinsOnlyStr == null) return;
        boolean pinsOnly = pinsOnlyStr.trim().equalsIgnoreCase("y");

        if (pinsOnly) {
            refreshPinsOnly(true);
            return;
        }

        String color = JOptionPane.showInputDialog(null, "GET filter (optional): colour (leave blank for any)");
        if (color == null) return;

        String contains = JOptionPane.showInputDialog(null, "GET filter (optional): contains as \"x y\" (leave blank for none)");
        if (contains == null) return;

        String refers = JOptionPane.showInputDialog(null, "GET filter (optional): refersTo text (leave blank for none)");
        if (refers == null) return;

        String cmd = buildGetCommand(color, contains, refers);
        refreshBoard(true, cmd, false);
    }


    private String buildGetCommand(String color, String contains, String refers) {
        String cmd = "GET";

        if (color != null) color = color.trim();
        if (contains != null) contains = contains.trim();
        if (refers != null) refers = refers.trim();

        if (!color.isEmpty()) {
            cmd += " colour=" + color;
        }

        if (!contains.isEmpty()) {
            String[] parts = contains.split("\\s+");
            if (parts.length == 2) {
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    if (x >= 0 && y >= 0) {
                        cmd += " contains=" + x + " " + y;
                    } else {
                        outputArea.append("Client validation: contains x y must be non-negative\n");
                    }
                } catch (NumberFormatException e) {
                    outputArea.append("Client validation: contains must be two integers like: 10 20\n");
                }
            } else {
                outputArea.append("Client validation: contains must be two values like: 10 20\n");
            }
        }

        if (!refers.isEmpty()) {
            cmd += " refersTo=" + refers;
        }

        return cmd;
    }

    private void runCommandAndRefresh(String cmd) {
        if (!connected) return;

        try {
            outputArea.append("> " + cmd + "\n");
            java.util.List<String> resp = connection.sendCommand(cmd);
            for (String line : resp) outputArea.append(line + "\n");

            String last = resp.get(resp.size() - 1);
            if (last.startsWith("OK")) {
                refreshBoard(false, "GET", true);
            }
        } catch (Exception ex) {
            outputArea.append("Client error: " + ex.getMessage() + "\n");
        }
    }


    private void refreshPinsOnly(boolean log) {
        if (!connected) return;

        try {
            if (log) outputArea.append("> GET\n");
            java.util.List<String> noteResp = connection.sendCommand("GET");
            java.util.List<ClientNote> notes = new java.util.ArrayList<>();

            for (String line : noteResp) {
                if (line.startsWith("NOTE ")) {
                    String[] parts = line.split("\\s+", 5);
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    String color = parts[3];
                    String msg = (parts.length >= 5) ? parts[4] : "";
                    notes.add(new ClientNote(x, y, color, msg));
                }
            }

            if (log) outputArea.append("> GET PINS\n");
            java.util.List<String> pinResp = connection.sendCommand("GET PINS");
            java.util.List<int[]> pins = new java.util.ArrayList<>();

            for (String line : pinResp) {
                if (line.startsWith("PIN ")) {
                    String[] parts = line.split("\\s+");
                    pins.add(new int[]{ Integer.parseInt(parts[1]), Integer.parseInt(parts[2]) });
                }
            }

            java.util.List<ClientNote> pinnedNotes = new java.util.ArrayList<>();
            for (ClientNote n : notes) {
                boolean hasPin = false;
                for (int[] p : pins) {
                    if (containsPoint(n, p[0], p[1])) {
                        hasPin = true;
                    }
                }
                if (hasPin) pinnedNotes.add(n);
            }

            boardPanel.setNotes(pinnedNotes);
            boardPanel.applyPins(pins);

            if (log) {
                for (String line : noteResp) outputArea.append(line + "\n");
                for (String line : pinResp) outputArea.append(line + "\n");
            }

        } catch (Exception ex) {
            outputArea.append("GET PINS failed: " + ex.getMessage() + "\n");
        }
    }

    private boolean containsPoint(ClientNote n, int px, int py) {
        int noteW = connection.getNoteW();
        int noteH = connection.getNoteH();
        return (px >= n.x && px < n.x + noteW &&
                py >= n.y && py < n.y + noteH);
    }


    private void refreshBoard(boolean log, String getCmd, boolean fetchPins) {
        if (!connected) return;

        try {
            if (log) outputArea.append("> " + getCmd + "\n");

            java.util.List<String> noteResp = connection.sendCommand(getCmd);
            java.util.List<ClientNote> notes = new java.util.ArrayList<>();

            for (String line : noteResp) {
                if (line.startsWith("NOTE ")) {
                    String[] parts = line.split("\\s+", 5);
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    String color = parts[3];
                    String msg = (parts.length >= 5) ? parts[4] : "";
                    notes.add(new ClientNote(x, y, color, msg));
                }
            }
            boardPanel.setNotes(notes);

            if (fetchPins) {
                if (log) outputArea.append("> GET PINS\n");

                java.util.List<String> pinResp = connection.sendCommand("GET PINS");
                java.util.List<int[]> pins = new java.util.ArrayList<>();
                for (String line : pinResp) {
                    if (line.startsWith("PIN ")) {
                        String[] parts = line.split("\\s+");
                        pins.add(new int[]{ Integer.parseInt(parts[1]), Integer.parseInt(parts[2]) });
                    }
                }
                boardPanel.applyPins(pins);

                if (log) {
                    for (String line : noteResp) outputArea.append(line + "\n");
                    for (String line : pinResp) outputArea.append(line + "\n");
                }
            } else {
                boardPanel.applyPins(new java.util.ArrayList<>());

                if (log) {
                    for (String line : noteResp) outputArea.append(line + "\n");
                }
            }

        } catch (Exception ex) {
            outputArea.append("Refresh failed: " + ex.getMessage() + "\n");
        }
    }
}
