package com.marginallyclever.weavingradon;

import javax.swing.*;

public class MainMenu extends JMenuBar {
    private final WeavingRadon parent;

    public MainMenu(WeavingRadon parent) {
        super();
        this.parent = parent;

        JMenu fileMenu = new JMenu("File");
        JMenuItem openMenuItem = new JMenuItem("Open");
        openMenuItem.addActionListener(parent::openFile);
        fileMenu.add(openMenuItem);
        add(fileMenu);
    }
}
