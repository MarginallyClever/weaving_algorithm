package com.marginallyclever.weavingradon.ui;

import com.marginallyclever.weavingradon.core.RadonTransform;
import com.marginallyclever.weavingradon.core.RayIllustrator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * RadonPanel listens for mouse events.  when the cursor is over the image it will tell the Viewport
 * to display the theta and r values at that point.
 */
public class RadonPanel extends JPanel {
    private RadonTransform radonTransform;
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

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
                setToolTipText("Theta: "+e.getX()+", R: "+(e.getY()-toolbar.getPreferredSize().height));
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                setToolTipText("Theta: "+e.getX()+", R: "+(e.getY()-toolbar.getPreferredSize().height));
            }
        });
    }

    private void buildToolbar() {
        toolbar.setFloatable(false);

        JToggleButton toggleClick = new JToggleButton("Point");
        toggleClick.setSelected(showClickPoint);
        toggleClick.addActionListener(e -> {
            showClickPoint=!showClickPoint;
            if(!showClickPoint) {
                rayIllustrator.highlightLine(-1,-1);
            } else {
                rayIllustrator.highlightLine(showTheta,showR);
            }
            repaint();
        });
        toolbar.add(toggleClick);

        add(toolbar, BorderLayout.NORTH);
    }

    public void updateThetaR(MouseEvent e) {
        if(radonTransform ==null) return;
        Dimension d = toolbar.getPreferredSize();
        showTheta = e.getX();
        showR = e.getY()-d.height;
        if(showTheta<0 || showR<0 || showTheta>= radonTransform.getWidth() || showR>= radonTransform.getHeight()) return;

        //System.out.println(theta+","+r);
        rayIllustrator.highlightLine(showTheta,showR);
    }

    public void setRadonTransform(RadonTransform image) {
        this.radonTransform = image;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(radonTransform ==null) return;
        Image graph = radonTransform.getGraph();
        if(graph==null) return;

        Dimension d = toolbar.getPreferredSize();
        g.translate(0,d.height);

        g.drawImage(radonTransform.getGraph(),0,0, radonTransform.getWidth(), radonTransform.getHeight(),this);
        if(showClickPoint) {
            g.setColor(Color.RED);
            g.fillOval(showTheta - 2, showR - 2, 4, 4);
            g.setColor(Color.GREEN);
            g.drawOval(showTheta - 2, showR - 2, 4, 4);
        }
        g.translate(0,-d.height);
    }
}
