package com.marginallyclever.weavingradon.core;

import javax.vecmath.Vector2d;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ThreadColor {
    public final Vector2d start;  // xy
    public final Vector2d end;  // xy
    public final ThetaR thetaR;
    public final double length;
    public Color col;  // rgba

    public ThreadColor(Vector2d start, Vector2d end, ThetaR thetaR, Color col,double length) {
        this.start = start;
        this.end = end;
        this.thetaR = thetaR;
        this.col = col;
        this.length = length;
    }

    public ThreadColor(ThreadColor b) {
        this.start = new Vector2d(b.start);
        this.end = new Vector2d(b.end);
        this.thetaR = new ThetaR(b.thetaR.theta,b.thetaR.r);
        this.col = new Color(b.col.getRGB());
        this.length = b.length;
    }

    public void display(BufferedImage image) {
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2.setColor(col);
        g2.drawLine((int)start.x, (int)start.y, (int)end.x, (int)end.y);
        g2.dispose();
    }

    @Override
    public String toString() {
        return thetaR+","+ start +","+end+","+ col.getRed()+","+ col.getGreen()+","+ col.getBlue()+","+ col.getAlpha();
    }
}