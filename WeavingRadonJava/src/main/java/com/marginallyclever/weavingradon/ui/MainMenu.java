package com.marginallyclever.weavingradon.ui;

import ModernDocking.app.DockableMenuItem;
import com.marginallyclever.weavingradon.WeavingApp;

import javax.swing.*;

public class MainMenu extends JMenuBar {
    private final WeavingApp parent;
    private final RecentFilesMenu recentFilesMenu = new RecentFilesMenu("Recent Files");

    public MainMenu(WeavingApp parent) {
        super();
        this.parent = parent;

        addFileMenu();
        addWindowsMenu();
        addAboutMenu();
    }

    private void addAboutMenu() {
        JMenu helpMenu = new JMenu("Help");
        add(helpMenu);

        JMenuItem aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.addActionListener(e->{
            JOptionPane.showMessageDialog(SwingUtilities.getRootPane(this),
                    "Weaving Radon\n" +
                            "Version: " + getVersion() + "\n");
        });
        helpMenu.add(aboutMenuItem);
    }

    private String getVersion() {
        // get version from the manifest
        String VERSION = "unknown";
        try {
            VERSION = getClass().getPackage().getImplementationVersion();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return VERSION;
    }

    private void addWindowsMenu() {
        JMenu viewMenu = new JMenu("Windows");
        parent.getWindows().forEach(w -> {
            DockableMenuItem item = new DockableMenuItem(w.getPersistentID(),w.getTabText());
            viewMenu.add(item);
        });
        add(viewMenu);
    }

    private void addFileMenu() {
        JMenu fileMenu = new JMenu("File");
        add(fileMenu);

        JMenuItem openMenuItem = new JMenuItem("Open");
        openMenuItem.addActionListener(e->{
            boolean result = parent.selectFile();
            if(result) openImageFromFilename(parent.getLastPath());
        });
        fileMenu.add(openMenuItem);

        recentFilesMenu.addSubmenuListener(e-> openImageFromFilename( ((JMenuItem) e.getSource()).getText() ));

        fileMenu.add(recentFilesMenu);
    }

    private void openImageFromFilename(String filename) {
        if(parent.loadImage(filename)) {
            recentFilesMenu.addFilename(filename);
        } else {
            recentFilesMenu.removeFilename(filename);
        }
    }
}
