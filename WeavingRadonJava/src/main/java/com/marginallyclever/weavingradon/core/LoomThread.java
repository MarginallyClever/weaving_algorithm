package com.marginallyclever.weavingradon.core;

import javax.vecmath.Vector2d;
import java.awt.*;

/**
 * Thread on a Loom.
 */
public class LoomThread {
    public final Point start;  // xy
    public final Point end;  // xy
    public final ThetaR thetaR;
    public Color col;  // rgba

    public LoomThread(Point start, Point end, double theta, int r, Color col) {
        this.start = start;
        this.end = end;
        this.thetaR = new ThetaR(theta, r,0);
        this.col = col;
    }

    public LoomThread(LoomThread b) {
        this.start = new Point(b.start);
        this.end = new Point(b.end);
        this.thetaR = new ThetaR(b.thetaR);
        this.col = new Color(b.col.getRGB());
    }

    public void display(Graphics g2) {
        g2.setColor(col);
        g2.drawLine(start.x, start.y, end.x, end.y);
        g2.dispose();
    }

    @Override
    public String toString() {
        return thetaR+","+ start +","+end+","+ col.getRed()+","+ col.getGreen()+","+ col.getBlue()+","+ col.getAlpha();
    }
}