package com.marginallyclever.weavingradon.ui;

import com.marginallyclever.weavingradon.core.*;

import javax.swing.*;
import javax.vecmath.Vector2d;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ListIterator;

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
    private JSlider slider = null;

    private JToggleButton togglePlay = null;

    public LoomPanel() {
        super(new BorderLayout());
        setName("Loom");

        // Create a Timer that fires every 100 milliseconds
        Timer timer = new Timer(50, e -> {
            boolean keepGoing = makeStep();
            if(!keepGoing || loom == null || loom.shouldStop() && togglePlay!=null) {
                System.out.println("Stopping.");
                togglePlay.setSelected(false);
            }
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

        JButton export = new JButton("Export");
        export.addActionListener(this::export);
        toolbar.add(export);

        slider = new JSlider(0, 0, 0);
        slider.addChangeListener(e -> {
            repaint();
        });
        toolbar.add(slider);

        add(toolbar, BorderLayout.NORTH);
    }

    private void export(ActionEvent actionEvent) {
        JFileChooser fileChooser = getJFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filename = fileChooser.getSelectedFile().getAbsolutePath();
            if (!filename.toLowerCase().endsWith(".txt")) {
                filename += ".txt";
            }
            try {
                writeExport(new File(filename));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static JFileChooser getJFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Loom Sequence");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".txt") || f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "TXT files";
            }
        });
        return fileChooser;
    }

    private void writeExport(File file) throws IOException {
        try(BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(file))) {
            writer.write("color,start,end");
            writer.newLine();

            ListIterator<LoomThread> iterator = loom.selectedThreads.listIterator(slider.getValue());
            while (iterator.hasPrevious()) {
                LoomThread tc = iterator.previous();
                writer.write(interpret(tc));
                writer.newLine();
            }
        }
    }

    private String interpret(LoomThread lt) {
        String colorName = "0x"+Integer.toHexString(lt.col.getRGB());
        return colorName+","+loom.getNailIndex(lt.start)+","+loom.getNailIndex(lt.end);
    }

    private JToggleButton addToggle(JToolBar toolbar, String label, ActionListener action) {
        JToggleButton toggle = new JToggleButton(label);
        toggle.addActionListener(action);
        toolbar.add(toggle);
        return toggle;
    }

    /**
     * @return true if a thread was added, false if no thread was added.
     */
    public boolean makeStep() {
        if(!radonThreader.addNextBestThread()) return false;
        radonPanel.setRadonTransform(radonThreader.getRadonTransform());
        if(showThread) repaint();
        showBest=false;
        slider.setMaximum(loom.selectedThreads.size());
        slider.setValue(loom.selectedThreads.size());
        return true;
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
        //super.paintComponent(g);
        Dimension dim = toolbar.getPreferredSize();

        Graphics2D g2 = (Graphics2D) g.create();
        RenderHintHelper.setRenderHints(g2);
        g2.setStroke(new BasicStroke(1f));

        g2.translate(0, dim.height);

        if(image==null) {
            // all black
            g2.clearRect(0, 0, getWidth(), getHeight());
        } else if(showImage) {
            // Draw the image at (0, 0) with the size of the panel
            g2.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), this);
        } else {
            g2.setColor(backgroundColor);
            g2.fillRect(0, 0, image.getWidth(), image.getHeight());
        }

        if(loom!=null) {
            g2.translate(loom.radius, loom.radius);
            if (showNails) drawNails(g2);
            if (showThread) drawAllTheads(g2);
            g2.translate(-loom.radius, -loom.radius);
        }

        // show the line theta/r, where theta is the angle and r is the distance from the center.
        if(image != null && showBest) {
            g2.setColor(Color.GREEN);
            bestFound.display(g2,image.getWidth()/2);
        }

        g2.translate(0,-dim.height);
    }

    private void drawAllTheads(Graphics2D g2) {
        // draw in reverse order so the most important thread (first in list) is on the top of the stack.
        ListIterator<LoomThread> iterator = loom.selectedThreads.listIterator(slider.getValue());
        while (iterator.hasPrevious()) {
            LoomThread tc = iterator.previous();
            drawOneThread(g2, tc);
        }
    }

    private void drawOneThread(Graphics2D g2, LoomThread thread) {
        g2.setColor(thread.col);
        g2.drawLine((int)thread.start.x,
                    (int)thread.start.y,
                    (int)thread.end.x,
                    (int)thread.end.y);
    }

    private void drawNails(Graphics2D g2) {
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

    @Override
    public void setRadon(RadonThreader radonThreader, RadonPanel radonPanel) {
        this.radonThreader = radonThreader;
        this.radonPanel = radonPanel;
    }

    public void setLoom(Loom loom) {
        this.loom = loom;
    }
}
