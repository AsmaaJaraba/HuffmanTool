package gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.File;
import java.util.List;

/**
 * Main is the JavaFX Application entry point.
 *
 * Two launch modes:
 *
 *   Mode 1 — Normal launch (no arguments):
 *     Opens the full main window.
 *     User selects files manually.
 *
 *   Mode 2 — Right-click launch (file path passed as argument):
 *     Opens the mini window directly.
 *     The file is already loaded — user just clicks the button.
 *
 * How does the file path get passed?
 *   When Mac's Automator Quick Action launches our app,
 *   it passes the selected file's path as a command argument.
 *   We read it here and skip straight to the mini window.
 */
public class Main extends Application {

    private static final double WINDOW_WIDTH  = 1100;
    private static final double WINDOW_HEIGHT = 780;

    @Override
    public void start(Stage stage) {

        // Check if a file path was passed as a command line argument
        List<String> args = getParameters().getRaw();

        if (!args.isEmpty()) {
            // ── Mode 2: Right-click launch ───────────────────────
            // A file was passed — open mini window with it loaded
            String filePath = args.get(0);
            File file = new File(filePath);

            if (file.exists()) {
                // Open mini window with the file pre-loaded
                MiniController mini = new MiniController();
                mini.showWithFile(file);
            } else {
                // File doesn't exist — fall back to normal launch
                openMainWindow(stage);
            }

        } else {
            // ── Mode 1: Normal launch
            // No file passed — open the full main window
            openMainWindow(stage);
        }
    }

    /**
     * Opens the full main application window.
     * This is the normal launch path.
     */
    private void openMainWindow(Stage stage) {
        MainController controller = new MainController(stage);
        Scene scene = controller.buildScene(WINDOW_WIDTH, WINDOW_HEIGHT);

        scene.getStylesheets().add(getClass().getResource("/com/huffmantool/styles.css").toExternalForm());

        stage.setTitle("HUFFMAN_TOOL");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}