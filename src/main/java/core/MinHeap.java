package core;

/**
 * MinHeap is our custom Priority Queue
 *
 * The key idea:
 *   - We store HuffmanNodes in an array starting at index 1 (index 0 is unused).
 *   - The node with the LOWEST frequency is always at index 1 (the root).
 *   - We use this to always extract the two least-frequent nodes first,
 */
public class MinHeap {

    private HuffmanNode[] heap;
    private int size;

    /**
     * Creates an empty MinHeap with the given capacity.
     * We use 512 to safely fit all 256 possible byte nodes
     * plus the internal nodes created during tree building.
     */
    public MinHeap(int capacity) {
        heap = new HuffmanNode[capacity + 1];
        size = 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public HuffmanNode getMin() {
        if (isEmpty()) return null;
        return heap[1];
    }

    public HuffmanNode removeMin() {
        if (isEmpty()) return null;

        HuffmanNode min = heap[1];
        exch(1, size);

        heap[size] = null; // clear reference so garbage collector can clean up
        size--;

        sink(1);

        return min;
    }

    public void insert(HuffmanNode node) {
        heap[++size] = node;
        swim(size);
    }

    private void swim(int k) {
        // Keep going while we haven't reached the root (k > 1)
        // AND the parent's frequency is GREATER than the current node's frequency
        // If parent > current, current should be higher up → swap
        while (k > 1 && heap[k / 2].frequency > heap[k].frequency) {
            exch(k, k / 2);
            k = k / 2;
        }
    }

    private void sink(int k) {
        // Keep going while a left child exists
        while (2 * k <= size) {
            int j = 2 * k;

            // We always want to swap with the SMALLER child in a MinHeap.
            if (j < size && heap[j].frequency > heap[j + 1].frequency) {
                j++; // right child is smaller, use it
            }

            if (heap[k].frequency <= heap[j].frequency) {
                break;
            }

            // Otherwise swap and continue sinking down
            exch(k, j);
            k = j;
        }
    }

    private void exch(int i, int j) {
        HuffmanNode temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }

    public void clear() {
        size = 0;
    }
}