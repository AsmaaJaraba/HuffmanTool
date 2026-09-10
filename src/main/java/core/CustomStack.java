package core;

/**
 * CustomStack is a simple stack data structure built using a plain array.
 * It is used during decompression to reconstruct the Huffman Tree.

 * Why we need this:
 *   - When we read the stored tree from the compressed file,
 *     we read it in PostOrder (Left → Right → Root).
 *   - We push leaf nodes onto the stack as we read them.
 *   - When we see an internal node marker, we pop two nodes,
 *     combine them, and push the result back.
 *   - This is exactly how PostOrder tree reconstruction works.
 */
public class CustomStack {

    // The array that holds our HuffmanNode objects
    // We use 1024 as max size — a Huffman tree for 256 unique bytes
    // will never need more than 512 nodes, so 1024 is safely more than enough
    private HuffmanNode[] stack;
    private int top;
    private int capacity;

    public CustomStack(int capacity) {
        this.capacity = capacity;
        this.stack = new HuffmanNode[capacity];
        this.top = 0;
    }

    public void push(HuffmanNode node) {
        if (top == capacity) {
            System.out.println("Stack is full! Cannot push.");
            return;
        }
        stack[top] = node;
        top++;
    }

    public HuffmanNode pop() {
        if (top == 0) {
            System.out.println("Stack is empty! Cannot pop.");
            return null;
        }

        top--;
        HuffmanNode node = stack[top];
        stack[top] = null;

        return node;
    }

    public HuffmanNode peek() {
        if (top == 0) {
            System.out.println("Stack is empty! Nothing to peek.");
            return null;
        }

        return stack[top - 1];
    }

    public boolean isEmpty() {
        return top == 0;
    }

    public int size() {
        return top;
    }
}