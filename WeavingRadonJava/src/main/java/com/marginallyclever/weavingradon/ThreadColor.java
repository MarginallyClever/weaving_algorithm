package com.marginallyclever.weavingradon;

import javax.vecmath.Vector2d;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ThreadColor {
    final Vector2d start;  // xy
    final Vector2d end;  // xy
    final Color col;  // rgba
    final ThetaR thetaR;
    final double length;

    public ThreadColor(Vector2d start, Vector2d end, ThetaR thetaR, Color col,double length) {
        this.start = start;
        this.end = end;
        this.thetaR = thetaR;
        this.col = col;
        this.length = length;
    }

    public void display(BufferedImage pg) {
        Graphics g = pg.getGraphics();
        g.setColor(col);
        g.drawLine((int)start.x, (int)start.y, (int)end.x, (int)end.y);
    }

    @Override
    public String toString() {
        return thetaR+","+ start +","+end+","+ col.getRed()+","+ col.getGreen()+","+ col.getBlue()+","+ col.getAlpha();
    }
}