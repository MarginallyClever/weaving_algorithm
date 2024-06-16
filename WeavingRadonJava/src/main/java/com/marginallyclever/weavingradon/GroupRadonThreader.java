package com.marginallyclever.weavingradon;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Facade for a group of RadonThreaders.
 */
public class GroupRadonThreader implements RadonThreader {
    private final List<SingleThreader> threaders = new ArrayList<>();
    private Loom loom;

    void addThreader(SingleThreader threader) {
        threaders.add(threader);
    }

    @Override
    public RadonTransform getRadonTransform() {
        return threaders.get(0).getRadonTransform();
    }

    @Override
    public void subtractThreadFromRadon(ThreadColor thread) {
        threaders.forEach(t->t.subtractThreadFromRadon(thread));
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
            System.out.print(t+","+t.intensity+" ");
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
