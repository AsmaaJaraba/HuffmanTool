# ⚡ HUFFMAN_TOOL

A lossless file compression and decompression tool built using the
Huffman coding algorithm.

Built for **COM336 — Design and Analysis of Algorithms**
Birzeit University — Second Semester 2025/2026

---

## What It Does

- Compresses any file type using Huffman coding
- Decompresses `.huff` files back to their original format
- Restored files are byte-for-byte identical to the original
- Displays encoding table, compression statistics, and header bits
- Right-click compress/decompress via macOS Automator Quick Action

---

## How It Works

Input File → Count Byte Frequencies → Build Huffman Tree
→ Generate Codes → Write Header + Encoded Data
→ Output .huff File

.huff File → Read Header → Reconstruct Tree
→ Decode Bits → Output Original File


---

## Data Structures (Built From Scratch)

| Class | Description |
|-------|-------------|
| `MinHeap` | Array-based priority queue — always returns lowest frequency node |
| `CustomStack` | Array-based LIFO stack — used for tree reconstruction |
| `HuffmanTree` | Builds the optimal prefix-free Huffman tree |
| `BitWriter` | Packs individual bits into bytes and writes to file |
| `BitReader` | Reads bytes from file and serves individual bits |

---

## Compressed File Structure (.huff)

[ Extension Length — 8 bits ]
[ Extension Chars — 8 bits each ]
[ Tree Bit Length — 32 bits ]
[ Huffman Tree — variable bits ]
[ Padding Count — 8 bits ]
[ Compressed Data — variable bits ]


---

## Features

- **Main Interface** — Full compression/decompression with stats
- **Encoding Table** — Shows every byte, frequency, code, and length
- **Header Display** — Shows every bit written to the compressed file
- **Quick Mode** — Right-click any file in Finder to compress/decompress
- **Any File Type** — Works on text, images, PDFs, binaries, and more

---

## How to Run

1. Open the project in **IntelliJ IDEA**
2. Make sure **Java 21** and **JavaFX 21** are configured
3. Run `HelloApplication.java`

---

## Tech Stack

- Java 21
- JavaFX 21
- Maven

