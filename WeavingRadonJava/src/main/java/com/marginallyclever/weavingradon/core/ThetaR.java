package com.marginallyclever.weavingradon.core;

import java.awt.*;

public class ThetaR {
    public double theta=0;
    public int r=0;
    public double intensity=0;

    public ThetaR(double theta, int r,double intensity) {
        set(theta,r,intensity);
    }

    public ThetaR(ThetaR current) {
        set(current.theta,current.r,current.intensity);
    }

    public void set(double theta, int r,double intensity) {
        if(theta<0) throw new IllegalArgumentException("theta must be >= 0");
        if(theta>180) throw new IllegalArgumentException("theta must be <= 180");
        if(intensity<0) throw new IllegalArgumentException("intensity must be >= 0");

        this.theta = theta;
        this.r = r;
        this.intensity = intensity;
    }

    public int getY(int radius) {
        return r+radius;
    }

    public void set(ThetaR current) {
        set(current.theta,current.r,current.intensity);
    }

    @Override
    public String toString() {
        return theta+","+r+","+intensity;
    }

    public void display(Graphics2D g2,int radius) {
        double radians = Math.toRadians(theta);
        int h2 = radius, w2 = radius;

        double s = Math.sin(radians);
        double c = Math.cos(radians);
        double d = Math.sqrt(w2*w2 - r*r);
        int x0 = (int)(w2 + r * c - d * s);
        int y0 = (int)(h2 + r * s + d * c);
        int x1 = (int)(w2 + r * c + d * s);
        int y1 = (int)(h2 + r * s - d * c);

        g2.drawLine(x0,y0,x1,y1);
    }
}
