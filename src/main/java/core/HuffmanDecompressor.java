package core;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * HuffmanDecompressor reads a .huff file and restores the original file.
 *
 * The process has two main steps:
 *   Step 1: Read the header → reconstruct the Huffman Tree
 *   Step 2: Read the compressed data → decode bits → write original bytes
 *
 * Everything is done in the EXACT reverse order of compression.
 */
public class HuffmanDecompressor {

    // Buffer size — same as compressor (8192 bytes = 8KB)
    private static final int BUFFER_SIZE = 8192;

    // The root of the Huffman Tree we rebuild from the header
    // Once built, we use it to decode the compressed data
    private HuffmanNode reconstructedRoot;

    // The original file extension read from the header (e.g. "txt")
    // We need this to give the output file its correct name
    private String originalExtension;

    // How many fake 0 bits were added at the end of the data
    // We use this to know when to stop decoding
    private int paddingBits;
    // How many bits the tree occupied in the header
// We need this to calculate where the data section starts
    private int treeBitLength;

    // Statistics for the GUI
    private long compressedFileSize;
    private long decompressedFileSize;

    /**
     * Creates a new HuffmanDecompressor.
     * Call decompress() to run the full pipeline.
     */
    public HuffmanDecompressor() {
        reconstructedRoot = null;
        originalExtension = "";
        paddingBits = 0;
    }

    /**
     * Runs the full decompression pipeline.
     */
    public void decompress(String inputFilePath, String outputFilePath) throws IOException {

        compressedFileSize = new java.io.File(inputFilePath).length();
        decompressedFileSize = new java.io.File(outputFilePath).length();

        // Open the BitReader — reads the .huff file bit by bit
        // This is our read buffer: file → buffer → our program
        BitReader bitReader = new BitReader(inputFilePath);

        // Open the output buffer — writes restored bytes to output file
        // This is our write buffer: program → buffer → output file
        BufferedOutputStream outputBuffer = new BufferedOutputStream(new FileOutputStream(outputFilePath), BUFFER_SIZE);

        // Step 1: read the header and reconstruct the tree
        readHeader(bitReader);

        // Step 2: decode the compressed data
        decodeData(bitReader, outputBuffer);

        // Close everything cleanly
        bitReader.close();
        outputBuffer.flush();
        outputBuffer.close();


    }

    /**
     * Reads the complete header from the .huff file.
     *
     * Reading order matches writing order exactly:
     *   Piece 1: extension length  (8 bits)
     *   Piece 2: extension chars   (8 bits each)
     *   Piece 3: tree bit length   (32 bits)
     *   Piece 4: tree bits         (reconstructed using Stack)
     *   Piece 5: padding count     (8 bits)
     *
     * @param bitReader connected to the .huff file
     * @throws IOException if reading fails
     */
    private void readHeader(BitReader bitReader) throws IOException {

        // ── Piece 1: extension length
        // Read the number that tells us how many chars the extension has
        int extLength = bitReader.readFullByte();

        // ── Piece 2: extension characters
        // Read each character and build the extension string
        StringBuilder ext = new StringBuilder();
        for (int i = 0; i < extLength; i++) {
            int charValue = bitReader.readFullByte();
            ext.append((char) charValue);
        }
        originalExtension = ext.toString();

        // ── Piece 3: tree bit length
        // Read the 32-bit number that tells us exactly how many bits the stored tree occupies.
        // This is CRITICAL — it tells us where the tree ends and where the compressed data begins.
         treeBitLength = bitReader.read32Bits();

        // ── Piece 4: reconstruct the tree
        // Read exactly treeBitLength bits and rebuild the Huffman Tree using our CustomStack
        reconstructedRoot = reconstructTree(bitReader, treeBitLength);

        // ── Piece 5: padding count
        // Read how many fake 0 bits are at the end of the data
        paddingBits = bitReader.readFullByte();
    }

    /**
     * Reconstructs the Huffman Tree from the stored header bits.
     *
     * Uses our CustomStack because the tree was stored in PostOrder.
     * PostOrder stores Left BEFORE Right, so:
     *   - Left node gets pushed FIRST → sits deeper in stack
     *   - Right node gets pushed SECOND → sits on TOP
     *   - When we pop: first pop = Right, second pop = Left
     *
     * Reading rules (mirror of writing rules):
     *   bit = 1 → leaf node: read 8 more bits for byte value, PUSH
     *   bit = 0 → internal node: POP right, POP left, combine, PUSH
     *
     * We read EXACTLY treeBitLength bits — not one more, not one less.
     * This is how we know where the tree ends and data begins.
     *
     * @param bitReader     reads bits from the .huff file
     * @param treeBitLength exactly how many bits belong to the tree
     * @return the root of the fully reconstructed Huffman Tree
     * @throws IOException if reading fails
     */
    private HuffmanNode reconstructTree(BitReader bitReader, int treeBitLength) throws IOException {

        // Our custom stack holds HuffmanNodes during reconstruction
        // 512 safely fits all possible nodes (max 511 for 256 unique bytes)
        CustomStack stack = new CustomStack(512);

        // We track how many bits we have read so far
        // We stop when bitsRead reaches treeBitLength
        int bitsRead = 0;

        while (bitsRead < treeBitLength) {

            int bit = bitReader.readBit();
            bitsRead++; // we consumed one bit

            if (bit == 1) {
                // ── LEAF NODE
                // bit=1 means this is a real byte (a leaf in the tree)
                // The next 8 bits are the byte value in ASCII

                int byteValue = bitReader.readFullByte();
                bitsRead += 8; // we consumed 8 more bits

                // Create the leaf node
                // Frequency is 0 — we don't need it for decoding,
                // only byteValue matters when we reach a leaf
                HuffmanNode leaf = new HuffmanNode(byteValue, 0);

                // Push it onto the stack — it will become someone's child later
                stack.push(leaf);

            } else {
                // ── INTERNAL NODE
                // bit=0 means this is an internal node (not a real byte)
                // We combine the top two nodes from the stack

                // First pop → RIGHT child
                // Because PostOrder visits Right AFTER Left,
                // Right was pushed LAST so it sits on TOP of the stack
                HuffmanNode rightChild = stack.pop();

                // Second pop → LEFT child
                // Left was pushed FIRST so it sits deeper in the stack
                HuffmanNode leftChild = stack.pop();

                // Create internal node linking both children
                // Frequency doesn't matter here either — only structure matters
                HuffmanNode internalNode = new HuffmanNode(leftChild, rightChild);

                // Push the combined node back
                // It may become someone else's child in a later step
                stack.push(internalNode);
            }
        }

        // After reading exactly treeBitLength bits,
        // exactly ONE node remains on the stack — the ROOT
        return stack.pop();
    }

    /**
     * Decodes the compressed data bits and writes the original bytes.
     *
     * How it works:
     *   We walk the reconstructed Huffman Tree bit by bit.
     *   bit=0 → go left
     *   bit=1 → go right
     *   When we reach a leaf → write that byte to output → go back to root
     *
     * When do we stop?
     *   We calculate exactly how many REAL data bits exist:
     *   totalDataBits = (compressedFileSize × 8) - headerBits - paddingBits
     *
     *   We count every bit we read and stop exactly at totalDataBits.
     *   This prevents us from decoding the padding zeros as real data.
     *
     * @param bitReader    reads bits from the .huff file
     * @param outputBuffer writes decoded bytes to the output file
     * @throws IOException if reading or writing fails
     */
    private void decodeData(BitReader bitReader, BufferedOutputStream outputBuffer) throws IOException {

        // ── Calculate how many bits the header took
        // We need this to know where the data section starts and how many real data bits exist
        // 8 bits for ext length
        // 8 × extLength bits for ext characters
        // 32 bits for tree length
        // treeBitLength bits for the tree itself
        // 8 bits for padding count
        int headerBits = 8 + (8 * originalExtension.length()) + 32 + treeBitLength + 8;

        // ── Calculate total REAL data bits
        // Total bits in file = compressedFileSize × 8 Minus the header bits = data section bits
        // Minus padding bits = real data bits (no fake zeros)
        long totalFileBits = compressedFileSize * 8L;
        long realDataBits  = totalFileBits - headerBits - paddingBits;

        // ── Walk the tree and decode
        // Start at the root
        HuffmanNode currentNode = reconstructedRoot;

        // Count how many data bits we have read so far
        long bitsDecoded = 0;

        while (bitsDecoded < realDataBits) {

            int bit = bitReader.readBit();
            bitsDecoded++;

            // Navigate the tree based on the bit
            if (bit == 0) {
                currentNode = currentNode.left;  // go left
            } else {
                currentNode = currentNode.right; // go right
            }

            // If we reached a leaf, we found a complete byte
            if (currentNode.isLeaf) {
                // Write this byte to the output file
                outputBuffer.write(currentNode.byteValue);

                // Go back to root for the next byte
                currentNode = reconstructedRoot;
            }
        }
    }

    /**
     * Builds the output file path for decompression.
     *
     * Input:  "/Users/name/Desktop/document.huff"
     * Output: "/Users/name/Desktop/document.txt"
     *
     * We strip .huff and replace with the original extension
     * that we read from the header.
     *
     * @param huffFilePath      the path to the .huff file
     * @param originalExtension the extension read from the header
     * @return the full restored output file path
     */
    public static String buildOutputPath(String huffFilePath, String originalExtension) {
        // Find the last dot — that's where .huff starts
        int lastDot = huffFilePath.lastIndexOf('.');
        String withoutExtension = huffFilePath.substring(0, lastDot);

        // Attach the original extension back
        return withoutExtension + "." + originalExtension;
    }


    // GETTERS — for GUI display

    public String getOriginalExtension()  { return originalExtension; }
    public int getPaddingBits()           { return paddingBits; }
    public long getCompressedFileSize()   { return compressedFileSize; }
    public long getDecompressedFileSize() { return decompressedFileSize; }
}