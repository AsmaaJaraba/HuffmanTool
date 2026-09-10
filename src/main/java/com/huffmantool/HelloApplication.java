package com.huffmantool;

import gui.Main;

/**
 * Entry point for the HuffmanTool application.
 *
 * JavaFX requires the main class to be in the module's
 * exported package. We keep this class here and delegate
 * immediately to our Main class in the gui package.
 */
public class HelloApplication {

    public static void main(String[] args) {
        // Delegate to our real Main class in the gui package
        Main.main(args);
    }
}