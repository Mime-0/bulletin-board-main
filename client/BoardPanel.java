import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class BoardPanel extends JPanel {
    private int noteW = 60;
    private int noteH = 40;

    private final List<ClientNote> notes = new ArrayList<>();
    private final List<int[]> pins = new ArrayList<>();

    public BoardPanel() {
        setBackground(Color.WHITE); 
    }

    public void setNoteSize(int w, int h) {
        noteW = w;
        noteH = h;
        repaint();
    }

    public void setNotes(List<ClientNote> newNotes) {
        notes.clear();
        notes.addAll(newNotes);
        repaint();
    }

    public void clearNotes() {
        notes.clear();
        pins.clear();
        repaint();
    }

    public void applyPins(List<int[]> newPins) {
        pins.clear();
        pins.addAll(newPins);

        for (ClientNote n : notes) {
            while (n.isPinned()) n.removePin();
        }

        for (int[] p : pins) {
            int px = p[0];
            int py = p[1];
            for (ClientNote n : notes) {
                if (containsPoint(n, px, py)) n.addPin();
            }
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (ClientNote n : notes) {
            g.setColor(new Color(0, 0, 0, 40));
            g.fillRect(n.x + 3, n.y + 3, noteW, noteH);

            g.setColor(mapColor(n.color));
            g.fillRect(n.x, n.y, noteW, noteH);

            g.setColor(Color.BLACK);
            g.drawRect(n.x, n.y, noteW, noteH);

            drawWrappedText(g, n.message, n.x + 5, n.y + 15, noteW - 10);
        }

        g.setColor(Color.BLACK);
        for (int[] p : pins) {
            int px = p[0];
            int py = p[1];
            g.fillOval(px - 4, py - 4, 8, 8);
        }
    }

    private Color mapColor(String name) {
        if (name == null) return Color.LIGHT_GRAY;
        String c = name.toLowerCase();

        if (c.equals("red")) return Color.PINK;
        if (c.equals("green")) return Color.GREEN;
        if (c.equals("blue")) return Color.CYAN;
        if (c.equals("yellow")) return Color.YELLOW;
        if (c.equals("white")) return Color.WHITE;

        return Color.LIGHT_GRAY;
    }

    private void drawWrappedText(Graphics g, String text, int x, int y, int maxWidth) {
        if (text == null) return;

        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split(" ");
        String line = "";
        int lineY = y;

        for (String w : words) {
            String test = line.isEmpty() ? w : line + " " + w;
            if (fm.stringWidth(test) > maxWidth) {
                g.drawString(line, x, lineY);
                line = w;
                lineY += fm.getHeight();
            } else {
                line = test;
            }
        }

        if (!line.isEmpty()) g.drawString(line, x, lineY);
    }

    private boolean containsPoint(ClientNote n, int px, int py) {
        return (px >= n.x && px < n.x + noteW &&
                py >= n.y && py < n.y + noteH);
    }
}



