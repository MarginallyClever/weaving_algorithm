package com.marginallyclever.weavingradon;

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

    void addThreader(SingleThreader threader) {
        threaders.add(threader);
    }

    @Override
    public RadonTransform getRadonTransform() {
        return threaders.getFirst().getRadonTransform();
    }

    @Override
    public void subtractThreadFromRadon(ThreadColor thread) {
        ThreadColor thread2 = new ThreadColor(thread);
        thread2.col = new Color(255, 255, 255);
        BufferedImage oneThreadOnCanvas = getRadonTransform().drawOneThread(thread2);
        RadonTransform oneThreadRadonTransform = new RadonTransform(oneThreadOnCanvas);
        threaders.forEach(t->t.getRadonTransform().subtract(oneThreadRadonTransform));
    }

    @Override
    public void addNextBestThread() {
        if (loom.potentialThreads.isEmpty()) return;
        ThreadColor bestThread = getNextBestThread();
        loom.addNextBestThread(bestThread);
        subtractThreadFromRadon(bestThread);
    }

    /**
     * Search every radon transform for the next best thread.
     * @return
     */
    public ThreadColor getNextBestThread() {
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

        ThreadColor thread = loom.findThreadClosestToThetaR(best);
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
    public void maskCurrentRadonByAllThreads() {
        threaders.forEach(SingleThreader::maskCurrentRadonByAllThreads);
    }

    @Override
    public void setLoomAndImage(Loom loom, BufferedImage image) {
        this.loom = loom;
        for(SingleThreader threader : threaders) {
            threader.setLoomAndImage(loom, image);
        }
    }
}
