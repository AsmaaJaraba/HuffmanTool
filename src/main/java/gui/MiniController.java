package gui;

import core.HuffmanCompressor;
import core.HuffmanDecompressor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.File;

/**
 * MiniController builds and manages the Quick Mode window.
 *
 * Launched ONLY from right-click in Finder via Automator.
 * The file is pre-loaded — no browsing needed.
 *
 * Rules:
 *   Any file that is NOT .huff → show COMPRESS button only
 *   .huff file                 → show DECOMPRESS button only
 */
public class MiniController {

    // The mini window itself
    private Stage miniStage;

    // The file the user selected or that was pre-loaded
    private File selectedFile;

    // ── UI components we update dynamically
    private Label selectedFileLabel;
    private Label statusLabel;
    private Button compressButton;
    private Button decompressButton;

    public MiniController() {
    }

    /**
     * Opens the mini window with a file ALREADY loaded.
     * Called when the app is launched with a file path argument
     * from the Automator Quick Action (right-click in Finder).

     * The correct button (COMPRESS or DECOMPRESS) shows immediately.
     *
     * @param file the file that was right-clicked in Finder
     */
    public void showWithFile(File file) {
        miniStage = new Stage();
        miniStage.setTitle("Quick Mode — HUFFMAN_TOOL");
        miniStage.setResizable(false);

        Scene scene = new Scene(buildRightClickLayout(file), 420, 260);
        scene.getStylesheets().add(getClass().getResource("/com/huffmantool/styles.css").toExternalForm());

        miniStage.setScene(scene);
        miniStage.show();
    }


    /**
     * Loads a file into the mini window.
     * Shows the correct button based on the file extension:
     *   .huff file → DECOMPRESS button only
     *   any other  → COMPRESS button only
     *
     * @param file the file to load
     */
    private void loadFile(File file) {
        selectedFile = file;
        selectedFileLabel.setText("Selected: " + file.getName());
        selectedFileLabel.getStyleClass().setAll("status-success");

        if (file.getName().toLowerCase().endsWith(".huff")) {
            // .huff file → show DECOMPRESS, hide COMPRESS
            showButton(decompressButton);
            hideButton(compressButton);
            statusLabel.setText("Ready to decompress.");
        } else {
            // Any other file → show COMPRESS, hide DECOMPRESS
            showButton(compressButton);
            hideButton(decompressButton);
            statusLabel.setText("Ready to compress.");
        }
        statusLabel.getStyleClass().setAll("status-ready");
    }

    /**
     * Compresses the selected file.
     * Output: same folder, same name, .huff extension.
     * Example: /Desktop/report.pdf → /Desktop/report.huff
     */
    private void runCompression() {
        try {
            statusLabel.setText("Compressing...");
            statusLabel.getStyleClass().setAll("status-ready");

            String inputPath  = selectedFile.getAbsolutePath();
            String outputPath = removeExtension(inputPath) + ".huff";

            HuffmanCompressor compressor = new HuffmanCompressor();
            compressor.compress(inputPath, outputPath);

            statusLabel.setText(String.format(" Done! %s → %s (%.1f%% saved)", formatSize(compressor.getOriginalFileSize()), formatSize(compressor.getCompressedFileSize()), compressor.getCompressionRatio()));
            statusLabel.getStyleClass().setAll("status-success");

        } catch (Exception ex) {
            statusLabel.setText(" Error: " + ex.getMessage());
            statusLabel.getStyleClass().setAll("status-error");
        }
    }

    /**
     * Decompresses the selected .huff file.
     * Output: same folder, original name and extension restored
     * from the header.
     * Example: /Desktop/report.huff → /Desktop/report.pdf
     */
    private void runDecompression() {
        try {
            statusLabel.setText("Decompressing...");
            statusLabel.getStyleClass().setAll("status-ready");

            String inputPath  = selectedFile.getAbsolutePath();
            String outputPath = HuffmanDecompressor.buildOutputPath(inputPath, getExtensionFromHeader(inputPath)
            );

            HuffmanDecompressor decompressor = new HuffmanDecompressor();
            decompressor.decompress(inputPath, outputPath);

            statusLabel.setText(String.format(" Done! Restored as: %s", new File(outputPath).getName()));
            statusLabel.getStyleClass().setAll("status-success");

        } catch (Exception ex) {
            statusLabel.setText(" Error: " + ex.getMessage());
            statusLabel.getStyleClass().setAll("status-error");
        }
    }

    /** Makes a button visible and reserves space for it. */
    private void showButton(Button button) {
        button.setVisible(true);
        button.setManaged(true);
    }

    /** Hides a button and removes its space from the layout. */
    private void hideButton(Button button) {
        button.setVisible(false);
        button.setManaged(false);
    }

    /**
     * Reads just the original extension from a .huff file header.
     * We only read the first two pieces of the header:
     *   8 bits → extension length
     *   8 bits × length → extension characters
     */
    private String getExtensionFromHeader(String huffFilePath) {
        try {
            core.BitReader bitReader = new core.BitReader(huffFilePath);
            int extLength = bitReader.readFullByte();
            StringBuilder ext = new StringBuilder();
            for (int i = 0; i < extLength; i++) {
                ext.append((char) bitReader.readFullByte());
            }
            bitReader.close();
            return ext.toString();
        } catch (Exception e) {
            return "bin"; // safe fallback
        }
    }

    /**
     * Removes the extension from a file path.
     * Example: "/Desktop/report.pdf" → "/Desktop/report"
     */
    private String removeExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot == -1) return filePath;
        return filePath.substring(0, lastDot);
    }

    /**
     * Formats byte count into readable string.
     * 1024 → "1.0 KB", 1048576 → "1.0 MB", 500 → "500 B"
     */
    private String formatSize(long bytes) {
        if (bytes >= 1_048_576) {
            return String.format("%.1f MB", bytes / 1_048_576.0);
        } else if (bytes >= 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return bytes + " B";
        }
    }

    /**
     * Builds a clean layout for right-click mode.
     * Just shows: title, file name, correct button, status.
     */
    private VBox buildRightClickLayout(File file) {

        // ── Title
        Label title = new Label(" Quick Mode");
        title.getStyleClass().add("app-title");
        title.setStyle("-fx-font-size: 20px;");

        Label subtitle = new Label("Fast compress or decompress any file");
        subtitle.getStyleClass().add("app-subtitle");

        // ── Selected file label
        selectedFileLabel = new Label("Selected: " + file.getName());
        selectedFileLabel.getStyleClass().add("status-success");

        // ── COMPRESS button
        compressButton = new Button("COMPRESS");
        compressButton.getStyleClass().add("action-button");
        compressButton.setMaxWidth(Double.MAX_VALUE);
        compressButton.setVisible(false);
        compressButton.setManaged(false);
        compressButton.setOnAction(e -> runCompression());

        // ── DECOMPRESS button
        decompressButton = new Button("DECOMPRESS");
        decompressButton.getStyleClass().add("action-button");
        decompressButton.setStyle("-fx-background-color: #059669;");
        decompressButton.setMaxWidth(Double.MAX_VALUE);
        decompressButton.setVisible(false);
        decompressButton.setManaged(false);
        decompressButton.setOnAction(e -> runDecompression());

        // ── Status label
        statusLabel = new Label("");
        statusLabel.getStyleClass().add("status-ready");

        // ── Assemble
        VBox layout = new VBox(16, title, subtitle, selectedFileLabel, compressButton, decompressButton, statusLabel);
        layout.setAlignment(Pos.TOP_LEFT);
        layout.setPadding(new Insets(28, 28, 28, 28));
        layout.getStyleClass().add("main-bg");

        // Load the file and show correct button immediately
        loadFile(file);

        return layout;
    }
}