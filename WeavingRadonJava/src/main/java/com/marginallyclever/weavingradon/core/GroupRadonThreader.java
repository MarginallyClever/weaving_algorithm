package com.marginallyclever.weavingradon.core;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * A GroupRadonThreader is a RadonThreader that can handle multiple colors.
 */
public class GroupRadonThreader implements RadonThreader {
    private final List<SingleThreader> threaders = new ArrayList<>();
    private Loom loom;

    public void addThreader(SingleThreader threader) {
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
        SingleThreader bestThreader = null;

        // find the best thetaR
        for(SingleThreader threader : threaders) {
            ThetaR t = threader.getNextBestThetaR();
            System.out.print(t.intensity+"\t");
            if(intensity < t.intensity) {
                intensity = t.intensity;
                best = t;
                bestThreader = threader;
            }
        }

        if(bestThreader==null) return null;

        System.out.println();

        LoomThread thread = loom.findThreadClosestToThetaR(best);
        thread.col = bestThreader.getColor();
        return thread;
    }

    @Override
    public ThetaR getNextBestThetaR() {
        double intensity = 0;
        ThetaR best = null;

        // find the best thetaR
        for(SingleThreader threader : threaders) {
            ThetaR t = threader.getNextBestThetaR();
            if(intensity < t.intensity) {
                intensity = t.intensity;
                best = t;
            }
        }

        return best;
    }

    @Override
    public void maskCurrentRadonByAllThreads() {/*
        System.out.println("filterRadonByThreads");
        int radius = loom.getRadius();
        var filter = new BufferedImage(radius*2, radius*2, BufferedImage.TYPE_INT_ARGB);
        int white = Color.WHITE.getRGB();

        for(ThreadColor thread : loom.potentialThreads) {
            filter.setRGB(thread.thetaR.theta, thread.thetaR.getY(radius), white);
        }
        for(ThreadColor thread : loom.selectedThreads) {
            filter.setRGB(thread.thetaR.theta, thread.thetaR.getY(radius), white);
        }

        for(SingleThreader s : threaders) {
            s.getRadonTransform().maskWith(filter,white);
        }*/
        for(SingleThreader s : threaders) {
            s.maskCurrentRadonByAllThreads();
        }
    }

    @Override
    public void setLoomAndImage(Loom loom, BufferedImage image) {
        this.loom = loom;
        for(SingleThreader threader : threaders) {
            threader.setLoomAndImage(loom, image);
        }
    }
}
