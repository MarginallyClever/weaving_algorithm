package com.marginallyclever.weavingradon.core;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;

public class LoomCanvasTest {
    // create a LoomCanvas, add some lines on top of each other, and save the result.
    // check that both methods work and mostly match.
    @Test
    public void testAddLines() throws Exception {
        LoomCanvas onTop = new LoomCanvas(100,100);
        LoomCanvas underneath = new LoomCanvas(100,100);
        for(int i=0;i<20;++i) {
            LoomThread thread = new LoomThread(randPoint(100), randPoint(100), 0, 0, Color.WHITE);
            onTop.addLineOnTop(thread);
            underneath.addLineUnderneath(thread);
        }

        // save the image
        ImageIO.write(onTop.getImage(),"png",new File("ontop.png"));
        ImageIO.write(underneath.getImage(),"png",new File("underneath.png"));
    }

    private Point randPoint(int max) {
        return new Point((int)(Math.random()*max)-max/2,(int)(Math.random()*max)-max/2);
    }
}
