package com.marginallyclever.weavingradon.ui;

import com.marginallyclever.weavingradon.core.RadonTransform;
import com.marginallyclever.weavingradon.core.RayIllustrator;
import com.marginallyclever.weavingradon.core.ThetaR;

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
    private final ThetaR selectedThetaR = new ThetaR(0,0,0);
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
                if(radonTransform ==null) return;
                setToolTipText("Theta: "+getTheta(e)+", R: "+getR(e));
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                if(radonTransform ==null) return;
                setToolTipText("Theta: "+getTheta(e)+", R: "+getR(e));
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
                rayIllustrator.hideLine();
            } else {
                rayIllustrator.highlightLine(selectedThetaR);
            }
            repaint();
        });
        toolbar.add(toggleClick);

        add(toolbar, BorderLayout.NORTH);
    }

    public void updateThetaR(MouseEvent e) {
        if(radonTransform ==null) return;
        selectedThetaR.theta = getTheta(e);
        selectedThetaR.r = getR(e);

        int radius = radonTransform.getHeight()/2;
        if(selectedThetaR.theta < 0 || selectedThetaR.theta >= radonTransform.getWidth()
            || selectedThetaR.r < -radius || selectedThetaR.r >= radius ) return;
        //System.out.println(theta+","+r);
        rayIllustrator.highlightLine(selectedThetaR);
    }

    private int getTheta(MouseEvent e) {
        return e.getX();
    }

    private int getR(MouseEvent e) {
        Dimension d = toolbar.getPreferredSize();
        int radius = radonTransform.getHeight()/2;
        return (e.getY() - d.height) - radius;
    }

    public void setRadonTransform(RadonTransform image) {
        this.radonTransform = image;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(radonTransform ==null) return;
        Image graph = radonTransform.getHeatMap();
        if(graph==null) return;

        Dimension d = toolbar.getPreferredSize();
        g.translate(0,d.height);

        g.drawImage(radonTransform.getHeatMap(),0,0, radonTransform.getWidth(), radonTransform.getHeight(),this);
        if(showClickPoint) {
            int radius = radonTransform.getHeight()/2;
            g.setColor(Color.RED);
            g.fillOval((int)selectedThetaR.theta - 2, selectedThetaR.r-radius - 2, 4, 4);
            g.setColor(Color.GREEN);
            g.drawOval((int)selectedThetaR.theta - 2, selectedThetaR.r-radius - 2, 4, 4);
        }
        g.translate(0,-d.height);
    }
}
