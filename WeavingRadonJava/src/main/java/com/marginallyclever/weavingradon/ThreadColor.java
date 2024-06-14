package com.marginallyclever.weavingradon;

import javax.vecmath.Vector2d;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ThreadColor {
    Vector2d start;  // xy
    Vector2d end;  // xy
    Color col;  // rgba
    int theta;  // degrees
    int r;  // distance, unit unknown

    public ThreadColor(Vector2d start, Vector2d end, int theta, int r, Color col) {
        this.start = start;
        this.end = end;
        this.theta = theta;
        this.r = r;
        this.col = col;
    }

    public void display(BufferedImage pg) {
        Graphics g = pg.getGraphics();
        g.setColor(col);
        g.drawLine((int)start.x, (int)start.y, (int)end.x, (int)end.y);
    }

    @Override
    public String toString() {
        return theta+","+r+","+ start +","+end+","+ col.getRed()+","+ col.getGreen()+","+ col.getBlue()+","+ col.getAlpha();
    }

    public int getY(int radius) {
        return r+radius;
    }
}