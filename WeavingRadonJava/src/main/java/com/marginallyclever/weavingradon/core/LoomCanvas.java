package com.marginallyclever.weavingradon.core;

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
    private final BufferedImage A; // Holds the complete drawing up to the current line
    private final BufferedImage B; // Temporary buffer for the new line
    int w2;
    int h2;

    public LoomCanvas(int width, int height) {
        width = Math.max(1,width);
        height = Math.max(1,height);
        w2 = width/2;
        h2 = height/2;
        A = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        B = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D gA = A.createGraphics();
        gA.setComposite(AlphaComposite.Clear); // Clear A before drawing
        gA.fillRect(0, 0, A.getWidth(), A.getHeight());
        gA.dispose();
    }

    public void addLineUnderneath(LoomThread tc) {
        Graphics2D gB = B.createGraphics();
        RenderHintHelper.setRenderHints(gB);
        gB.setComposite(AlphaComposite.Clear); // Clear B before drawing
        gB.fillRect(0, 0, B.getWidth(), B.getHeight());
        gB.setComposite(AlphaComposite.SrcOver);
        // Draw the new line on B
        gB.setColor(tc.col);
        gB.drawLine(tc.start.x+w2, tc.start.y+h2, tc.end.x+w2, tc.end.y+h2);
        gB.dispose();

        // Composite A onto B to produce A'
        Graphics2D gA = A.createGraphics();
        gA.drawImage(B, 0, 0, null);
        gA.dispose();
    }

    public BufferedImage getImage() {
        return A;
    }

    /**
     * Assume we started from a blank canvas.  Add the new line to A.
     * @param tc
     */
    public void addLineOnTop(LoomThread tc) {
        Graphics2D gA = A.createGraphics();
        RenderHintHelper.setRenderHints(gA);
        gA.setComposite(AlphaComposite.SrcOver);
        gA.setColor(tc.col);
        gA.drawLine(tc.start.x+w2, tc.start.y+h2, tc.end.x+w2, tc.end.y+h2);
        gA.dispose();
    }
}
