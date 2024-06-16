package com.marginallyclever.weavingradon;

import java.awt.image.BufferedImage;

public interface RadonThreader {
    void addNextBestThread();
    void maskCurrentRadonByAllThreads();

    ThetaR getNextBestThetaR();

    RadonTransform getRadonTransform();

    void subtractThreadFromRadon(ThreadColor thread);

    void setLoomAndImage(Loom loom, BufferedImage grey);
}
