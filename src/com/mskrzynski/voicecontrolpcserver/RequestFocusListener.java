package com.mskrzynski.voicecontrolpcserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class RequestFocusListener implements HierarchyListener {

    @Override
    public void hierarchyChanged(HierarchyEvent hierarchyEvent) {
        final Component c = hierarchyEvent.getComponent();
        if (c.isShowing() && (hierarchyEvent.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            Window toplevel = SwingUtilities.getWindowAncestor(c);
            toplevel.addWindowFocusListener(new WindowAdapter() {

                @Override
                public void windowGainedFocus(WindowEvent e) {
                    c.requestFocus();
                }
            });
        }

    }
}
