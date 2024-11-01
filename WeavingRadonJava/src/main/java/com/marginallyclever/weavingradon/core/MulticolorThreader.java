package com.marginallyclever.weavingradon.core;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * A GroupRadonThreader is a RadonThreader that can handle multiple colors.
 */
public class MulticolorThreader extends RadonThreader {
    private final List<MonochromaticThreader> colors = new ArrayList<>();

    private static class ThreaderChoice {
        public MonochromaticThreader threader;
        public ThetaR bestThetaR;
    }

    @Override
    public void setLoomAndImage(Loom loom, BufferedImage image) {
        for(MonochromaticThreader threader : colors) {
            threader.setLoomAndImage(loom, image);
        }
        setLoom(loom);
    }

    @Override
    public void maskRadonTransformByAllThreads() {
        for(MonochromaticThreader threader : colors) {
            threader.maskRadonTransformByAllThreads();
        }
    }

    /**
     * Get the next best thread, add it to the loom, and subtract it from the current radon image.
     *
     * @return true if a thread was added, false if no threads are left.
     */
    @Override
    public boolean addNextBestThread() {
        if (loom.allThreads.isEmpty()) return false;
        ThreaderChoice choice = getBestThreaderChoice();
        System.out.println(loom.selectedThreads.size()+ " " + choice.bestThetaR.intensity);// choice.threader.getColor() + " @ "+choice.bestThetaR);
        if(choice.bestThetaR.intensity==0) return false;
        LoomThread bestThread = loom.findThreadClosestToThetaR(choice.bestThetaR);

        choice.threader.getRadonTransform().subtractThread(bestThread);

        LoomThread selection = new LoomThread(bestThread);
        selection.col = choice.threader.getColor();
        loom.selectThread(selection);
        return true;
    }

    /**
     * Finds the best thread in the best color to add to the loom.
     */
    public ThreaderChoice getBestThreaderChoice() {
        ThreaderChoice bestFound = new ThreaderChoice();

        double intensity = 0;
        for(MonochromaticThreader threader : colors) {
            var tr = threader.getBestThetaR();
            if(intensity < tr.intensity) {
                bestFound.threader = threader;
                bestFound.bestThetaR = tr;
                intensity = tr.intensity;
            }
        }
        return bestFound;
    }

    public void addThreader(MonochromaticThreader radonThreader) {
        colors.add(radonThreader);
    }
}