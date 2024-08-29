package com.marginallyclever.weavingradon.core;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A RadonThreader uses a RadonTransform of an image to find the best thread to add to a Loom.
 * The cumulative effect is a weaving pattern on the Loom that approximates the image.
 */
public abstract class RadonThreader {
    protected Loom loom;
    protected int radius;
    protected RadonTransform radonTransform;

    public void setLoomAndImage(Loom loom, BufferedImage image) {
        setLoom(loom);
        setImage(image);
    }

    public void setLoom(Loom loom) {
        this.loom = loom;
    }

    public void setImage(BufferedImage referenceImage) {
        radius = referenceImage.getWidth() / 2;
        radonTransform = new RadonTransform(referenceImage);
    }

    /**
     * get the next best thread, add it to the loom, and subtract it from the current radon image.
     *
     * @return false if there are no more threads to add.
     */
    abstract public boolean addNextBestThread();

    /**
     * Mask the current radon image with the current threads.
     * This is achieved by generating a mask of all possible threads and then masking the radon image with it.
     */
    public void maskRadonTransformByAllThreads() {
        System.out.println("filterRadonByThreads multicolor");
        var filter = new BufferedImage(radonTransform.getWidth(), radonTransform.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int white = Color.WHITE.getRGB();

        for(LoomThread thread : loom.allThreads) {
            filter.setRGB((int)thread.thetaR.theta, thread.thetaR.getY(radius), white);
        }
        for(LoomThread thread : loom.selectedThreads) {
            filter.setRGB((int)thread.thetaR.theta, thread.thetaR.getY(radius), white);
        }

        radonTransform.maskWith(filter,white);
    }

    /**
     * Sets the bestTheta/bestR for the next thread to add.<br/>
     * bestTheta is in the range 0...180.<br/>
     * bestR is in the range -radius...radius.
     */
    public ThetaR getBestThetaR() {
        ThetaR bestFound = new ThetaR(0,0,0);
        // Find the pixel with the maximum intensity in the current radon transform
        for(int r=-radius;r<radius;++r) {
            for(int theta = 0; theta<180; ++theta) {
                double intensity = radonTransform.getIntensity(theta, r);
                if (intensity > bestFound.intensity) {
                    bestFound.set(theta,r,intensity);
                }
            }
        }
        return bestFound;
    }

    public RadonTransform getRadonTransform() {
        return radonTransform;
    }
}
