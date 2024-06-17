package com.marginallyclever.weavingradon.core;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A MonochromaticThreader is a concrete implementation of a RadonThreader.  It can only handle one color.
 */
public class MonochromaticThreader implements RadonThreader {
    private final Color threaderColor;
    public int radius;
    private RadonTransform radonTransform;
    private Loom loom;

    public MonochromaticThreader(Color threaderColor) {
        super();
        this.threaderColor = threaderColor;
    }

    public void setLoom(Loom loom) {
        this.loom = loom;
    }

    public void setImage(BufferedImage referenceImage) {
        radius = referenceImage.getWidth() / 2;

        BufferedImage filtered = makeFilteredImage(referenceImage, getColor());
        radonTransform = new RadonTransform(filtered);
    }

    /**
     * Mask the current radon image with the remaining threads.
     */
    public void maskCurrentRadonByAllThreads() {
        System.out.println("filterRadonByThreads "+this.getColor());
        var filter = new BufferedImage(radonTransform.getWidth(), radonTransform.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int white = Color.WHITE.getRGB();

        for(LoomThread thread : loom.potentialThreads) {
            filter.setRGB(thread.thetaR.theta, thread.thetaR.getY(radius), white);
        }
        for(LoomThread thread : loom.selectedThreads) {
            filter.setRGB(thread.thetaR.theta, thread.thetaR.getY(radius), white);
        }

        radonTransform.maskWith(filter,white);
    }

    /**
     * get the next best thread, add it to the loom, and subtract it from the current radon image.
     */
    @Override
    public void addNextBestThread() {
        if (loom.potentialThreads.isEmpty()) return;
        LoomThread bestThread = loom.findThreadClosestToThetaR(getNextBestThetaR());
        loom.addNextBestThread(bestThread);
        subtractThreadFromRadon(bestThread);
    }

    /**
     * Sets the bestTheta/bestR for the next thread to add.
     * bestTheta is in the range 0...180.
     * bestR is in the range -radius...radius.
     */
    public ThetaR getNextBestThetaR() {
        ThetaR bestFound = new ThetaR(0,0,0);
        // Find the pixel with the maximum intensity in the current radon transform
        for(int r=-radius;r<radius;++r) {
            for(int theta = 0; theta<180; ++theta) {
                int intensity = radonTransform.getIntensity(theta, r+radius);
                if (intensity > bestFound.intensity) {
                    bestFound.set(theta,r,intensity);
                }
            }
        }
        return bestFound;
    }

    @Override
    public void subtractThreadFromRadon(LoomThread thread) {
        radonTransform.subtractThread(thread);
    }

    public RadonTransform getRadonTransform() {
        return radonTransform;
    }

    public void setLoomAndImage(Loom loom, BufferedImage image) {
        setLoom(loom);
        setImage(image);
    }

    public Color getColor() {
        return threaderColor;
    }

    /**
     * Creates a new greyscale image such that the intensity is the inverse of the distance from the target color.
     * This means a red pixel and a red target will be white, while a blue pixel and a red target will be black.
     * @param original the image to convert
     * @param target the color to measure distance from
     * @return a new image
     */
    private BufferedImage makeFilteredImage(BufferedImage original, Color target) {
        BufferedImage channel = new BufferedImage(original.getWidth(),original.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<original.getHeight();++y) {
            for(int x=0;x<original.getWidth();++x) {
                int rgb = original.getRGB(x,y);
                Color c = new Color(rgb);
                int r = Math.abs(c.getRed()-target.getRed());
                int g = Math.abs(c.getGreen()-target.getGreen());
                int b = Math.abs(c.getBlue()-target.getBlue());
                int v = 255 - (int)Math.min(255, Math.max(0, (r + g + b) / 3.0));
                channel.setRGB(x,y,new Color(v,v,v).getRGB());
            }
        }
        return channel;
    }
}