package com.marginallyclever.weavingradon.core;

import com.marginallyclever.weavingradon.WeavingApp;

import javax.vecmath.Vector2d;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Storage for the nails and thread on a loom.  The nails are split into two groups:</p>
 * <ul>
 *     <li>potential threads are all the threads that could be added to the loom.</li>
 *     <li>selected threads are the threads that have been added to the loom.</li>
 * </ul>
 */
public class Loom {
    public int numNails;
    public int radius;

    public final List<Vector2d> nails = new ArrayList<>();
    public final List<ThreadColor> selectedThreads = new ArrayList<>();
    public final List<ThreadColor> potentialThreads = new ArrayList<>();

    public Loom(int radius,int numNails) {
        this.radius = radius;
        this.numNails = numNails;
        reset();
    }

    public void reset() {
        createNails();
        createThreads();
    }

    /**
     * Don't forget to call reset() after changing this value.
     * @param radius the radius of the loom
     */
    public void setRadius(int radius) {
        this.radius = radius;
    }

    /**
     * Don't forget to call reset() after changing this value.
     * @param numNails the number of nails on the loom
     */
    public void setNumNails(int numNails) {
        this.numNails = numNails;
    }

    private void createNails() {
        System.out.println("createNails");
        nails.clear();
        for(int i = 0; i< numNails; ++i) {
            double angle = i * Math.PI * 2 / numNails;
            nails.add(new Vector2d(
                    radius + Math.sin(angle) * radius,
                    radius + Math.cos(angle) * radius
            ));
        }
    }

    /**
     * allocate all the threads once.  includes start, end, theta, r, and color.
     */
    private void createThreads() {
        System.out.println("createThreads");
        selectedThreads.clear();
        potentialThreads.clear();

        double maxR = 0;
        for (int i = 0; i < numNails; i++) {
            Vector2d start = nails.get(i);
            double sx = start.x - radius;
            double sy = start.y - radius;

            for (int j = i + 1; j < numNails; j++) {
                Vector2d end = nails.get(j);
                double dx = end.x - start.x;
                double dy = end.y - start.y;
                double len = Math.sqrt(dx * dx + dy * dy);

                int theta = (int)Math.toDegrees(Math.atan2(-dx, dy));

                // Ensure theta is within [0-180)
                if(theta < 0) theta += 180;
                if(theta >= 180) theta -= 180;

                double angle = Math.toRadians(theta);
                int r = (int)(sx * Math.cos(angle)
                        + sy * Math.sin(angle));

                //System.out.println("theta="+theta+" r="+r);
                maxR = Math.max(maxR, Math.abs(r));
                ThreadColor thread = new ThreadColor(start, end, new ThetaR(theta, r), new Color(255, 255, 255, WeavingApp.ALPHA),len);
                potentialThreads.add(thread);
            }
        }
    }

    public void addNextBestThread(ThreadColor bestThread) {
        if (bestThread == null) return;
        potentialThreads.remove(bestThread);
        selectedThreads.add(bestThread);
    }

    public ThreadColor findThreadClosestToThetaR(ThetaR target) {
        ThreadColor nearestThread = null;
        double minDistance = Double.MAX_VALUE;

        for (ThreadColor thread : potentialThreads) {
            double x = thread.thetaR.theta - target.theta;
            double y = thread.thetaR.r - target.r;
            double distanceSquared = ( x*x + y*y );
            if (distanceSquared < minDistance) {
                minDistance = distanceSquared;
                nearestThread = thread;
            }
        }

        //if(minDistance>2) return null;
        //System.out.println("matches "+ nearestThread );
        //System.out.println(threads.size() +"/"+remainingThreads.size());
        return nearestThread;
    }

    public boolean shouldStop() {
        return potentialThreads.size() <= numNails*0.2;
    }

    public int getNumNails() {
        return numNails;
    }

    public int getRadius() {
        return radius;
    }
}
