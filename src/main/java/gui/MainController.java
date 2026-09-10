package gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import core.HuffmanCompressor;
import core.HuffmanDecompressor;

/**
 * MainController builds and manages the entire main window UI.
 *
 * It is responsible for:
 *   1. Building every visual component (title, tabs, drop zone, etc.)
 *   2. Handling user interactions (button clicks, file selection)
 *   3. Calling HuffmanCompressor / HuffmanDecompressor when needed
 *   4. Displaying results in the stats panel, table, and header display

 */
public class MainController {


    private Stage stage; // We need this to open file chooser dialogs

    private boolean isCompressMode = true;

    private File selectedFile = null;

    private Button compressTabButton;
    private Button decompressTabButton;


    private VBox dropZone;
    private Label dropZoneMainText;
    private Label dropZoneSubText;
    private Label selectedFileLabel;

    private Button actionButton;

    // Stats panel labels — updated after compression/decompression
    private Label originalSizeValue;
    private Label compressedSizeValue;
    private Label ratioValue;
    private Label headerSizeValue;
    private Label treeBitLengthValue;
    private Label paddingBitsValue;

    // The grid that holds the encoding table rows
    private GridPane encodingTableGrid;

    // The text area that shows the header bits
    private Label headerBitsLabel;

    private Label statusLabel;

    // Results section — hidden until compression runs
    private HBox statsAndTableRow;
    private VBox headerPanel;
    private VBox resultsSection;


    public MainController(Stage stage) {
        this.stage = stage;
    }

    public Scene buildScene(double width, double height) {

        // ── Root layout ──────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.getStyleClass().add("main-bg");

        root.setTop(buildTitleBar());

        // ── CENTER: scrollable main content
        // We wrap in ScrollPane so the window can scroll if the content is taller than the window
        ScrollPane scrollPane = new ScrollPane(buildCenterContent());
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setFitToWidth(true);  // content stretches to fill width
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setCenter(scrollPane);

        return new Scene(root, width, height);
    }

    /**
     * Builds the title bar at the top of the window.
     *
     * Contains:
     * app title + subtitle
     */
    private HBox buildTitleBar() {

        // ── Left side: title + subtitle
        Label title = new Label(" HUFFMAN_TOOL");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Optimal prefix code compression and decompression engine");
        subtitle.getStyleClass().add("app-subtitle");

        VBox titleSection = new VBox(4, title, subtitle);
        titleSection.setAlignment(Pos.CENTER_LEFT);

        // ── Combine
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("title-bar");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.getChildren().add(titleSection);
        return titleBar;
    }


    /**
     * Builds the main scrollable content area.
     *
     * The results section (stats + table + header bits)
     * is hidden by default and only appears after
     * compression or decompression runs.
     * This feels more professional — like real web tools.
     */
    private VBox buildCenterContent() {

        VBox content = new VBox(20);
        content.getStyleClass().add("main-bg");
        content.setPadding(new Insets(28, 32, 32, 32));
        content.setAlignment(Pos.TOP_CENTER);

        // 1. Tab buttons
        content.getChildren().add(buildTabButtons());

        // 2. Drop zone
        content.getChildren().add(buildDropZone());

        // 3. Selected file label
        selectedFileLabel = new Label("No file selected");
        selectedFileLabel.getStyleClass().add("status-ready");
        content.getChildren().add(selectedFileLabel);

        // 4. Status label
        statusLabel = new Label("Select a file to begin.");
        statusLabel.getStyleClass().add("status-ready");
        content.getChildren().add(statusLabel);

        // 5. Action button
        content.getChildren().add(buildActionButton());

        // 6. Results section — HIDDEN until compression runs
        // We build it now but set it invisible
        // After compression → we make it visible
        resultsSection = new VBox(20);
        resultsSection.setVisible(false); // hidden initially
        resultsSection.setManaged(false); // takes no space when hidden
        // (managed=false means layout ignores it completely)

        statsAndTableRow = buildStatsAndTable();
        headerPanel = buildHeaderPanel();

        resultsSection.getChildren().addAll(statsAndTableRow, headerPanel);
        content.getChildren().add(resultsSection);

        return content;
    }

    /**
     * Makes the results section visible after compression runs.
     *
     * Why both visible AND managed?
     *   visible=true  → we can see it
     *   managed=true  → the layout makes room for it
     *
     * If we only set visible=true without managed=true,
     * the panel appears but overlaps other elements.
     * Both must be true for it to behave correctly.
     */
    private void showResults() {
        resultsSection.setVisible(true);
        resultsSection.setManaged(true);
    }

    /**
     * Builds the COMPRESS / DECOMPRESS tab toggle.
     *
     * Only one tab can be active at a time.
     * Active tab → purple background (tab-button-active)
     * Inactive tab → white background (tab-button)
     *
     * When a tab is clicked:
     *   - isCompressMode updates
     *   - Button styles swap
     *   - Drop zone hint text updates
     *   - Action button text updates
     */
    private HBox buildTabButtons() {

        compressTabButton = new Button("COMPRESS");
        decompressTabButton = new Button("DECOMPRESS");

        // Start with COMPRESS as active
        compressTabButton.getStyleClass().add("tab-button-active");
        decompressTabButton.getStyleClass().add("tab-button");

        // COMPRESS tab clicked
        compressTabButton.setOnAction(e -> {
            isCompressMode = true;
            selectedFile = null;
            selectedFileLabel.setText("No file selected");
            statusLabel.setText("Select a file to begin.");
            statusLabel.getStyleClass().setAll("status-ready");

            // Swap button styles
            compressTabButton.getStyleClass().setAll("tab-button-active");
            decompressTabButton.getStyleClass().setAll("tab-button");

            // Update drop zone appearance and text
            dropZone.getStyleClass().setAll("drop-zone");
            dropZoneMainText.setText("Drag & drop a file here, or click to select");
            dropZoneSubText.setText("Any file type supported");
            actionButton.setText("COMPRESS NOW");
        });

        // DECOMPRESS tab clicked
        decompressTabButton.setOnAction(e -> {
            isCompressMode = false;
            selectedFile = null;
            selectedFileLabel.setText("No file selected");
            statusLabel.setText("Select a .huff file to decompress.");
            statusLabel.getStyleClass().setAll("status-ready");

            // Swap button styles
            decompressTabButton.getStyleClass().setAll("tab-button-active");
            compressTabButton.getStyleClass().setAll("tab-button");

            // Update drop zone appearance and text
            dropZone.getStyleClass().setAll("drop-zone-inactive");
            dropZoneMainText.setText("Drag & drop a .huff file here, or click to select");
            dropZoneSubText.setText("Only .huff files supported");
            actionButton.setText("DECOMPRESS NOW");
        });

        HBox tabs = new HBox(0, compressTabButton, decompressTabButton);
        tabs.setAlignment(Pos.CENTER_LEFT);
        return tabs;
    }


    /**
     * Builds the file drop zone.
     * Compact and centered — not too tall, not too wide.
     * Click anywhere to open the file chooser.
     */
    private VBox buildDropZone() {

        Label icon = new Label("📁");
        icon.setStyle("-fx-font-size: 28px;");

        dropZoneMainText = new Label("Drag & drop a file here, or click to select");
        dropZoneMainText.getStyleClass().add("drop-zone-text");

        dropZoneSubText = new Label("Any file type supported");
        dropZoneSubText.getStyleClass().add("drop-zone-subtext");

        // Put icon and text in a horizontal row — more compact
        HBox content = new HBox(14, icon, new VBox(4, dropZoneMainText, dropZoneSubText));content.setAlignment(Pos.CENTER);

        dropZone = new VBox(content);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.getStyleClass().add("drop-zone");
        dropZone.setPrefHeight(90);  // much more compact than before
        dropZone.setMaxWidth(Double.MAX_VALUE);
        dropZone.setOnMouseClicked(e -> openFileChooser());

        // ── Enable actual drag and drop ───────────────────────────
        // When user drags a file over the drop zone
        dropZone.setOnDragOver(event -> {
            // Only accept if the dragged item contains files
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });

   // When user drops the file onto the drop zone
        dropZone.setOnDragDropped(event -> {
            javafx.scene.input.Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasFiles()) {
                File droppedFile = db.getFiles().get(0); // take first file only

                // In DECOMPRESS mode → only accept .huff files
                if (!isCompressMode && !droppedFile.getName().toLowerCase().endsWith(".huff")) {
                    statusLabel.setText(" Please drop a .huff file to decompress.");
                    statusLabel.getStyleClass().setAll("status-error");
                } else {
                    // Valid file — load it
                    selectedFile = droppedFile;
                    selectedFileLabel.setText("Selected: " + droppedFile.getName());
                    selectedFileLabel.getStyleClass().setAll("status-success");
                    statusLabel.setText("File ready. Click the button to proceed.");
                    statusLabel.getStyleClass().setAll("status-ready");
                    success = true;
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });

        return dropZone;
    }

    /**
     * Builds the main action button.
     *
     * Text changes based on mode:
     *   COMPRESS mode   → "COMPRESS NOW"
     *   DECOMPRESS mode → "DECOMPRESS NOW"
     *
     * Clicking it triggers the actual compression or decompression.
     */
    private HBox buildActionButton() {

        actionButton = new Button("COMPRESS NOW");
        actionButton.getStyleClass().add("action-button");

        actionButton.setOnAction(e -> handleAction());

        HBox wrapper = new HBox(actionButton);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    // STATS PANEL + ENCODING TABLE
    /**
     * Builds the stats panel and encoding table side by side.
     *
     * Left side  → stats panel (file sizes, ratio, header info)
     * Right side → encoding table (byte, freq, code, length)
     *
     * We use HBox with equal column widths.
     * Each side grows to fill available space.
     */
    private HBox buildStatsAndTable() {

        HBox row = new HBox(16);
        row.setAlignment(Pos.TOP_CENTER);

        // ── Left: Stats panel
        VBox statsPanel = buildStatsPanel();
        HBox.setHgrow(statsPanel, Priority.ALWAYS);

        // ── Right: Encoding table
        VBox tablePanel = buildEncodingTable();
        HBox.setHgrow(tablePanel, Priority.ALWAYS);

        row.getChildren().addAll(statsPanel, tablePanel);
        return row;
    }

    /**
     * Builds the statistics panel.
     *
     * Shows:
     *   - Original file size
     *   - Compressed file size
     *   - Compression ratio (highlighted in green)
     *   - Header size in bytes
     *   - Tree bit length
     *   - Padding bits used
     *
     * All values start as "—" and are filled after compression.
     */
    private VBox buildStatsPanel() {

        Label panelTitle = new Label("COMPRESSION RESULTS");
        panelTitle.getStyleClass().add("stats-title");

        // Create each stat row
        originalSizeValue  = new Label("—");
        compressedSizeValue = new Label("—");
        ratioValue = new Label("—");
        headerSizeValue = new Label("—");
        treeBitLengthValue = new Label("—");
        paddingBitsValue = new Label("—");

        // Style the values
        originalSizeValue.getStyleClass().add("stats-value");
        compressedSizeValue.getStyleClass().add("stats-value");
        ratioValue.getStyleClass().add("stats-value-green");
        headerSizeValue.getStyleClass().add("stats-value-purple");
        treeBitLengthValue.getStyleClass().add("stats-value");
        paddingBitsValue.getStyleClass().add("stats-value");

        // Build a grid of label → value pairs
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);

        addStatRow(grid, 0, "Original Size:",  originalSizeValue);
        addStatRow(grid, 1, "Compressed Size:",compressedSizeValue);
        addStatRow(grid, 2, "Compression Ratio:",ratioValue);
        addStatRow(grid, 3, "Header Size:", headerSizeValue);
        addStatRow(grid, 4, "Tree Bit Length:",treeBitLengthValue);
        addStatRow(grid, 5, "Padding Bits:", paddingBitsValue);

        VBox panel = new VBox(16, panelTitle, grid);
        panel.getStyleClass().add("stats-panel");
        return panel;
    }

    /**
     * Helper: adds one label+value row to the stats grid.
     *
     * @param grid  the GridPane to add to
     * @param row   which row index
     * @param text  the label text (e.g. "Original Size:")
     * @param value the Label node that will show the value
     */
    private void addStatRow(GridPane grid, int row, String text, Label value) {
        Label label = new Label(text);
        label.getStyleClass().add("stats-label");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }

    /**
     * Builds the encoding table.
     *
     * Columns: Byte | Char | Frequency | Code | Length
     * Header row → purple background
     * Data rows  → alternating white and light gray
     *

     */
    private VBox buildEncodingTable() {

        Label panelTitle = new Label("ENCODING TABLE");
        panelTitle.getStyleClass().add("stats-title");

        // The grid that holds all rows
        encodingTableGrid = new GridPane();
        encodingTableGrid.setHgap(0);
        encodingTableGrid.setVgap(0);

        // Set equal column widths
        for (int i = 0; i < 5; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(20); // 5 columns × 20% = 100%
            encodingTableGrid.getColumnConstraints().add(col);
        }

        // Build the header row
        String[] headers = {"Byte", "Char", "Frequency", "Code", "Length"};
        for (int i = 0; i < headers.length; i++) {
            Label cell = new Label(headers[i]);
            cell.getStyleClass().add("table-header-cell");
            cell.setMaxWidth(Double.MAX_VALUE);
            GridPane.setFillWidth(cell, true);
            encodingTableGrid.add(cell, i, 0);
        }

        // Wrap table in scrollable area
        // (could be many rows for files with lots of unique bytes)
        ScrollPane tableScroll = new ScrollPane(encodingTableGrid);
        tableScroll.getStyleClass().add("scroll-pane");
        tableScroll.setFitToWidth(true);
        tableScroll.setPrefHeight(220);

        VBox panel = new VBox(16, panelTitle, tableScroll);
        panel.getStyleClass().add("table-panel");
        return panel;
    }


    // HEADER BITS PANEL

    /**
     * Builds the header bits display panel.
     *
     * Shows the raw bits of the header in groups of 8
     * so the professor can see exactly what was written:
     *   ext length | ext chars | tree length | tree | padding
     *
     */
    private VBox buildHeaderPanel() {

        Label panelTitle = new Label("HEADER BITS");
        panelTitle.getStyleClass().add("stats-title");

        Label sectionHint = new Label("[ ext_len | ext_chars | tree_len | tree_bits | padding ]");
        sectionHint.getStyleClass().add("section-label");

        headerBitsLabel = new Label("Run compression to see header bits.");
        headerBitsLabel.getStyleClass().add("header-bits-text");
        headerBitsLabel.setWrapText(true);

        VBox panel = new VBox(12, panelTitle, sectionHint, headerBitsLabel);
        panel.getStyleClass().add("header-panel");
        return panel;
    }


    // FILE CHOOSER

    /**
     * Opens a file chooser dialog so the user can pick a file.
     *
     * In COMPRESS mode:   accepts any file type
     * In DECOMPRESS mode: accepts only .huff files
     *
     * After selection:
     *   - selectedFile is updated
     *   - selectedFileLabel shows the file name
     */
    private void openFileChooser() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a File");

        if (isCompressMode) {
            // Compress mode — any file is valid
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
        }else {
            // Decompress mode — only .huff files
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Huffman Files", "*.huff"));
        }

        File chosen = fileChooser.showOpenDialog(stage);

        if (chosen != null) {
            selectedFile = chosen;
            selectedFileLabel.setText("Selected: " + chosen.getName());
            selectedFileLabel.getStyleClass().setAll("status-success");
            statusLabel.setText("File ready. Click the button to proceed.");
            statusLabel.getStyleClass().setAll("status-ready");
        }
    }


    // ACTION HANDLER

    /**
     * Called when the user clicks COMPRESS NOW or DECOMPRESS NOW.

     */
    private void handleAction() {
        if (selectedFile == null) {
            statusLabel.setText("Please select a file first!");
            statusLabel.getStyleClass().setAll("status-error");
            return;
        }

        if (isCompressMode) {
            handleCompression();
        } else {
            handleDecompression();
        }
    }

    /**
     * Handles the full compression pipeline when
     * "COMPRESS NOW" is clicked.
     *
     * Steps:
     *   1. Ask user where to save the .huff file
     *   2. Run HuffmanCompressor
     *   3. Show results (stats + table + header bits)
     *   4. Update status label
     */
    private void handleCompression() {
        try {
            // ── Step 1: ask where to save the output file
            FileChooser saveChooser = new FileChooser();
            saveChooser.setTitle("Save Compressed File As");

            // Suggest the same name but with .huff extension
            String suggestedName = removeExtension(selectedFile.getName()) + ".huff";
            saveChooser.setInitialFileName(suggestedName);

            // Start in the same folder as the input file
            saveChooser.setInitialDirectory(selectedFile.getParentFile());
            saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Huffman Files", "*.huff"));

            File outputFile = saveChooser.showSaveDialog(stage);
            if (outputFile == null) return; // user cancelled

            // ── Step 2: run compression
            statusLabel.setText("Compressing...");
            statusLabel.getStyleClass().setAll("status-ready");

            HuffmanCompressor compressor = new HuffmanCompressor();
            compressor.compress(selectedFile.getAbsolutePath(), outputFile.getAbsolutePath());

            // ── Step 3: show results
            populateStats(compressor);
            populateEncodingTable(compressor);
            populateHeaderBits(compressor);
            showResults();

            // ── Step 4: update status
            statusLabel.setText(String.format(" Compressed successfully! Saved as: %s", outputFile.getName()));
            statusLabel.getStyleClass().setAll("status-success");

        } catch (Exception ex) {
            statusLabel.setText(" Error: " + ex.getMessage());
            statusLabel.getStyleClass().setAll("status-error");
            ex.printStackTrace();
        }
    }

    /**
     * Handles the full decompression pipeline when
     * "DECOMPRESS NOW" is clicked.
     *
     * Steps:
     *   1. Read the original extension from the .huff header
     *   2. Ask user where to save the restored file
     *   3. Run HuffmanDecompressor
     *   4. Update status label
     *
     * Note: we don't show the encoding table after decompression
     * because we don't rebuild the codes — we only reconstruct
     * the tree structure, not the frequency table.
     */
    private void handleDecompression() {
        try {
            // ── Step 1: peek at the header to get the extension
            String originalExt = getExtensionFromHeader(
                    selectedFile.getAbsolutePath()
            );

            // ── Step 2: ask where to save the restored file
            FileChooser saveChooser = new FileChooser();
            saveChooser.setTitle("Save Decompressed File As");

            // Suggest the original name with original extension
            String suggestedName = removeExtension(selectedFile.getName()) + "." + originalExt;
            saveChooser.setInitialFileName(suggestedName);
            saveChooser.setInitialDirectory(selectedFile.getParentFile());

            File outputFile = saveChooser.showSaveDialog(stage);
            if (outputFile == null) return; // user cancelled

            // ── Step 3: run decompression
            statusLabel.setText("Decompressing...");
            statusLabel.getStyleClass().setAll("status-ready");

            HuffmanDecompressor decompressor = new HuffmanDecompressor();
            decompressor.decompress(selectedFile.getAbsolutePath(), outputFile.getAbsolutePath());

            // ── Step 4: update status
            statusLabel.setText(String.format(" Decompressed successfully! Restored as: %s", outputFile.getName()));
            statusLabel.getStyleClass().setAll("status-success");

        } catch (Exception ex) {statusLabel.setText(" Error: " + ex.getMessage());
            statusLabel.getStyleClass().setAll("status-error");
            ex.printStackTrace();
        }
    }

    /**
     * Fills the stats panel with compression results.
     *
     * @param compressor the compressor after running compress()
     */
    private void populateStats(HuffmanCompressor compressor) {

        // Format sizes nicely (bytes, KB, or MB)
        originalSizeValue.setText(formatSize(compressor.getOriginalFileSize()));
        compressedSizeValue.setText(formatSize(compressor.getCompressedFileSize()));

        // Compression ratio — green means we saved space
        double ratio = compressor.getCompressionRatio();
        ratioValue.setText(String.format("%.2f%%", ratio));

        if (ratio > 0) {
            ratioValue.getStyleClass().setAll("stats-value-green");
        } else {
            // Negative ratio means file got bigger — show in red
            ratioValue.getStyleClass().setAll("status-error");
        }

        // Header size in bytes
        // Header bits = 8 + (8×extLen) + 32 + treeBitLength + 8
        int headerBits = 8 + (8 * compressor.getFileExtension().length()) + 32 + compressor.getTreeBitLength() + 8;
        // Convert bits to bytes (round up)
        int headerBytes = (int) Math.ceil(headerBits / 8.0);
        headerSizeValue.setText(headerBytes + " bytes (" + headerBits + " bits)");

        treeBitLengthValue.setText(compressor.getTreeBitLength() + " bits");
        paddingBitsValue.setText(compressor.getPaddingBits() + " bits");
    }

    /**
     * Fills the encoding table with one row per unique byte.
     *
     * Columns: Byte value | Char | Frequency | Code | Code Length
     *
     * We clear any old rows first (row 0 is the header — keep it).
     * Then add one row per byte that appeared in the file.
     */
    private void populateEncodingTable(HuffmanCompressor compressor) {

        // Remove all rows except the header (row 0)
        encodingTableGrid.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) > 0);

        String[] codes = compressor.getHuffmanTree().getCodes();
        long[] freq    = compressor.getHuffmanTree().getFrequencyTable();

        int rowIndex = 1; // start after header row

        for (int i = 0; i < 256; i++) {
            if (codes[i] != null) {

                // Determine row style — alternating white and light gray
                String cellStyle = (rowIndex % 2 == 0) ? "table-cell-even" : "table-cell-odd";

                // Column 0: byte value (decimal)
                addTableCell(String.valueOf(i), 0, rowIndex, cellStyle);

                // Column 1: character representation
                // Printable chars show as the char itself
                // Non-printable show as their decimal value
                String charDisplay;
                if (i >= 32 && i <= 126) {
                    charDisplay = "'" + (char) i + "'";
                } else {
                    charDisplay = "[" + i + "]";
                }
                addTableCell(charDisplay, 1, rowIndex, cellStyle);

                // Column 2: frequency
                addTableCell(String.valueOf(freq[i]), 2, rowIndex, cellStyle);

                // Column 3: Huffman code
                addTableCell(codes[i], 3, rowIndex, cellStyle);

                // Column 4: code length in bits
                addTableCell(codes[i].length() + " bit(s)", 4, rowIndex, cellStyle);

                rowIndex++;
            }
        }
    }

    /**
     * Helper: adds one cell to the encoding table grid.
     *
     * @param text      the text to display
     * @param col       column index
     * @param row       row index
     * @param styleClass the CSS class for styling
     */
    private void addTableCell(String text, int col, int row, String styleClass) {
        Label cell = new Label(text);
        cell.getStyleClass().add(styleClass);
        cell.setMaxWidth(Double.MAX_VALUE);
        GridPane.setFillWidth(cell, true);
        encodingTableGrid.add(cell, col, row);
    }

    /**
     * Builds and displays the header bits string.
     *
     * We reconstruct the header bit by bit to show
     * exactly what was written into the .huff file.
     *
     * Format: groups of 8 bits separated by spaces
     * with section labels showing what each part means.
     *
     * Example:
     *   [ext_len] 00000011
     *   [ext]     01110100 01111000 01110100
     *   [tree_len] 00000000 00000000 00000000 00100111
     *   [tree]    10100001 ...
     *   [padding] 00000011
     */
    private void populateHeaderBits(HuffmanCompressor compressor) {

        StringBuilder sb = new StringBuilder();

        String ext         = compressor.getFileExtension();
        int treeBitLength  = compressor.getTreeBitLength();
        int padding        = compressor.getPaddingBits();

        // ── Piece 1: extension length (8 bits)
        sb.append("[ext_len]  ");
        sb.append(toBinary8(ext.length()));
        sb.append("\n");

        // ── Piece 2: extension characters (8 bits each)
        sb.append("[ext]      ");
        for (int i = 0; i < ext.length(); i++) {
            sb.append(toBinary8(ext.charAt(i)));
            if (i < ext.length() - 1) sb.append(" ");
        }
        sb.append("\n");

        // ── Piece 3: tree bit length (32 bits)
        sb.append("[tree_len] ");
        sb.append(toBinary32(treeBitLength));
        sb.append("\n");

        // ── Piece 4: tree bits (variable)
        // We re-serialize the tree using PostOrder to get the bits
        sb.append("[tree]     ");
        StringBuilder treeBits = new StringBuilder();
        serializeTreeBits(compressor.getHuffmanTree().getRoot(), treeBits);
        // Display in groups of 8 bits
        String treeStr = treeBits.toString();
        for (int i = 0; i < treeStr.length(); i++) {
            sb.append(treeStr.charAt(i));
            if ((i + 1) % 8 == 0 && i < treeStr.length() - 1) {
                sb.append(" ");
            }
        }
        sb.append("\n");

        // ── Piece 5: padding count (8 bits)
        sb.append("[padding]  ");
        sb.append(toBinary8(padding));

        headerBitsLabel.setText(sb.toString());
    }

    /**
     * Recursively serializes the tree in PostOrder to a bit string.
     * Mirrors writeTreeBits() in HuffmanCompressor exactly.
     *
     * Leaf node     → "1" + 8-bit byte value
     * Internal node → recurse left, recurse right, then "0"
     */
    private void serializeTreeBits(core.HuffmanNode node, StringBuilder sb) {
        if (node == null) return;

        if (node.isLeaf) {
            sb.append("1");
            sb.append(toBinary8String(node.byteValue));
        } else {
            serializeTreeBits(node.left, sb);
            serializeTreeBits(node.right, sb);
            sb.append("0");
        }
    }

    /**
     * Converts a value to an 8-bit binary string.
     * Example: 65 → "01000001"
     */
    private String toBinary8(int value) {
        String binary = Integer.toBinaryString(value & 0xFF);
        // Pad with leading zeros to ensure exactly 8 bits
        while (binary.length() < 8) binary = "0" + binary;
        return binary;
    }

    /**
     * Same as toBinary8 — used inside serializeTreeBits
     * for clarity (avoids confusion with the other method).
     */
    private String toBinary8String(int value) {
        return toBinary8(value);
    }

    /**
     * Converts a value to a 32-bit binary string in groups of 8.
     * Example: 59 → "00000000 00000000 00000000 00111011"
     */
    private String toBinary32(int value) {
        String binary = Integer.toBinaryString(value);
        while (binary.length() < 32) binary = "0" + binary;
        // Add spaces every 8 bits for readability
        return binary.substring(0, 8)  + " " +
                binary.substring(8, 16) + " " +
                binary.substring(16, 24)+ " " +
                binary.substring(24, 32);
    }

    /**
     * Formats a byte count into a readable string.
     * Example: 1024 → "1.0 KB"
     */
    private String formatSize(long bytes) {
        if (bytes >= 1_048_576) {
            return String.format("%.1f MB", bytes / 1_048_576.0);
        } else if (bytes >= 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return bytes + " bytes";
        }
    }

    /**
     * Removes the extension from a file name.
     * Example: "document.txt" → "document"
     */
    private String removeExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) return fileName;
        return fileName.substring(0, lastDot);
    }

    /**
     * Reads just the extension from a .huff file header.
     * Used before decompression to suggest the correct output name.
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
            return "bin";
        }
    }


}