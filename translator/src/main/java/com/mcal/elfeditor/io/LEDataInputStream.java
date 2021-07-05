/*
 * [The "BSD licence"]
 * Copyright (c) 2017 ZhaoHai
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * 二进制文件数据读取工具
 *
 * @author zhaohai
 * @time 2015/10/10
 */
package com.mcal.elfeditor.io;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

public class LEDataInputStream {
    public long size;
    /**
     * 缓冲字节数组区
     */
    protected byte[] work;
    /**
     * 二进制输入流
     */
    private DataInputStream dis;
    /**
     * 输入流
     */
    private InputStream is;
    private boolean mIsLittleEndian = true;

    public LEDataInputStream(byte[] data) throws IOException {
        this(new ByteArrayInputStream(data));
    }

    /**
     * 构造函数
     *
     * @throws IOException
     */
    public LEDataInputStream(InputStream in) throws IOException {
        // 获取输入流
        this.is = in;
        this.dis = new DataInputStream(is);
        work = new byte[8];
        size = in.available();
    }

    /**
     * 剩余未读取的数据大小
     */
    public int available() throws IOException {
        return is.available();
    }

    /**
     * 关闭流
     */
    public void close() throws IOException {
        dis.close();
        is.close();
    }

    public void mark(int readlimit) throws IOException {
        is.mark(readlimit);
    }

    /**
     * 读取一些数据，储存到buffer中
     *
     * @throws IOException
     */
    public int read(byte[] buffer, int start, int end) throws IOException {
        return dis.read(buffer, start, end);
    }

    /**
     * 读取一个字节
     *
     * @throws IOException
     */
    public byte readByte() throws IOException {
        return dis.readByte();
    }

    /**
     * 读取字节，储存到数组中，直到数组被填满
     *
     * @throws IOException
     */
    public void readFully(byte ba[]) throws IOException {
        dis.readFully(ba, 0, ba.length);
    }

    /**
     * 读取字节，储存到数组offset开始，len长度大小中，直到数组被填满
     *
     * @throws IOException
     */
    public void readFully(byte ba[], int off, int len) throws IOException {
        dis.readFully(ba, off, len);
    }

    /**
     * 读取一个32位的int型数据
     *
     * @throws IOException
     */
    public int readInt() throws IOException {
        if (mIsLittleEndian) {
            dis.readFully(work, 0, 4);
            return (work[3]) << 24 | (work[2] & 0xff) << 16 | (work[1] & 0xff) << 8 | (work[0] & 0xff);
        } else {
            return dis.readInt();
        }
    }

    /**
     * 读取32位int数据，并储存到数组中，直到数组填满
     *
     * @throws IOException
     */
    public int[] readIntArray(int length) throws IOException {
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = readInt();
        }
        return array;
    }

    /***
     * 读取一个64位长整型数据
     */
    public final long readLong() throws IOException {
        if (mIsLittleEndian) {
            dis.readFully(work, 0, 8);
            return (long) (work[7]) << 56 | (long) (work[6] & 0xff) << 48 | (long) (work[5] & 0xff) << 40
                    | (long) (work[4] & 0xff) << 32 | (long) (work[3] & 0xff) << 24 | (long) (work[2] & 0xff) << 16
                    | (long) (work[1] & 0xff) << 8 | work[0] & 0xff;
        } else {
            return dis.readLong();
        }
    }

    /**
     * 读取包名
     */
    public String readNulEndedString(int length, boolean fixed) throws IOException {
        StringBuilder string = new StringBuilder(16);
        while (length-- != 0) {
            short ch = readShort();
            if (ch == 0) {
                break;
            }
            string.append((char) ch);
        }
        if (fixed) {
            skipBytes(length * 2);
        }

        return string.toString();
    }

    /**
     * 读取一个16位的short型数据
     *
     * @throws IOException
     */
    public short readShort() throws IOException {
        if (mIsLittleEndian) {
            dis.readFully(work, 0, 2);
            return (short) ((work[1] & 0xff) << 8 | (work[0] & 0xff));
        } else {
            return dis.readShort();
        }
    }

    /***
     * 读取一个16位的无符号Short型数据
     */
    public int readUnsignedShort() throws IOException {
        return dis.readUnsignedShort();
    }

    public void reset() throws IOException {
        is.reset();
    }

    /**
     * 跳转
     *
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws IOException
     * @throws NoSuchFieldException
     */
    public void seek(long position) throws IOException {
        if (is instanceof ByteArrayInputStream) {
            Class<ByteArrayInputStream> clazz = ByteArrayInputStream.class;
            Field field;
            try {
                field = clazz.getDeclaredField("pos");
                field.setAccessible(true);
                field.setInt(is, (int) position);
            } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
                e.printStackTrace();
                throw new IOException("Unsupported");
            }
        } else {
            throw new IOException("Unsupported");
        }
    }

    public void setIsLittleEndian(boolean isLittleEndian) {
        mIsLittleEndian = isLittleEndian;
    }

    /**
     * 跳过1个字节
     *
     * @throws IOException
     */
    public void skipByte() throws IOException {
        skipBytes(1);
    }

    /**
     * 跳过n个字节
     *
     * @throws IOException
     */
    public void skipBytes(int n) throws IOException {
        dis.skipBytes(n);
    }

    public void skipCheckByte(byte expected) throws IOException {
        byte got = readByte();
        if (got != expected) {
            throw new IOException(String.format("CheckByte Expected: 0x%08x, got: 0x%08x", expected, got));
        }
    }

    public int skipCheckChunkTypeInt(int expected, int possible) throws IOException {
        int got = readInt();

        if (got == possible) {
            skipCheckChunkTypeInt(expected, -1);
        } else if (got != expected) {
            throw new IOException(String.format("CheckChunkTypeInt Expected: 0x%08x, got: 0x%08x", expected, got));
        }
        return got;
    }

    public void skipCheckInt(int expected) throws IOException {
        int got = readInt();
        if (got != expected) {
            throw new IOException(String.format("CheckInt Expected: 0x%08x, got: 0x%08x", expected, got));
        }
    }

    public void skipCheckShort(short expected) throws IOException {
        short got = readShort();
        if (got != expected) {
            throw new IOException(String.format("CheckShort Expected: 0x%08x, got: 0x%08x", expected, got));
        }
    }

    /**
     * 跳过一个32位的int型数据
     *
     * @throws IOException
     */
    public void skipInt() throws IOException {
        skipBytes(4);
    }

    /**
     * 跳过一个16位的short型数据
     *
     * @throws IOException
     */
    public void skipShort() throws IOException {
        skipBytes(2);
    }

}
