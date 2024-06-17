package com.marginallyclever.weavingradon.core;

import javax.vecmath.Vector2d;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Thread on a Loom.
 */
public class LoomThread {
    public final Vector2d start;  // xy
    public final Vector2d end;  // xy
    public final ThetaR thetaR;
    public final double length;
    public Color col;  // rgba

    public LoomThread(Vector2d start, Vector2d end, ThetaR thetaR, Color col, double length) {
        this.start = start;
        this.end = end;
        this.thetaR = thetaR;
        this.col = col;
        this.length = length;
    }

    public LoomThread(LoomThread b) {
        this.start = new Vector2d(b.start);
        this.end = new Vector2d(b.end);
        this.thetaR = new ThetaR(b.thetaR);
        this.col = new Color(b.col.getRGB());
        this.length = b.length;
    }

    public void display(BufferedImage image) {
        Graphics2D g2 = image.createGraphics();
        RenderHintHelper.setRenderHints(g2);

        g2.setColor(col);
        g2.drawLine((int)start.x, (int)start.y, (int)end.x, (int)end.y);
        g2.dispose();
    }

    @Override
    public String toString() {
        return thetaR+","+ start +","+end+","+ col.getRed()+","+ col.getGreen()+","+ col.getBlue()+","+ col.getAlpha();
    }
}