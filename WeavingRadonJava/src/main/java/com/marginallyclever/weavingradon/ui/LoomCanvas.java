package com.marginallyclever.weavingradon.ui;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * LoomCanvas makes the process of adding new lines to the loom a linear-time process.
 * As the list of lines grows, so does the length of time to redraw the entire set.
 * Instead, I'd like to
 * - allocate a RGBA BufferedImage A to hold the current set of lines;
 * - draw the new line to a separate buffer B; and
 * - paint A onto B to produce A', the new image.
 */
public class LoomCanvas {
    private BufferedImage A; // Holds the complete drawing up to the current line
    private BufferedImage B; // Temporary buffer for the new line

    public LoomCanvas(int width, int height) {
        A = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        B = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    public void addLine(int x1, int y1, int x2, int y2, Color color) {
        Graphics2D gB = B.createGraphics();
        gB.setComposite(AlphaComposite.Clear); // Clear B before drawing
        gB.fillRect(0, 0, B.getWidth(), B.getHeight());
        gB.setComposite(AlphaComposite.SrcOver);

        // Draw the new line on B
        gB.setColor(color);
        gB.drawLine(x1, y1, x2, y2);
        gB.dispose();

        // Composite A onto B to produce A'
        Graphics2D gA = A.createGraphics();
        gA.drawImage(B, 0, 0, null);
        gA.dispose();
    }

    public BufferedImage getImage() {
        return A;
    }
}
