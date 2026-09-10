package core;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * BitWriter allows us to write individual bits to a file.
 *
 * Why do we need this?
 *   Files store data in bytes (8 bits at a time).
 *   Huffman codes are variable-length bit strings.
 *   We need to pack those bits into complete bytes before writing.
 *
 * How it works:
 *   We accumulate bits one by one into 'currentByte'.
 *   When we have 8 bits, we write that byte to the output buffer.
 *   The buffer (8192 bytes) collects bytes and writes to disk in chunks.
 *
 * Pipeline:
 *   our bits → currentByte → BufferedOutputStream (8192B buffer) → file
 */
public class BitWriter {

    // The buffer layer between our program and the output file
    // 8192 bytes matches the system bus size for efficient I/O
    private BufferedOutputStream outputBuffer;

    // The byte we are currently building up bit by bit
    // We use int (not byte) to avoid Java's sign issues with bytes
    private int currentByte;

    // How many bits we have written into currentByte so far (0 to 7)
    // When this reaches 8, currentByte is complete and gets written to file
    private int bitCount;


    public BitWriter(String filePath) throws IOException {
        outputBuffer = new BufferedOutputStream(new FileOutputStream(filePath), 8192);
        currentByte = 0;
        bitCount = 0;
    }

    /**
     * Writes a single bit (0 or 1) to the output.
     *
     * Step 1: shift currentByte left by 1 to make room
     * Step 2: OR with the new bit to plug it into the rightmost position
     * Step 3: increment bitCount
     * Step 4: if bitCount == 8, the byte is full → write it, reset both
     */
    public void writeBit(int bit) throws IOException {
        // Shift left to make room, then plug in the new bit on the right
        currentByte = (currentByte << 1) | (bit & 1);
        bitCount++;

        // If we have filled a complete byte, write it to the buffer
        if (bitCount == 8) {
            outputBuffer.write(currentByte);
            currentByte = 0; // reset for the next byte
            bitCount = 0;
        }
    }

    /**
     * Writes a full byte value as 8 individual bits.
     *
     * We write from the MOST significant bit to LEAST significant bit.
     * Example: byteValue = 65 = 01000001
     *   We write: 0, 1, 0, 0, 0, 0, 0, 1  (left to right)
     *
     * Why do we go from bit 7 down to bit 0?
     *   Bit 7 is the leftmost (most significant) bit.
     *   We want to write bits in order from left to right.
     *   To extract bit at position i: (byteValue >> i) & 1
     */
    public void writeFullByte(int byteValue) throws IOException {
        // Write all 8 bits from most significant to least significant
        for (int i = 7; i >= 0; i--) {
            writeBit((byteValue >> i) & 1);
        }
    }

    /**
     * Writes a bit string like "1010110" one character at a time.
     * Used when writing Huffman codes for each byte.
     *
     * '0' in the string → writeBit(0)
     * '1' in the string → writeBit(1)
     * We subtract '0' to convert char to int: '1' - '0' = 1, '0' - '0' = 0
     */
    public void writeBitString(String bits) throws IOException {
        for (int i = 0; i < bits.length(); i++) {
            writeBit(bits.charAt(i) - '0');
        }
    }

    /**
     * Writes a 32-bit integer value as exactly 32 individual bits.
     * Used for storing the tree length in the header.
     *
     * Why 32 bits?
     *   The tree length can be a large number.
     *   32 bits can hold values up to ~4 billion — safely enough.
     *
     * We write from bit 31 (most significant) down to bit 0.
     */
    public void write32Bits(int value) throws IOException {
        for (int i = 31; i >= 0; i--) {
            writeBit((value >> i) & 1);
        }
    }

    /**
     * Flushes any remaining bits by padding with 0s.
     *
     * If bitCount > 0, we have a partial byte that isn't full yet.
     * We shift it left to pad the remaining positions with 0s,
     * then write it to the file.
     *
     * Example: bitCount = 3, currentByte = 00000101
     *   We need 5 more bits of padding.
     *   Shift left by 5: 10100000
     *   Write 10100000 to file.

     */
    public void flush() throws IOException {
        if (bitCount > 0) {
            // Shift remaining bits to the left side of the byte
            // The right side becomes 0s automatically (our padding)
            currentByte = currentByte << (8 - bitCount);
            outputBuffer.write(currentByte);
            currentByte = 0;
            bitCount = 0;
        }
        // Tell the buffer to push everything remaining to disk
        outputBuffer.flush();
    }

    /**
     * Closes the output file.
     * Always call this when finished writing.
     */
    public void close() throws IOException {
        outputBuffer.close();
    }
}