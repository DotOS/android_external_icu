/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.i18n.timezone.internal;

/**
 * Iterates over big- or little-endian bytes on the native heap.
 * See {@link MemoryMappedFile#bigEndianIterator} and {@link MemoryMappedFile#littleEndianIterator}.
 *
 * @hide
 */
public final class NioBufferIterator extends BufferIterator {

    private final MemoryMappedFile file;
    private final long address;
    private final int length;
    private final boolean swap;

    private int position;

    NioBufferIterator(MemoryMappedFile file, long address, int length, boolean swap) {
        file.checkNotClosed();

        this.file = file;
        this.address = address;

        if (length < 0) {
            throw new IllegalArgumentException("length < 0");
        }
        final long MAX_VALID_ADDRESS = -1;
        if (Long.compareUnsigned(address, MAX_VALID_ADDRESS - length) > 0) {
            throw new IllegalArgumentException(
                    "length " + length + " would overflow 64-bit address space");
        }
        this.length = length;

        this.swap = swap;
    }

    @Override
    public void seek(int offset) {
        position = offset;
    }

    @Override
    public void skip(int byteCount) {
        position += byteCount;
    }

    @Override
    public int pos() {
        return position;
    }

    @Override
    public void readByteArray(byte[] bytes, int arrayOffset, int byteCount) {
        checkArrayBounds(arrayOffset, bytes.length, byteCount);
        file.checkNotClosed();
        checkReadBounds(position, length, byteCount);
        Memory.peekByteArray(address + position, bytes, arrayOffset, byteCount);
        position += byteCount;
    }

    @Override
    public byte readByte() {
        file.checkNotClosed();
        checkReadBounds(position, length, 1);
        byte result = Memory.peekByte(address + position);
        ++position;
        return result;
    }

    @Override
    public int readInt() {
        file.checkNotClosed();
        checkReadBounds(position, length, Integer.BYTES);
        int result = Memory.peekInt(address + position, swap);
        position += Integer.BYTES;
        return result;
    }

    @Override
    public void readIntArray(int[] ints, int arrayOffset, int intCount) {
        checkArrayBounds(arrayOffset, ints.length, intCount);
        file.checkNotClosed();
        final int byteCount = Integer.BYTES * intCount;
        checkReadBounds(position, length, byteCount);
        Memory.peekIntArray(address + position, ints, arrayOffset, intCount, swap);
        position += byteCount;
    }

    @Override
    public void readLongArray(long[] longs, int arrayOffset, int longCount) {
        checkArrayBounds(arrayOffset, longs.length, longCount);
        file.checkNotClosed();
        final int byteCount = Long.BYTES * longCount;
        checkReadBounds(position, length, byteCount);
        Memory.peekLongArray(address + position, longs, arrayOffset, longCount, swap);
        position += byteCount;
    }

    @Override
    public short readShort() {
        file.checkNotClosed();
        checkReadBounds(position, length, Short.BYTES);
        short result = Memory.peekShort(address + position, swap);
        position += Short.BYTES;
        return result;
    }

    private static void checkReadBounds(int position, int length, int byteCount) {
        if (position < 0 || byteCount < 0) {
            throw new IndexOutOfBoundsException(
                    "Invalid read args: position=" + position + ", byteCount=" + byteCount);
        }
        // Use of int here relies on length being an int <= Integer.MAX_VALUE.
        final int finalReadPos = position + byteCount;
        if (finalReadPos < 0 || finalReadPos > length) {
            throw new IndexOutOfBoundsException(
                    "Read outside range: position=" + position + ", byteCount=" + byteCount
                            + ", length=" + length);
        }
    }

    private static void checkArrayBounds(int arrayOffset, int arrayLength, int count) {
        if (arrayOffset < 0 || count < 0) {
            throw new IndexOutOfBoundsException(
                    "Invalid args: arrayOffset=" + arrayOffset + ", count=" + count);
        }
        // Use of int here relies on arrayLength being an int <= Integer.MAX_VALUE, which it has to
        // be because it's an array length.
        final int targetPos = arrayOffset + count;
        if (targetPos < 0 || targetPos > arrayLength) {
            throw new IndexOutOfBoundsException(
                    "Write outside range: arrayLength=" + arrayLength + ", arrayOffset="
                            + arrayOffset + ", count=" + count);
        }
    }
}
