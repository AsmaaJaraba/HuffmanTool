package core;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * BitReader allows us to read individual bits from a .huff file.
 *
 * Why do we need this?
 *   The compressed file stores data packed as bits inside bytes.
 *   To decode, we need to read one bit at a time.
 *
 * How it works:
 *   We read one byte at a time from the buffer into 'currentByte'.
 *   We then serve bits one at a time from that byte.
 *   When all 8 bits of currentByte are served, we read the next byte.
 *
 * Pipeline:
 *   file → BufferedInputStream (8192B buffer) → currentByte → individual bits
 */
public class BitReader {

    // The buffer layer between the file and our program
    // Reads 8192 bytes at a time from disk — matches the system bus
    private BufferedInputStream inputBuffer;

    // The current byte we are extracting bits from
    // We use int (not byte) to avoid Java sign issues
    private int currentByte;

    // How many bits are still unread in currentByte (starts at 0)
    // When it reaches 0, we need to read the next byte from the buffer
    private int bitsRemaining;

    /**
     * Opens a .huff file for bit-by-bit reading.
     */
    public BitReader(String filePath) throws IOException {
        inputBuffer = new BufferedInputStream(new FileInputStream(filePath), 8192);
        currentByte = 0;
        bitsRemaining = 0;
    }

    /**
     * Reads and returns a single bit (0 or 1) from the file.
     *
     * How it works:
     *   If bitsRemaining == 0, we need a fresh byte from the buffer.
     *   We read the next byte and set bitsRemaining = 8.
     *
     *   To extract the leftmost (most significant) bit:
     *     (currentByte >> 7) & 1
     *   Then we shift currentByte left by 1 to move the next bit into position.
     *
     * Why leftmost first?
     *   When we WROTE bits, we wrote them left to right (MSB first).
     *   So when we READ them back, we must also go left to right.
     *
     * @return 0 or 1
     * @throws IOException if reading fails or file ends unexpectedly
     */
    public int readBit() throws IOException {
        // If no bits left in currentByte, fetch a new byte from the buffer
        if (bitsRemaining == 0) {
            currentByte = inputBuffer.read();
            bitsRemaining = 8;
        }

        // Extract the leftmost (most significant) bit
        // >> 7 shifts that bit all the way to position 0
        // & 1 keeps only that bit, zeroes everything else
        int bit = (currentByte >> 7) & 1;

        // Shift currentByte left by 1 so the next bit becomes the leftmost
        currentByte = currentByte << 1;

        // We've consumed one bit
        bitsRemaining--;

        return bit;
    }

    /**
     * Reads exactly 8 bits and assembles them into one byte value (0–255).
     *
     * Used for reading:
     *   - Extension length (1 byte)
     *   - Each extension character (1 byte each)
     *   - Padding count (1 byte)
     *   - Leaf byte values during tree reconstruction (1 byte each)
     *
     * We build the value bit by bit:
     *   Start with value = 0
     *   For each bit: shift value left, OR in the new bit
     *   After 8 bits: value holds the complete byte
     *
     * @return the byte value (0–255)
     * @throws IOException if reading fails
     */
    public int readFullByte() throws IOException {
        int value = 0;
        for (int i = 0; i < 8; i++) {
            // Shift left to make room, then plug in the next bit
            value = (value << 1) | readBit();
        }
        return value;
    }

    /**
     * Reads exactly 32 bits and assembles them into one integer.
     *
     * Used for reading the tree length from the header.
     * Must match exactly how write32Bits() wrote it.
     *
     * @return the integer value stored in 32 bits
     * @throws IOException if reading fails
     */
    public int read32Bits() throws IOException {
        int value = 0;
        for (int i = 0; i < 32; i++) {
            value = (value << 1) | readBit();
        }
        return value;
    }

    /**
     * Closes the input file.
     * Always call this when finished reading.
     *
     * @throws IOException if closing fails
     */
    public void close() throws IOException {
        inputBuffer.close();
    }
}