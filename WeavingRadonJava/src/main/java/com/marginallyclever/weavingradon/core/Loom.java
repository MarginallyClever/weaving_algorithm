package com.marginallyclever.weavingradon.core;

import com.marginallyclever.weavingradon.WeavingApp;

import javax.swing.event.EventListenerList;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
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

    public final List<Point> nails = new ArrayList<>();
    public final List<LoomThread> selectedThreads = new ArrayList<>();
    public final List<LoomThread> allThreads = new ArrayList<>();

    private final EventListenerList listeners = new EventListenerList();

    public void addListener(LoomEventListener ear) {
        listeners.add(LoomEventListener.class, ear);
    }

    public Loom(int radius,int numNails) {
        this.radius = radius;
        this.numNails = numNails;
        createNailsAndThreads();
    }

    public Loom(Loom other) {
        this.radius = other.radius;
        this.numNails = other.numNails;
        createNailsAndThreads();
    }

    // Effectively reset the loom.
    public void createNailsAndThreads() {
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
            nails.add(new Point(
                    (int)(Math.sin(angle) * radius),
                    (int)(Math.cos(angle) * radius)
            ));
        }
    }

    /**
     * allocate all the threads once.  includes start, end, theta, r, and color.
     */
    private void createThreads() {
        System.out.println("createThreads");
        selectedThreads.clear();
        allThreads.clear();

        for (int i = 0; i < numNails; i++) {
            Point start = nails.get(i);
            double sx = start.x;
            double sy = start.y;

            for (int j = i + 1; j < numNails; j++) {
                Point end = nails.get(j);
                double dx = end.x - sx;
                double dy = end.y - sy;

                double theta = Math.toDegrees(Math.atan2(-dx, dy));

                // Ensure theta is within [0-180)
                if(theta < 0) theta += 180;
                if(theta >= 180) theta -= 180;

                double angle = Math.toRadians(theta);
                int r = (int)(sx * Math.cos(angle)
                            + sy * Math.sin(angle));

                //System.out.println("theta="+theta+" r="+r);
                LoomThread thread = new LoomThread(start, end, theta, r, new Color(255,255,255, WeavingApp.ALPHA));
                allThreads.add(thread);
            }
        }
    }

    public void selectThread(LoomThread bestThread) {
        if (bestThread == null) return;
        //potentialThreads.remove(bestThread);
        selectedThreads.add(bestThread);

        for( var listener : listeners.getListeners(LoomEventListener.class) ) {
            listener.threadAdded(bestThread);
        }
    }

    public LoomThread findThreadClosestToThetaR(ThetaR target) {
        LoomThread nearestThread = null;
        double minDistance = Double.MAX_VALUE;

        for (LoomThread thread : allThreads) {
            double dTheta = thread.thetaR.theta - target.theta;
            double dr = thread.thetaR.r - target.r;
            double distanceSquared = Math.sqrt( dTheta*dTheta + dr*dr );
            if (distanceSquared < minDistance) {
                minDistance = distanceSquared;
                nearestThread = thread;
            }
        }

        if(minDistance>2) {
            System.out.println("near hit "+target + " vs "+ nearestThread );/*
            double previous = minDistance;

            for (LoomThread thread : selectedThreads) {
                double dTheta = thread.thetaR.theta - target.theta;
                double dr = thread.thetaR.r - target.r;
                double distanceSquared = Math.sqrt( dTheta*dTheta + dr*dr );
                if (distanceSquared < minDistance) {
                    minDistance = distanceSquared;
                    nearestThread = thread;
                }
            }
            System.out.println("difference "+ nearestThread + " "+previous+"->"+minDistance);*/
        }
        //System.out.println(threads.size() +"/"+remainingThreads.size());
        return nearestThread;
    }

    public boolean shouldStop() {
        return allThreads.size() <= numNails*0.2;
    }

    public int getNumNails() {
        return numNails;
    }

    public int getRadius() {
        return radius;
    }

    public int getNailIndex(Point point) {
        for(Point n : nails) {
            if(n.distanceSq(point) < 1) {
                return nails.indexOf(n);
            }
        }
        return -1;
    }
}
