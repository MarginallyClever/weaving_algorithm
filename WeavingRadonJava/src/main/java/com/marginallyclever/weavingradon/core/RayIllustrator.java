package com.marginallyclever.weavingradon.core;

import com.marginallyclever.weavingradon.ui.RadonPanel;

import java.awt.image.BufferedImage;

public interface RayIllustrator {
    void setRadon(RadonThreader radonThreader, RadonPanel singleRadon);
    void highlightLine(int theta, int r);
    void setLoomAndImage(Loom loom, BufferedImage grey);
}
