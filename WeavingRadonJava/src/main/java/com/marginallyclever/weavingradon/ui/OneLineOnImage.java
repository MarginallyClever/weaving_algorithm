package com.marginallyclever.weavingradon.ui;

import com.marginallyclever.weavingradon.core.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Displays a single {@link LoomThread} on a {@link BufferedImage}.  Used for debugging the {@link MonochromaticThreader}.
 */
public class OneLineOnImage extends JPanel implements RayIllustrator {
    private BufferedImage image;
    private final ThetaR bestFound = new ThetaR(0,0,0);
    private boolean showBest=false;
    private RadonThreader radonThreader;
    private RadonPanel singleRadon;
    private final ReentrantLock lock = new ReentrantLock();
    private Component oldLabel;
    private Loom loom;

    public OneLineOnImage() {
        super(new BorderLayout());

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        add(toolbar, BorderLayout.NORTH);

        JButton allThreads = new JButton("All Threads");
        allThreads.addActionListener(e -> {
            if(radonThreader!=null && singleRadon !=null) {
                drawAllThreads();
            }
        });
        toolbar.add(allThreads);
    }

    public void setLoom(Loom loom) {
        this.loom = loom;
    }

    private void drawAllThreads() {
        var g = image.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0,0,image.getWidth(),image.getHeight());

        for(LoomThread t : loom.selectedThreads) {
            t.display(g);
        }
        for(LoomThread t : loom.allThreads) {
            t.display(g);
        }
        singleRadon.setRadonTransform(new RadonTransform(image));
    }

    public void setImage(BufferedImage srcImage) {
        int w = 0, h = 0;
        if (srcImage != null) {
            w = srcImage.getWidth();
            h = srcImage.getHeight();
        }
        if(w==0||h==0) return;
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        if(oldLabel!=null) remove(oldLabel);
        add(oldLabel = new JLabel(new ImageIcon(image)), BorderLayout.CENTER);
        repaint();
        singleRadon.setRadonTransform(new RadonTransform(image));
    }

    @Override
    public void setRadon(RadonThreader radonThreader, RadonPanel singleRadon) {
        this.radonThreader = radonThreader;
        this.singleRadon = singleRadon;
    }

    @Override
    public void highlightLine(ThetaR tr) {
        showBest = true;
        bestFound.set(tr);
        if(image==null) return;
        if (lock.isLocked()) return;
        lock.lock();
        try {
            updateLine();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void hideLine() {
        showBest=false;
        repaint();
    }

    private void updateLine() {
        // show the line theta/r, where theta is the angle and r is the distance from the center.
        if(image == null) return;

        Graphics2D g2 = image.createGraphics();
        RenderHintHelper.setRenderHints(g2);

        if(showBest) {
            //System.out.println("showTheta="+showTheta+" showR="+showR);
            LoomThread th = loom.findThreadClosestToThetaR(bestFound);
            g2.setColor(Color.BLACK);
            g2.fillRect(0,0,image.getWidth(),image.getHeight());
            g2.setColor(th.col);
            g2.translate(loom.radius,loom.radius);
            g2.drawLine((int)th.start.x, (int)th.start.y, (int)th.end.x, (int)th.end.y);
            g2.translate(-loom.radius,-loom.radius);

            if(singleRadon != null) {
                singleRadon.setRadonTransform(new RadonTransform(loom.radius,th));
            }
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0,0,image.getWidth(),image.getHeight());

            if(singleRadon != null) {
                singleRadon.setRadonTransform(new RadonTransform(image));
            }
        }
        g2.dispose();
        repaint();
    }

    @Override
    public void setLoomAndImage(Loom loom, BufferedImage grey) {
        setLoom(loom);
        setImage(grey);
    }
}
