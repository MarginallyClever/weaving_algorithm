package com.marginallyclever.weavingradon;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import ModernDocking.app.Docking;
import ModernDocking.app.RootDockingPanel;
import ModernDocking.ext.ui.DockingUI;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.marginallyclever.weavingradon.core.GroupRadonThreader;
import com.marginallyclever.weavingradon.core.Loom;
import com.marginallyclever.weavingradon.core.SingleThreader;
import com.marginallyclever.weavingradon.ui.*;

public class WeavingApp {
    public static final int DIAMETER = 800;
    public static final int TITLEBAR_HEIGHT = 30;
    public static final int DOCKING_TAB_HEIGHT = 30;
    public static final int ALPHA = 127;
    public static final int TOOLBAR_HEIGHT = 30;

    private final JFrame frame;
    private final ArrayList<DockingPanel> windows = new ArrayList<>();
    private final JFileChooser fileChooser;

    private final Loom loom = new Loom(DIAMETER/2, 100);
    private final LoomPanel loomPanel;
    private final RadonPanel radonPanel;

    //public final RadonThreader myThreader = new SingleThreader(Color.WHITE);
    public final GroupRadonThreader myThreader = new GroupRadonThreader();
    public final SingleThreader radonThreaderC = new SingleThreader(new Color(  0,255,255, ALPHA));
    public final SingleThreader radonThreaderM = new SingleThreader(new Color(255,  0,255, ALPHA));
    public final SingleThreader radonThreaderY = new SingleThreader(new Color(255,255,  0, ALPHA));
    public final SingleThreader radonThreaderK = new SingleThreader(new Color(  0,  0,  0, ALPHA));
    public final SingleThreader radonThreaderW = new SingleThreader(new Color(255,255,255, ALPHA));

    private final OneLineOnImage singleLine;
    private final RadonPanel singleRadon;
    public final SingleThreader radonThreaderB = new SingleThreader(new Color(255,255,255));

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeavingApp::new);
    }

    public WeavingApp() {
        setLookAndFeel();  // must come before creating the frames and panels.

        frame = new JFrame("Radon Weaving");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(DIAMETER, DIAMETER + TITLEBAR_HEIGHT + DOCKING_TAB_HEIGHT + TOOLBAR_HEIGHT);
        //frame.setLocationByPlatform(true);
        frame.setLocationRelativeTo(null);

        // create panels
        loomPanel = new LoomPanel();
        radonPanel = new RadonPanel(loomPanel);
        loomPanel.setRadon(myThreader, radonPanel);

        singleLine = new OneLineOnImage();
        singleRadon = new RadonPanel(singleLine);
        singleLine.setRadon(radonThreaderB,singleRadon);

        myThreader.addThreader(radonThreaderC);
        myThreader.addThreader(radonThreaderM);
        myThreader.addThreader(radonThreaderY);
        myThreader.addThreader(radonThreaderK);
        myThreader.addThreader(radonThreaderW);

        // setup the docking system and dock the panels.
        initDocking();
        createDefaultLayout();
        resetDefaultLayout();
        frame.setJMenuBar(new MainMenu(this));

        // create a file chooser for images
        String [] list = ImageIO.getReaderFileSuffixes();
        Arrays.sort(list);
        String name = String.join(", ", list);
        name = "Image files (" + name + ")";
        fileChooser = new PersistentJFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(name, ImageIO.getReaderFileSuffixes()));

        // show the window
        frame.setVisible(true);
    }

    private void setLookAndFeel() {
        System.out.println("Setting look and feel...");
        FlatLaf.registerCustomDefaultsSource("com.marginallyclever.weavingradon");
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            // option 2: UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ignored) {
            System.out.println("failed to set flat look and feel. falling back to default native look and feel");
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                System.out.println("failed to set native look and feel.");
            }
        }
    }

    private void initDocking() {
        Docking.initialize(frame);
        DockingUI.initialize();
        ModernDocking.settings.Settings.setAlwaysDisplayTabMode(true);
        ModernDocking.settings.Settings.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        // create root panel
        RootDockingPanel root = new RootDockingPanel(frame);
        frame.add(root, BorderLayout.CENTER);
    }

    /**
     * Persistent IDs were generated using <code>UUID.randomUUID().toString()</code>
     * or <a href="https://www.uuidgenerator.net/">one of many websites</a>.
     */
    private void createDefaultLayout() {
        DockingPanel resultsView = new DockingPanel("8e50154c-a149-4e95-9db5-4611d24cc0cc", "Loom");
        resultsView.add(loomPanel, BorderLayout.CENTER);
        windows.add(resultsView);

        DockingPanel radonView = new DockingPanel("f2308391-8388-4f90-89f0-61caca03eb18", "Radon");
        radonView.add(radonPanel, BorderLayout.CENTER);
        windows.add(radonView);

        DockingPanel oneView = new DockingPanel("e675ab55-fea9-49a5-a04d-51daa9cd31e6", "One line");
        oneView.add(singleLine, BorderLayout.CENTER);
        windows.add(oneView);

        DockingPanel oneRadon = new DockingPanel("eb1ea92b-fc66-4c54-a5a9-3e4f8203bd5d", "Line R");
        oneRadon.add(singleRadon, BorderLayout.CENTER);
        windows.add(oneRadon);
    }

    private void resetDefaultLayout() {
        for (DockingPanel w : windows) {
            Docking.undock(w);
        }
        Docking.dock(windows.getFirst(), frame);
        //Docking.dock(windows.get(0), windows.get(2), DockingRegion.CENTER);
        //Docking.dock(windows.get(3), frame, DockingRegion.EAST);
        //Docking.dock(windows.get(1), windows.get(3), DockingRegion.CENTER);
    }

    public void openFile(ActionEvent actionEvent) {
        // show the file chooser dialog
        if(fileChooser.showOpenDialog(frame)== JFileChooser. APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getAbsolutePath();
            System.out.println("Open file: "+path);
            try {
                BufferedImage square = makeSquare(ImageIO.read(new File(path)));
                // build the nails and threads
                loom.reset();
                // build a radon transform for every thread, based on the color filter for that threader.
                myThreader.setLoomAndImage(loom,square);
                myThreader.maskCurrentRadonByAllThreads();

                loomPanel.setLoomAndImage(loom,square);
                radonPanel.setRadonTransform(myThreader.getRadonTransform());

                BufferedImage grey = makeGreyscale(square);
                radonThreaderB.setLoomAndImage(loom,grey);
                singleLine.setLoomAndImage(loom,grey);
                singleRadon.setRadonTransform(radonThreaderB.getRadonTransform());
            } catch (Exception e) {
                System.out.println("Failed to load file.");
                e.printStackTrace();
            }
        }
    }

    // make square greyscale
    private BufferedImage makeGreyscale(BufferedImage square) {
        BufferedImage grey = new BufferedImage(square.getWidth(),square.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<square.getHeight();++y) {
            for(int x=0;x<square.getWidth();++x) {
                int rgb = square.getRGB(x,y);
                Color c = new Color(rgb);
                int avg = (int)( (c.getRed()+c.getGreen()+c.getBlue()) / 3.0 );
                grey.setRGB(x,y,new Color(avg,avg,avg).getRGB());
            }
        }
        return grey;
    }

    /**
     * Crop an image to make it square.  cut either the bottom or the right side.
     * @param image the image to crop
     * @return a square image
     */
    private BufferedImage makeSquare(BufferedImage image) {
        int s = Math.min(image.getWidth(),image.getHeight());

        BufferedImage square = new BufferedImage(DIAMETER,DIAMETER,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = square.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2.drawImage(image,0,0,DIAMETER,DIAMETER,0,0,s,s,null);
        g2.dispose();
        return square;
    }

    public ArrayList<DockingPanel> getWindows() {
        return windows;
    }
}