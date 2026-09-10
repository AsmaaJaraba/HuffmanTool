package core;

/**
 * HuffmanNode represents a single node in the Huffman Tree.
 *
 * There are two kinds of nodes:
 *   - Leaf node: holds an actual byte value and its frequency.
 *     These are the "real" characters from the file.
 *   - Internal node: holds only a combined frequency.
 *     These are created when we merge two nodes together during tree building.
 *
 * We use one class for both types, and the boolean 'isLeaf' tells us which type it is.
 */
public class HuffmanNode {

    // The byte value this node represents (only meaningful if isLeaf == true)
    // We use int instead of byte because Java's byte goes from -128 to 127,
    // but file bytes go from 0 to 255. Using int avoids sign issues.
    public int byteValue;

    public long frequency;
    public HuffmanNode left;
    public HuffmanNode right;
    public boolean isLeaf;

    /**
     * Constructor for a LEAF node.
     * Call this when you have a real byte from the file with a known frequency.
     */
    public HuffmanNode(int byteValue, long frequency) {
        this.byteValue = byteValue;
        this.frequency = frequency;
        this.isLeaf = true;
        this.left = null;
        this.right = null;
    }

    /**
     * Constructor for an INTERNAL node.
     * Call this when merging two nodes together during tree construction.
     * Internal nodes don't represent a real byte — they just carry a combined frequency.
     */
    public HuffmanNode(HuffmanNode left, HuffmanNode right) {
        this.left = left;
        this.right = right;
        this.frequency = left.frequency + right.frequency; // combined frequency
        this.isLeaf = false;
        this.byteValue = -1; // -1 means "no real byte value" — this is an internal node
    }
}
