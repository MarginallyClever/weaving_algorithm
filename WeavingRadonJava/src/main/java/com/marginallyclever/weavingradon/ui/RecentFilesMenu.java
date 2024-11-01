package com.marginallyclever.weavingradon.ui;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * A list of recent files displayed as a JMenu.  The list can be saved to and loaded from {@link Preferences}.
 */
public final class RecentFilesMenu extends JMenu {
    public final int MAX_FILES = 10;

    private final Preferences prefs = Preferences.userNodeForPackage(RecentFilesMenu.class);

    private final ArrayList<String> fileList = new ArrayList<>();
    private ActionListener submenuListener;

    // Load recent files from prefs
    public RecentFilesMenu(String label) {
        super(label);

        loadFromStorage();
        updateLists();
    }

    /**
     * Adds a filename to the recent files list, saves the updated prefs, and refreshes the menus.
     * Changes the order of the recent files with the new item at the top.  If the item was already in the list
     * It is moved to the top.
     * @param filename the file to push to the top of the list.
     */
    public void addFilename(String filename) {
        if(filename==null || filename.trim().isEmpty()) return;

        int i = getIndexOf(filename);
        if(i==-1) {
            fileList.addFirst(filename);
        } else {
            // bump to the head of the list
            fileList.addFirst(fileList.remove(i));
        }
        updateLists();
    }

    /**
     * removes a filename from the recent files list, saves the updated prefs, and refreshes the menus.
     * @param filename the file to remove from the list.
     */
    public void removeFilename(String filename) {
        int i = getIndexOf(filename);
        if(i==-1) return;

        fileList.remove(i);
        updateLists();
    }

    private int getIndexOf(String filename) {
        int i=0;
        for( String j : fileList ) {
            if(j.contentEquals(filename)) {
                return i;
            }
            ++i;
        }
        return -1;
    }

    public int getMaxFiles() {
        return MAX_FILES;
    }

    public String getFile(int index) {
        return fileList.get(index);
    }

    private void updateLists() {
        this.removeAll();

        for(int i=0;i<MAX_FILES;++i) {
            prefs.remove(getNodeName(i));
        }

        int i=0;
        for( String f : fileList ) {
            prefs.put(getNodeName(i++), f);
            JMenuItem item = new JMenuItem(f);
            this.add(item);
            item.addActionListener(submenuListener);
        }
    }

    private void loadFromStorage() {
        for (int i=0; i<MAX_FILES; ++i) {
            String name = getNodeName(i);
            String value = prefs.get(name, "");
            if (!value.trim().isEmpty()) {
                fileList.add(value);
            }
        }
    }

    private String getNodeName(int i) {
        return "recent-files-"+i;
    }

    /**
     * Adds a listener to all the submenus.
     */
    public void addSubmenuListener(ActionListener listener) {
        submenuListener = listener;
        updateLists();
    }
}
