module WeavingRadonJava {
    requires com.formdev.flatlaf;
    requires java.datatransfer;
    requires java.desktop;
    requires java.logging;
    requires java.prefs;
    requires modern_docking.api;
    requires modern_docking.single_app;
    requires modern_docking.ui_ext;
    requires vecmath;
    requires com.github.weisj.jsvg;

    exports com.marginallyclever.weavingradon.core;
}