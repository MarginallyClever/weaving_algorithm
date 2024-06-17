package com.marginallyclever.weavingradon;

import java.awt.image.BufferedImage;

/**
 * A RadonThreader uses a RadonTransform of an image to find the best thread to add to a Loom.
 * The cumulative effect is a weaving pattern on the Loom that approximates the image.
 */
public interface RadonThreader {
    void addNextBestThread();
    void maskCurrentRadonByAllThreads();

    ThetaR getNextBestThetaR();

    RadonTransform getRadonTransform();

    void subtractThreadFromRadon(ThreadColor thread);

    void setLoomAndImage(Loom loom, BufferedImage grey);
}
