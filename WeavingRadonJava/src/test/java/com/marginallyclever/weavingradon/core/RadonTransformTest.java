package com.marginallyclever.weavingradon.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class RadonTransformTest {
    @Test
    public void testOneIntersection() {
        int radius = 100;
        int r = 0;
        int theta = 0;

        LoomThread thread = new LoomThread(
                new Point(radius,0),
                new Point(radius,radius*2),
                theta,
                r,
                Color.WHITE
        );
        RadonTransform rt = new RadonTransform(radius,thread);

        double c = rt.testIntersection(thread,theta,r);
        Assertions.assertEquals(radius,c);
        c = rt.testIntersection(thread,theta,r+1);
        Assertions.assertEquals(0,c);
        c = rt.testIntersection(thread,theta,r-1);
        Assertions.assertEquals(0,c);

        c = rt.testIntersection(thread,theta+90,r);
        Assertions.assertEquals(1,c);
        c = rt.testIntersection(thread,theta+90,r+1);
        Assertions.assertEquals(1,c);
        c = rt.testIntersection(thread,theta+90,r-1);
        Assertions.assertEquals(1,c);

        c = rt.testIntersection(thread,theta+45,r);
        assert(c>1 && c<radius);
    }

    // write sinMap to a file
    @Test
    public void testSinMap() throws Exception {
        BufferedImage img = new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB);
        RadonTransform rt = new RadonTransform(img);
        var map = rt.createImportanceMap();
        for(int x=0;x<100;++x) {
            for(int y=0;y<100;++y) {
                img.setRGB(x,y,Color.HSBtoRGB(1,1,(float)map[y*100+x]));
            }
        }
        // save the image
        ImageIO.write(img,"png",new File("sinMap.png"));
    }
}
