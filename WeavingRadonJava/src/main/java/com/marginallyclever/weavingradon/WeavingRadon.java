package com.marginallyclever.weavingradon;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import ModernDocking.DockingRegion;
import ModernDocking.app.Docking;
import ModernDocking.app.RootDockingPanel;
import ModernDocking.ext.ui.DockingUI;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

public class WeavingRadon {
    public static final int WIDTH = 800;
    public static final int HEIGHT = 800;
    public static final int TITLEBAR_HEIGHT = 30;
    public static final int DOCKING_TAB_HEIGHT = 30;

    private final JFrame frame;
    private final ArrayList<DockingPanel> windows = new ArrayList<>();
    private final JFileChooser fileChooser;

    private final ResultsPanel resultsPanel;
    private final RadonPanel radonPanel;
    public static final RadonThreader radonThreaderA = new RadonThreader();

    private final OneLineOnImage singleLine;
    private final RadonPanel singleRadon;
    public static final RadonThreader radonThreaderB = new RadonThreader();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeavingRadon::new);
    }

    public WeavingRadon() {
        setLookAndFeel();  // must come before creating the frames and panels.

        frame = new JFrame("Radon Weaving");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WIDTH, HEIGHT + TITLEBAR_HEIGHT + DOCKING_TAB_HEIGHT + ResultsPanel.TOOLBAR_HEIGHT);
        //frame.setLocationByPlatform(true);
        frame.setLocationRelativeTo(null);

        // create panels
        resultsPanel = new ResultsPanel();
        radonPanel = new RadonPanel(resultsPanel);
        resultsPanel.setRadon(radonThreaderA, radonPanel);

        singleLine = new OneLineOnImage();
        singleRadon = new RadonPanel(singleLine);
        singleLine.setRadon(radonThreaderB,singleRadon);

        // setup the docking system and dock the panels.
        initDocking();
        createDefaultLayout();
        resetDefaultLayout();
        frame.setJMenuBar(new MainMenu(this));

        // create a file chooser for images
        String name = String.join(", ", ImageIO.getReaderFileSuffixes());
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
        DockingPanel resultsView = new DockingPanel("8e50154c-a149-4e95-9db5-4611d24cc0cc", "View");
        resultsView.add(resultsPanel, BorderLayout.CENTER);
        windows.add(resultsView);

        DockingPanel radonView = new DockingPanel("f2308391-8388-4f90-89f0-61caca03eb18", "View R");
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
        Docking.dock(windows.get(0), frame);
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
                BufferedImage image = ImageIO.read(new File(path));
                // make square
                int d = Math.min(resultsPanel.getWidth(), resultsPanel.getHeight());
                int s = Math.min(image.getWidth(),image.getHeight());
                BufferedImage square = new BufferedImage(d,d,BufferedImage.TYPE_INT_ARGB);
                square.getGraphics().drawImage(image,0,0,d,d,0,0,s,s,null);

                radonThreaderA.setImage(square);
                radonThreaderA.maskCurrentRadonByRemainingThreads();
                resultsPanel.setImage(square);
                radonPanel.setImage(radonThreaderA.getCurrentRadonImage());

                radonThreaderB.setImage(square);
                singleLine.setImage(square);
                singleRadon.setImage(radonThreaderB.getCurrentRadonImage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<DockingPanel> getWindows() {
        return windows;
    }
}