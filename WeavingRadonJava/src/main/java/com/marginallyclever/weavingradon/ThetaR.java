package com.marginallyclever.weavingradon;

public class ThetaR {
    public int theta;
    public int r;

    public ThetaR(int theta, int r) {
        set(theta,r);
    }

    public void set(int theta, int r) {
        if(theta<0) throw new IllegalArgumentException("theta must be >= 0");
        if(theta>180) throw new IllegalArgumentException("theta must be <= 180");

        this.theta = theta;
        this.r = r;
    }

    public int getY(int radius) {
        return r+radius;
    }

    public void set(ThetaR current) {
        set(current.theta,current.r);
    }

    @Override
    public String toString() {
        return theta+","+r;
    }
}
