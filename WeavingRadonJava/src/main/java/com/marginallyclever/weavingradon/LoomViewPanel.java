package com.marginallyclever.weavingradon;

import javax.swing.*;
import javax.vecmath.Vector2d;
import java.awt.*;
import java.awt.image.BufferedImage;

public class LoomViewPanel extends JPanel implements RayIllustrator {
    private Loom loom;
    public static final int NAIL_RADIUS = 3;
    public static final int TOOLBAR_HEIGHT = 30;

    private RadonThreader radonThreader;
    private RadonPanel radonPanel;
    private final JToolBar toolbar = new JToolBar();

    private BufferedImage image;
    private boolean showImage = true;
    private boolean showNails = true;
    private boolean showThread = true;
    private int showTheta=-1;
    private int showR=-1;

    JToggleButton togglePlay = new JToggleButton("Play");

    public LoomViewPanel() {
        super(new BorderLayout());
        setName("Loom");

        // Create a Timer that fires every 100 milliseconds
        Timer timer = new Timer(50, (e) -> {
            makeStep();
            if(loom == null || loom.shouldStop()) {
                togglePlay.setSelected(false);
            }
            invalidate();
        });


        toolbar.setFloatable(false);

        JToggleButton toggleImage = new JToggleButton("Image");
        toggleImage.setSelected(showImage);
        toggleImage.addActionListener(e -> {
            showImage=!showImage;
            setImage(image);
        });
        toolbar.add(toggleImage);

        JToggleButton toggleNails = new JToggleButton("Nails");
        toggleNails.setSelected(showNails);
        toggleNails.addActionListener(e -> {
            showNails=!showNails;
            repaint();
        });
        toolbar.add(toggleNails);

        JToggleButton toggleThread = new JToggleButton("Thread");
        toggleThread.setSelected(showThread);
        toggleThread.addActionListener(e -> {
            showThread=!showThread;
            repaint();
        });
        toolbar.add(toggleThread);

        JButton nextBest = new JButton("Next Best Thread");
        nextBest.addActionListener(e -> {
            ThetaR bestFound = radonThreader.getNextBestThetaR();
            showTheta = bestFound.theta;
            showR = bestFound.r;
            repaint();
        });
        toolbar.add(nextBest);

        JButton step = new JButton("Step");
        step.addActionListener(e -> makeStep());
        toolbar.add(step);

        togglePlay.setSelected(false);
        togglePlay.addActionListener(e -> {
            togglePlay.setText(togglePlay.isSelected() ? "Stop" : "Play");
            if(togglePlay.isSelected()) {
                // if play is becoming active, start a recurring timer that adds the next best thread.
                timer.start();
            } else {
                // if play is becoming inactive, stop the timer.
                timer.stop();
            }
        });
        toolbar.add(togglePlay);


        add(toolbar, BorderLayout.NORTH);
    }

    public void makeStep() {
        radonThreader.addNextBestThread();
        radonPanel.setRadonTransform(radonThreader.getRadonTransform());
        repaint();
    }

    public void setImage(BufferedImage image) {
        this.image = image;

        repaint();
    }

    @Override
    public void highlightLine(int theta, int r) {
        this.showTheta = theta;
        this.showR = r;
        repaint();
    }

    @Override
    public void setLoomAndImage(Loom loom, BufferedImage grey) {
        setLoom(loom);
        setImage(grey);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension dim = toolbar.getPreferredSize();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2.translate(0, dim.height);

        if (showImage && image != null) {
            // Draw the image at (0, 0) with the size of the panel
            g2.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), this);
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        if(showNails && loom !=null) {
            int r = NAIL_RADIUS/2;
            g2.translate(-r,-r);
            // fill the ovals
            g2.setColor(Color.RED);
            for(Vector2d nail : loom.nails) {
                g2.fillOval((int)nail.x, (int)nail.y, NAIL_RADIUS, NAIL_RADIUS);
            }
            // draw the borders
            g2.setColor(Color.WHITE);
            for(Vector2d nail : loom.nails) {
                g2.drawOval((int)nail.x, (int)nail.y, NAIL_RADIUS, NAIL_RADIUS);
            }
            g2.translate(r,r);
        }

        if(showThread && loom !=null) {
            for(ThreadColor tc : loom.selectedThreads) {
                g2.setColor(tc.col);
                g2.drawLine((int)tc.start.x,
                            (int)tc.start.y,
                            (int)tc.end.x,
                            (int)tc.end.y);
            }
        }

        // show the line theta/r, where theta is the angle and r is the distance from the center.
        if(image != null && showTheta>=0 && showTheta<180) {
            double theta = Math.toRadians(showTheta);
            g2.setColor(Color.GREEN);
            var w2 = image.getWidth()/2;
            var h2 = image.getHeight()/2;

            double r = showR - loom.radius;
            double s = Math.sin(theta);
            double c = Math.cos(theta);
            double d = Math.sqrt(w2*w2 - r*r);
            int x0 = (int)(w2 + r * c - d * s);
            int y0 = (int)(h2 + r * s + d * c);
            int x1 = (int)(w2 + r * c + d * s);
            int y1 = (int)(h2 + r * s - d * c);

            g2.drawLine(x0,y0,x1,y1);
        }

        g2.translate(0,-dim.height);
    }

    @Override
    public void setRadon(RadonThreader radonThreader, RadonPanel radonPanel) {
        this.radonThreader = radonThreader;
        this.radonPanel = radonPanel;
    }

    public void setLoom(Loom loom) {
        this.loom = loom;
    }
}
