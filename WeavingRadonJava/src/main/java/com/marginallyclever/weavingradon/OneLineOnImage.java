package com.marginallyclever.weavingradon;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.locks.ReentrantLock;

public class OneLineOnImage extends JPanel implements RayIllustrator {
    private BufferedImage image;
    private int showTheta=-1;
    private int showR=-1;
    private RadonThreader radonThreader;
    private RadonPanel singleRadon;
    ReentrantLock lock = new ReentrantLock();

    public OneLineOnImage() {
        super();
    }

    public void setImage(BufferedImage srcImage) {
        removeAll();

        int w = 0, h = 0;
        if (srcImage != null) {
            w = srcImage.getWidth();
            h = srcImage.getHeight();
        }
        if(w==0||h==0) return;
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        add(new JLabel(new ImageIcon(image)), BorderLayout.CENTER);
        repaint();
        var radon = radonThreader.createRadonTransform(image);
        singleRadon.setImage(radon);
    }

    public void setRadon(RadonThreader radonThreader, RadonPanel singleRadon) {
        this.radonThreader = radonThreader;
        this.singleRadon = singleRadon;
    }

    @Override
    public void highlightLine(int angle, int r) {
        this.showTheta = angle;
        this.showR = r;

        if(image==null) return;

        if (lock.isLocked()) return;
        lock.lock();
        try {
            updateLine();
        } finally {
            lock.unlock();
        }
    }

    private void updateLine() {
        // show the line theta/r, where theta is the angle and r is the distance from the center.
        if(image == null) return;

        var g = image.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0,0,image.getWidth(),image.getHeight());

        if(showTheta>=0 && showTheta<180) {
            //System.out.println("showTheta="+showTheta+" showR="+showR);
            double radius = Math.min(image.getWidth(), image.getHeight()) / 2.0;
            double r = showR - radius;
            double theta = Math.toRadians(showTheta);
            var w2 = image.getWidth() / 2;
            var h2 = image.getHeight() / 2;

            double s = Math.sin(theta);
            double c = Math.cos(theta);
            double d = Math.sqrt(w2*w2 - r*r);
            int x0 = (int)(w2 + r * c - d * s);
            int y0 = (int)(h2 + r * s + d * c);
            int x1 = (int)(w2 + r * c + d * s);
            int y1 = (int)(h2 + r * s - d * c);

            g.setColor(Color.WHITE);
            g.drawLine(x0, y0, x1, y1);
        }
        repaint();
        if(radonThreader!=null && singleRadon !=null) {
            System.out.println("c");
            var radon = radonThreader.createRadonTransform(image);
            singleRadon.setImage(radon);
        }
    }
}
