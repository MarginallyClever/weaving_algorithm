package com.marginallyclever.weavingradon.core;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A GroupRadonThreader is a RadonThreader that can handle multiple colors.
 */
public class MulticolorThreader extends RadonThreader {
    private final List<Color> colors = new ArrayList<>();

    public void addColor(Color c) {
        colors.add(c);
    }

    /**
     * get the next best thread, add it to the loom, and subtract it from the current radon image.
     */
    @Override
    public void addNextBestThread() {
        if (loom.allThreads.isEmpty()) return;
        ThetaR tr = getBestThetaR();
        LoomThread bestThread = loom.findThreadClosestToThetaR(tr);
        Color c = radonTransform.getColor(bestThread.thetaR.theta, bestThread.thetaR.r);
        bestThread.col = getNearestColor(c);
        loom.selectThread(bestThread);
        radonTransform.subtractThread(bestThread);
    }

    private Color getNearestColor(Color c) {
        Color nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;
        for(Color color : colors) {
            double distanceSquared = /*Math.sqrt*/(
                    Math.pow(color.getRed() - c.getRed(), 2) +
                    Math.pow(color.getGreen() - c.getGreen(), 2) +
                    Math.pow(color.getBlue() - c.getBlue(), 2)
            );
            if (distanceSquared < nearestDistanceSquared) {
                nearest = color;
                nearestDistanceSquared = distanceSquared;
            }
        }
        return nearest;
    }
}