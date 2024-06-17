package com.marginallyclever.weavingradon.core;

public class ThetaR {
    public int theta;
    public int r;
    public double intensity=0;

    public ThetaR(int theta, int r,double intensity) {
        set(theta,r,intensity);
    }

    public ThetaR(ThetaR current) {
        set(current);
    }

    public void set(int theta, int r,double intensity) {
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
        return theta+","+r;
    }
}
