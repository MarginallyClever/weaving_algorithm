package com.marginallyclever.weavingradon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * RadonPanel listens for mouse events.  when the cursor is over the image it will tell the Viewport
 * to display the theta and r values at that point.
 */
public class RadonPanel extends JPanel {
    private BufferedImage image;
    private final RayIllustrator rayIllustrator;
    private int showR,showTheta;
    private boolean showClickPoint = true;
    private JToolBar toolbar = new JToolBar();

    public RadonPanel(RayIllustrator rayIllustrator) {
        super(new BorderLayout());
        this.rayIllustrator = rayIllustrator;

        buildToolbar();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                updateThetaR(e);
                repaint();
            }
        });
    }

    private void buildToolbar() {
        toolbar.setFloatable(false);

        JToggleButton toggleClick = new JToggleButton("Point");
        toggleClick.setSelected(showClickPoint);
        toggleClick.addActionListener(e -> {
            showClickPoint=!showClickPoint;
            repaint();
        });
        toolbar.add(toggleClick);

        add(toolbar, BorderLayout.NORTH);
    }

    public void updateThetaR(MouseEvent e) {
        if(image==null) return;
        Dimension d = toolbar.getPreferredSize();
        showTheta = e.getX();
        showR = e.getY()-d.height;
        if(showTheta<0 || showR<0 || showTheta>=image.getWidth() || showR>=image.getHeight()) return;

        //System.out.println(theta+","+r);
        rayIllustrator.highlightLine(showTheta,showR);
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(image==null) return;

        Dimension d = toolbar.getPreferredSize();
        g.translate(0,d.height);
        g.drawImage(image,0,0,image.getWidth(),image.getHeight(),this);
        if(showClickPoint) {
            g.setColor(Color.RED);
            g.fillOval(showTheta - 2, showR - 2, 4, 4);
            g.setColor(Color.GREEN);
            g.drawOval(showTheta - 2, showR - 2, 4, 4);
        }
        g.translate(0,-d.height);
    }
}
