package com.marginallyclever.weavingradon.ui;

import com.marginallyclever.weavingradon.core.*;

import javax.swing.*;
import javax.vecmath.Vector2d;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class LoomPanel extends JPanel implements RayIllustrator {
    private Loom loom;
    public static final int NAIL_RADIUS = 3;

    private RadonThreader radonThreader;
    private RadonPanel radonPanel;
    private final JToolBar toolbar = new JToolBar();

    private BufferedImage image;
    private boolean showImage = true;
    private boolean showNails = true;
    private boolean showThread = true;
    private final ThetaR bestFound = new ThetaR(0,0,0);
    private boolean showBest=false;
    private Color backgroundColor = Color.BLACK;

    private JToggleButton togglePlay = null;

    public LoomPanel() {
        super(new BorderLayout());
        setName("Loom");

        // Create a Timer that fires every 100 milliseconds
        Timer timer = new Timer(50, e -> {
            makeStep();
            if(loom == null || loom.shouldStop() && togglePlay!=null) {
                togglePlay.setSelected(false);
            }
            invalidate();
        });


        toolbar.setFloatable(false);

        addToggle(toolbar,"Image", e->{
            showImage = !showImage;
            setImage(image);
        });
        addToggle(toolbar,"Nails", e->{
            showNails = !showNails;
            repaint();
        });
        addToggle(toolbar,"Thread", e->{
            showThread = !showThread;
            repaint();
        });

        JButton nextBest = new JButton("Next Best Thread");
        nextBest.addActionListener(e -> {
            bestFound.set(radonThreader.getBestThetaR());
            showBest = true;
            repaint();
        });
        toolbar.add(nextBest);

        JButton step = new JButton("Step");
        step.addActionListener(e -> makeStep());
        toolbar.add(step);

        togglePlay = addToggle(toolbar,"Play", e->{
            togglePlay.setText(togglePlay.isSelected() ? "Stop" : "Play");
            if(togglePlay.isSelected()) {
                // if play is becoming active, start a recurring timer that adds the next best thread.
                timer.start();
            } else {
                // if play is becoming inactive, stop the timer.
                timer.stop();
            }
        });

        add(toolbar, BorderLayout.NORTH);
    }

    private JToggleButton addToggle(JToolBar toolbar, String label, ActionListener action) {
        JToggleButton toggle = new JToggleButton(label);
        toggle.addActionListener(action);
        toolbar.add(toggle);
        return toggle;
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
    public void highlightLine(ThetaR tr) {
        showBest=true;
        bestFound.set(tr);
        repaint();
    }

    @Override
    public void hideLine() {
        showBest=false;
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
        RenderHintHelper.setRenderHints(g2);

        g2.translate(0, dim.height);

        if (image != null) {
            if(showImage) {
                // Draw the image at (0, 0) with the size of the panel
                g2.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), this);
            } else {
                g2.setColor(backgroundColor);
                g2.fillRect(0, 0, image.getWidth(), image.getHeight());
            }
        } else {
            g2.clearRect(0, 0, getWidth(), getHeight());
        }

        if(loom!=null) {
            g2.translate(loom.radius, loom.radius);

            if (showNails) {
                int r = NAIL_RADIUS / 2;
                g2.translate(-r, -r);
                // fill the ovals
                g2.setColor(Color.RED);
                for (Vector2d nail : loom.nails) {
                    g2.fillOval((int) nail.x, (int) nail.y, NAIL_RADIUS, NAIL_RADIUS);
                }
                // draw the borders
                g2.setColor(Color.WHITE);
                for (Vector2d nail : loom.nails) {
                    g2.drawOval((int) nail.x, (int) nail.y, NAIL_RADIUS, NAIL_RADIUS);
                }
                g2.translate(r, r);
            }

            if (showThread) {
                for (LoomThread tc : loom.selectedThreads) {
                    g2.setColor(tc.col);
                    g2.drawLine((int) tc.start.x,
                            (int) tc.start.y,
                            (int) tc.end.x,
                            (int) tc.end.y);
                }
            }
            g2.translate(-loom.radius, -loom.radius);
        }

        // show the line theta/r, where theta is the angle and r is the distance from the center.
        if(image != null && showBest) {
            g2.setColor(Color.GREEN);
            bestFound.display(g2,image.getWidth()/2);
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
