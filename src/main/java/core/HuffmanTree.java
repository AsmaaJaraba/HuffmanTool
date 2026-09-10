package core;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * HuffmanTree is the central class of the entire project.
 * It is responsible for:
 *
 *   1. Reading a file and counting byte frequencies
 *   2. Building the Huffman Tree using a MinHeap
 *   3. Generating Huffman codes for each byte
 *   4. Serializing (storing) the tree into bits for the header
 *   5. Deserializing (reconstructing) the tree from header bits
 *
 * The tree is built using Huffman's greedy algorithm:
 *   - Always merge the two nodes with the LOWEST frequencies first.
 *   - Repeat until only one node remains — that is the root.
 */
public class HuffmanTree {

    // Buffer size matches the system bus (8KB = 8192 bytes)
    // The CPU moves data in chunks — 8192 is a clean multiple of 8
    private static final int BUFFER_SIZE = 8192;

    // Total number of possible byte values (0–255)
    private static final int ALPHABET_SIZE = 256;

    // frequencyTable[i] = how many times byte value i appeared in the file
    // We use long because a large file could have a single byte appear billions of times, which overflows int (max ~2.1 billion)
    private long[] frequencyTable;

    // codes[i] = the Huffman binary code string for byte value i
    // Example: codes[65] = "0" means byte 'A' is encoded as just one bit: 0
    // Example: codes[70] = "1100" means byte 'F' is encoded as 4 bits: 1100
    // If a byte never appeared in the file, codes[i] stays null
    private String[] codes;

    private HuffmanNode root;

    // How many unique byte values appeared in the file
    // (i.e., how many slots in frequencyTable are greater than 0)
    private int uniqueByteCount;


    public HuffmanTree() {
        frequencyTable = new long[ALPHABET_SIZE];
        codes = new String[ALPHABET_SIZE];
        root = null;
        uniqueByteCount = 0;
    }


    // STEP 1: READ FILE AND COUNT FREQUENCIES

    /**
     * Reads a file byte by byte and fills the frequencyTable.
     *
     * How it works:
     *   - We open the file through a BufferedInputStream (8192 byte buffer).
     *   - read() pulls one byte at a time from the buffer.
     *   - The buffer automatically refills from disk when it runs out.
     *   - We use the byte value (0–255) as the index into frequencyTable.
     *   - When read() returns -1, we've reached the end of the file.
     *
     * After this method finishes:
     *   - frequencyTable[i] holds the count of byte value i
     *   - uniqueByteCount tells us how many distinct bytes appeared
     */
    public void countFrequencies(String filePath) throws IOException {

        // Reset the table before counting (in case this object is reused)
        for (int i = 0; i < ALPHABET_SIZE; i++) {
            frequencyTable[i] = 0;
        }
        uniqueByteCount = 0;

        // Open the file through a buffered stream
        // BufferedInputStream is the buffer layer: file → buffer → program
        BufferedInputStream inputBuffer = new BufferedInputStream(new FileInputStream(filePath), BUFFER_SIZE);

        int byteValue; // read() returns int, not byte — avoids sign issues

        while ((byteValue = inputBuffer.read()) != -1) {
            frequencyTable[byteValue]++;
        }

        inputBuffer.close();

        // Count how many unique bytes appeared (frequency > 0)
        for (int i = 0; i < ALPHABET_SIZE; i++) {
            if (frequencyTable[i] > 0) {
                uniqueByteCount++;
            }
        }
    }


    // STEP 2: BUILD THE HUFFMAN TREE

    /**
     * Builds the Huffman Tree from the frequency table.
     *
     * Algorithm:
     *   1. Create one leaf HuffmanNode for each byte that appeared (freq > 0)
     *   2. Insert all leaf nodes into the MinHeap (priority queue)
     *   3. Repeat until only one node remains in the heap:
     *        a. Extract the node with the SMALLEST frequency (x)
     *        b. Extract the node with the next smallest frequency (y)
     *        c. Create a new internal node z:
     *              z.left = x
     *              z.right = y
     *              z.frequency = x.frequency + y.frequency
     *        d. Insert z back into the heap
     *   4. The last remaining node in the heap is the ROOT
     *
     * This is Huffman's greedy algorithm — always merging the two least frequent nodes ensures optimal prefix codes.
     *
     * Special case: if only one unique byte exists in the file,
     * we manually assign it code "0" to handle it correctly.
     */
    public void buildTree() {

        // Create the MinHeap — capacity 512 fits all 256 leaves plus the 255 internal nodes created during merging (2*256-1 = 511)
        MinHeap heap = new MinHeap(512);

        for (int i = 0; i < ALPHABET_SIZE; i++) {
            if (frequencyTable[i] > 0) {
                // This byte appeared in the file — create a leaf node for it
                HuffmanNode leaf = new HuffmanNode(i, frequencyTable[i]);
                heap.insert(leaf);
            }
        }

        // Special case: only one unique byte in the entire file
        // We can't build a tree with one node, so handle it separately
        if (uniqueByteCount == 1) {
            HuffmanNode onlyNode = heap.removeMin();
            // Create a fake root with this node as left child
            root = new HuffmanNode(onlyNode, new HuffmanNode(0, 0));
            return;
        }

        // Step 3: merge nodes until only one remains (the root)
        // We run this exactly (uniqueByteCount - 1) times
        // because each merge reduces the count by 1
        while (heap.size() > 1) {

            // Extract the two smallest nodes
            HuffmanNode x = heap.removeMin();
            HuffmanNode y = heap.removeMin();

            // Merge them into a new internal node
            // x becomes left child, y becomes right child
            // The new node's frequency = x.frequency + y.frequency
            HuffmanNode z = new HuffmanNode(x, y);

            // Put the merged node back into the heap
            heap.insert(z);
        }

        // Step 4: the last node left is the root of our Huffman Tree
        root = heap.removeMin();
    }


    // STEP 3: GENERATE HUFFMAN CODES


    /**
     * Generates the Huffman code for every byte by traversing the tree.
     *
     * How it works:
     *   - Start at the root with an empty code string ""
     *   - Every time we go LEFT, add "0" to the current code
     *   - Every time we go RIGHT, add "1" to the current code
     *   - When we reach a LEAF node, save the code for that byte
     *
     * We use recursion here. Recursion is naturally a stack-based operation — Java manages the call stack internally.
     */
    public void generateCodes() {
        if (root == null) return;

        // Start the recursive traversal from the root with empty code
        traverseAndAssignCodes(root, "");
    }

    /**
     * Recursive helper that walks the tree and assigns codes.
     *
     * @param node        the current node we are visiting
     * @param currentCode the code built so far on the path from root to here
     */
    private void traverseAndAssignCodes(HuffmanNode node, String currentCode) {

        // Base case: if we reach a null node, stop
        if (node == null) return;

        if (node.isLeaf) {
            codes[node.byteValue] = currentCode;
            return;
        }

        // If it's an internal node, keep going deeper:
        // Go LEFT → add "0" to the code
        traverseAndAssignCodes(node.left, currentCode + "0");

        // Go RIGHT → add "1" to the code
        traverseAndAssignCodes(node.right, currentCode + "1");
    }


    // GETTERS — for use by Compressor, Decompressor, and GUI

    /** Returns the full frequency table (256 slots) */
    public long[] getFrequencyTable() { return frequencyTable; }

    /** Returns the full codes array (256 slots, null if byte unused) */
    public String[] getCodes() { return codes; }

    /** Returns the root of the built Huffman Tree */
    public HuffmanNode getRoot() { return root; }

    /** Returns how many unique bytes appeared in the file */
    public int getUniqueByteCount() { return uniqueByteCount; }

    /** Returns the buffer size we use for file I/O */
    public static int getBufferSize() { return BUFFER_SIZE; }
}