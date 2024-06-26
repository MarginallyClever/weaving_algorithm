package com.marginallyclever.weavingradon.core;

import com.marginallyclever.weavingradon.ui.RadonPanel;

import java.awt.image.BufferedImage;

public interface RayIllustrator {
    void setRadon(RadonThreader radonThreader, RadonPanel singleRadon);
    void highlightLine(ThetaR tr);
    void hideLine();
    void setLoomAndImage(Loom loom, BufferedImage grey);
}
