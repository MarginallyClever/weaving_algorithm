package com.marginallyclever.weavingradon.core;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * A GroupRadonThreader is a RadonThreader that can handle multiple colors.
 */
public class MulticolorThreader implements RadonThreader {
    private final List<MonochromaticThreader> threaders = new ArrayList<>();
    private Loom loom;

    public void addThreader(MonochromaticThreader threader) {
        threaders.add(threader);
    }

    @Override
    public RadonTransform getRadonTransform() {
        return threaders.getFirst().getRadonTransform();
    }

    @Override
    public void subtractThreadFromRadon(LoomThread thread) {
        LoomThread thread2 = new LoomThread(thread);
        thread2.col = new Color(255, 255, 255);
        BufferedImage oneThreadOnCanvas = getRadonTransform().drawOneThread(thread2);
        RadonTransform oneThreadRadonTransform = new RadonTransform(oneThreadOnCanvas);
        threaders.forEach(t->t.getRadonTransform().subtract(oneThreadRadonTransform));
    }

    @Override
    public void addNextBestThread() {
        if (loom.potentialThreads.isEmpty()) return;
        LoomThread bestThread = getNextBestThread();
        loom.addNextBestThread(bestThread);
        subtractThreadFromRadon(bestThread);
    }

    /**
     * Search every radon transform for the next best thread.
     * @return
     */
    public LoomThread getNextBestThread() {
        double intensity = 0;
        ThetaR best = null;
        MonochromaticThreader bestThreader = null;

        // find the best thetaR
        for(MonochromaticThreader threader : threaders) {
            ThetaR t = threader.getNextBestThetaR();
            //System.out.print(t.intensity+"\t");
            if(intensity < t.intensity) {
                intensity = t.intensity;
                best = t;
                bestThreader = threader;
            }
        }

        if(bestThreader==null) return null;

        System.out.println(bestThreader.getColor());

        LoomThread thread = loom.findThreadClosestToThetaR(best);
        thread.col = bestThreader.getColor();
        return thread;
    }

    @Override
    public ThetaR getNextBestThetaR() {
        double intensity = 0;
        ThetaR best = null;

        // find the best thetaR
        for(MonochromaticThreader threader : threaders) {
            ThetaR t = threader.getNextBestThetaR();
            if(intensity < t.intensity) {
                intensity = t.intensity;
                best = t;
            }
        }

        return best;
    }

    @Override
    public void maskCurrentRadonByAllThreads() {
        System.out.println("filterRadonByThreads");
        // generate the filter image
        int radius = loom.getRadius();
        var filter = new BufferedImage(radius*2, radius*2, BufferedImage.TYPE_INT_ARGB);
        int white = Color.WHITE.getRGB();
        for(LoomThread thread : loom.potentialThreads) {
            filter.setRGB(thread.thetaR.theta, thread.thetaR.getY(radius), white);
        }
        for(LoomThread thread : loom.selectedThreads) {
            filter.setRGB(thread.thetaR.theta, thread.thetaR.getY(radius), white);
        }
        // apply the filter
        for(MonochromaticThreader s : threaders) {
            s.getRadonTransform().maskWith(filter,white);
        }
    }

    @Override
    public void setLoomAndImage(Loom loom, BufferedImage image) {
        this.loom = loom;
        for(MonochromaticThreader threader : threaders) {
            threader.setLoomAndImage(loom, image);
        }
    }
}
