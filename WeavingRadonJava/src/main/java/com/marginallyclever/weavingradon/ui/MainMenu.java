package com.marginallyclever.weavingradon.ui;

import ModernDocking.app.DockableMenuItem;
import com.marginallyclever.weavingradon.WeavingApp;

import javax.swing.*;

public class MainMenu extends JMenuBar {
    private final WeavingApp parent;

    public MainMenu(WeavingApp parent) {
        super();
        this.parent = parent;

        JMenu fileMenu = new JMenu("File");
        JMenuItem openMenuItem = new JMenuItem("Open");
        openMenuItem.addActionListener(parent::openFile);
        fileMenu.add(openMenuItem);
        add(fileMenu);

        JMenu viewMenu = new JMenu("Windows");
        parent.getWindows().forEach(w -> {
            DockableMenuItem item = new DockableMenuItem(w.getPersistentID(),w.getTabText());
            viewMenu.add(item);
        });
        add(viewMenu);
    }
}
