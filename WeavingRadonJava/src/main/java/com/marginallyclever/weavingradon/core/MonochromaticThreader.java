package com.marginallyclever.weavingradon.core;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A {@link MonochromaticThreader} is a concrete implementation of a RadonThreader.  It can only handle one color.
 */
public class MonochromaticThreader extends RadonThreader {
    private final Color threaderColor;

    public MonochromaticThreader(Color threaderColor) {
        super();
        this.threaderColor = threaderColor;
    }

    @Override
    public void setImage(BufferedImage referenceImage) {
        BufferedImage filtered = makeFilteredImage(referenceImage, getColor());
        super.setImage(filtered);
    }

    /**
     * get the next best thread, add it to the loom, and subtract it from the current radon image.
     */
    @Override
    public void addNextBestThread() {
        if (loom.allThreads.isEmpty()) return;
        ThetaR tr = getBestThetaR();
        //System.out.println("best thetaR: " + tr);
        LoomThread bestThread = loom.findThreadClosestToThetaR(tr);
        //System.out.println("best thread: " + bestThread);
        loom.selectThread(bestThread);
        radonTransform.subtractThread(bestThread);
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
                int r = Math.abs(c.getRed()   - target.getRed()  );
                int g = Math.abs(c.getGreen() - target.getGreen());
                int b = Math.abs(c.getBlue()  - target.getBlue() );
                int v = 255 - (int)Math.min(255, Math.max(0, (r + g + b) / 3.0));
                channel.setRGB(x,y,new Color(v,v,v).getRGB());
            }
        }
        return channel;
    }
}