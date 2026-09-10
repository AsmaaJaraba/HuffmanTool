package core;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * HuffmanCompressor handles the full compression pipeline.
 *
 * It takes an input file, compresses it using Huffman coding,
 * and writes the result to a .huff output file.
 *
 * The output file structure:
 *   [ext length 8 bits]
 *   [ext chars 8 bits each]
 *   [tree length 32 bits]
 *   [tree bits - PostOrder]
 *   [padding count 8 bits]
 *   [compressed data bits]
 */
public class HuffmanCompressor {

    // Buffer size matches the system bus (8KB)
    private static final int BUFFER_SIZE = 8192;

    // The HuffmanTree does (frequency counting, tree building, code generation)
    private HuffmanTree huffmanTree;

    // Stores the original file's extension (e.g. "txt", "png")
    // We need this so decompression can restore the original file name
    private String fileExtension;

    // How many bits the serialized tree takes up in the header
    // We need to calculate this BEFORE writing so we can store it first
    private int treeBitLength;

    // How many 0 bits we add at the end to complete the last byte
    private int paddingBits;

    // Statistics — filled after compression for the GUI to display
    private long originalFileSize;
    private long compressedFileSize;


    public HuffmanCompressor() {
        huffmanTree = new HuffmanTree();
    }

    public void compress(String inputFilePath, String outputFilePath) throws IOException {

        // ── Step 1: extract the file extension
        // We need to store it in the header so decompression can restore the original file name and extension
        fileExtension = getExtension(inputFilePath);

        // ── Step 2: count how many times each byte appears
        huffmanTree.countFrequencies(inputFilePath);

        // ── Step 3: build the Huffman Tree from frequencies
        huffmanTree.buildTree();

        // ── Step 4: generate codes for each byte
        huffmanTree.generateCodes();

        // ── Step 5: calculate tree bit length
        // We need to know this BEFORE writing the tree so we can store it just before the tree in the header
        calculateTreeBitLength(huffmanTree.getRoot());

        // ── Step 6: calculate padding bits
        // We need to know this BEFORE writing data
        // so we can store it in the header
        calculatePaddingBits();


        // Record the original file size for statistics
        originalFileSize = new java.io.File(inputFilePath).length();

        // ── Step 7: open the BitWriter and write everything
        BitWriter bitWriter = new BitWriter(outputFilePath);

        // Write header first, then compressed data
        writeHeader(bitWriter);
        writeCompressedData(bitWriter, inputFilePath);

        // Flush any remaining bits + close the file
        bitWriter.flush();
        bitWriter.close();

        // Record compressed file size for statistics
        compressedFileSize = new java.io.File(outputFilePath).length();
    }

    /**
     * Writes the complete header to the .huff file.
     *
     * Header structure:
     *   [ext length  — 8 bits ]
     *   [ext chars   — 8 bits each]
     *   [tree length — 32 bits]
     *   [tree bits   — variable, PostOrder]
     *   [padding     — 8 bits]
     *
     * @param bitWriter the BitWriter connected to the output file
     */
    private void writeHeader(BitWriter bitWriter) throws IOException {

        // ── PIECE 1: extension length (8 bits)
        // Write the NUMBER of characters in the extension.
        // Example: "txt" has 3 characters → write 00000011
        // We use writeFullByte because the length fits in one byte (max 255)
        bitWriter.writeFullByte(fileExtension.length());

        // ── PIECE 2: extension characters (8 bits each)
        // Write each character of the extension as its ASCII value.
        // Example: 't'=116, 'x'=120, 't'=116
        // This lets decompression know the original file's extension.
        for (int i = 0; i < fileExtension.length(); i++) {
            bitWriter.writeFullByte(fileExtension.charAt(i));
        }

        // ── PIECE 3: tree bit length (32 bits)
        // Write HOW MANY BITS the tree takes up.
        // The decompressor reads exactly this many bits for the tree,
        // then knows everything after is compressed data.
        // We use 32 bits because the tree length can be a large number.
        bitWriter.write32Bits(treeBitLength);

        // ── PIECE 4: the Huffman Tree (variable bits)
        // Write the tree using PostOrder traversal: Left → Right → Root
        // Leaf node   → write bit 1, then 8 bits for the byte value
        // Internal node → write bit 0
        writeTreeBits(bitWriter, huffmanTree.getRoot());

        // ── PIECE 5: padding count (8 bits)
        // Write how many fake 0 bits are at the end of the data.
        // The decompressor uses this to know when to stop decoding.
        bitWriter.writeFullByte(paddingBits);
    }

    /**
     * Recursively writes the Huffman Tree in PostOrder (Left→Right→Root).
     *
     * PostOrder means:
     *   1. Visit Left subtree first
     *   2. Visit Right subtree second
     *   3. Visit Root last
     *
     * Encoding rules:
     *   Leaf node     → write 1, then write the byte value (8 bits)
     *   Internal node → write 0
     *
     * Why PostOrder?
     *   When decompressing, we use a Stack.
     *   PostOrder + Stack naturally reconstructs the tree correctly.
     *   (Stack is LIFO — it reverses the PostOrder back to proper tree shape)
     *
     * @param bitWriter the BitWriter to write to
     * @param node   the current node being visited
     * @throws IOException if writing fails
     */
    private void writeTreeBits(BitWriter bitWriter, HuffmanNode node) throws IOException {
        if (node == null) return;

        if (node.isLeaf) {
            // This is a real byte — write 1 as marker, then the byte value
            bitWriter.writeBit(1);
            bitWriter.writeFullByte(node.byteValue);
        } else {
            // Internal node — recurse into children first (PostOrder)
            writeTreeBits(bitWriter, node.left);   // Left first
            writeTreeBits(bitWriter, node.right);  // Right second
            bitWriter.writeBit(0);                 // Root last → write 0
        }
    }


    // COMPRESSED DATA WRITING

    /**
     * Re-reads the input file byte by byte and writes each byte's Huffman code as bits to the output file.
     *
     * Why do we read the file AGAIN?
     *   First read was just for counting frequencies.
     *   Now we need the actual bytes to look up their codes.
     *
     * Pipeline:
     *   input file → buffer (8192B) → read one byte → look up codes[byteValue] → write those bits → output file
     */
    private void writeCompressedData(BitWriter bitWriter, String inputFilePath) throws IOException {

        String[] codes = huffmanTree.getCodes();

        // Open the input file again through an 8192-byte buffer
        BufferedInputStream inputBuffer = new BufferedInputStream(new FileInputStream(inputFilePath), BUFFER_SIZE);

        int byteValue;

        // Read one byte at a time (-1 means end of file)
        while ((byteValue = inputBuffer.read()) != -1) {
            // Look up this byte's Huffman code and write it bit by bit
            bitWriter.writeBitString(codes[byteValue]);
        }

        inputBuffer.close();
    }


    // CALCULATIONS

    /**
     * Calculates how many padding bits flush() will add at the end.
     *
     * The file is ONE continuous bit stream:
     *   [header bits] + [data bits] + [padding zeros]
     *
     * Padding zeros complete the LAST BYTE of the entire stream.
     * So we must count ALL bits — header AND data — to know
     * how many zeros are needed to fill the last byte.
     *
     * IMPORTANT: calculateTreeBitLength() must be called BEFORE this.
     *   Because we need treeBitLength to calculate headerBits correctly.
     *
     * Formula:
     *   totalStreamBits = headerBits + dataBits
     *   paddingBits = (8 - (totalStreamBits % 8)) % 8
     *
     *   The second % 8 handles the case where totalStreamBits
     *   is already a multiple of 8 → paddingBits = 0.
     */
    private void calculatePaddingBits() {
        long[] freq  = huffmanTree.getFrequencyTable();
        String[] codes = huffmanTree.getCodes();

        // Count total data bits
        // (sum of frequency × code length for every byte)
        long totalDataBits = 0;
        for (int i = 0; i < 256; i++) {
            if (codes[i] != null) {
                totalDataBits += freq[i] * codes[i].length();
            }
        }

        // Count total header bits — every piece we write to the header:
        // 8 bits        → extension length
        // 8×extLength   → extension characters
        // 32 bits       → tree bit length value
        // treeBitLength → the actual tree
        // 8 bits        → padding count itself
        long headerBitsTotal = 8 + (8L * fileExtension.length()) + 32 + treeBitLength + 8;

        // Now account for the FULL stream
        long totalStreamBits = headerBitsTotal + totalDataBits;

        // How many zeros needed to complete the last byte?
        paddingBits = (int) ((8 - (totalStreamBits % 8)) % 8);
    }

    /**
     * Calculates how many bits the serialized tree will take.
     *
     * We count BEFORE writing so we can store this number in the header (as 32 bits) right before the tree itself.
     *
     * Counting rules match writeTreeBits exactly:
     *   Leaf node     → 1 bit (marker) + 8 bits (byte value) = 9 bits
     *   Internal node → 1 bit (marker = 0)
     *
     * @param node the current node (start with root)
     */
    private void calculateTreeBitLength(HuffmanNode node) {
        if (node == null) return;

        if (node.isLeaf) {
            treeBitLength += 9; // 1 bit for marker + 8 bits for byte value
        } else {
            treeBitLength += 1; // 1 bit for internal node marker
            calculateTreeBitLength(node.left);
            calculateTreeBitLength(node.right);
        }
    }


    // HELPER METHODS

    /**
     * Extracts the file extension from a file path.
     *
     * Example: "/Users/name/Desktop/document.txt" → "txt"
     * Example: "/Users/name/Desktop/photo.png"    → "png"
     *
     * If no extension exists, returns empty string "".

     */
    private String getExtension(String filePath) {
        // Find the last dot in the file path
        int lastDot = filePath.lastIndexOf('.');

        // If no dot found, or dot is at the very end, no extension
        if (lastDot == -1 || lastDot == filePath.length() - 1) {
            return "";
        }

        return filePath.substring(lastDot + 1);
    }


    public long getOriginalFileSize()    { return originalFileSize; }
    public long getCompressedFileSize()  { return compressedFileSize; }
    public HuffmanTree getHuffmanTree()  { return huffmanTree; }
    public int getPaddingBits()          { return paddingBits; }
    public int getTreeBitLength()        { return treeBitLength; }
    public String getFileExtension()     { return fileExtension; }

    /**
     * Returns the compression ratio as a percentage.
     * Example: 100KB → 28KB means we saved 72% of space.
     *
     * Formula: (1 - compressedSize / originalSize) × 100
     */
    public double getCompressionRatio() {
        if (originalFileSize == 0) return 0;
        return (1.0 - (double) compressedFileSize / originalFileSize) * 100.0;
    }
}