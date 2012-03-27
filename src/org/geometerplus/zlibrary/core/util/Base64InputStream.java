/*
 * Copyright (C) 2007-2012 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.zlibrary.core.util;

import java.io.IOException;
import java.io.InputStream;

public class Base64InputStream extends InputStream {
	private final InputStream myBaseStream;

	private final byte[] myBuffer = new byte[32768];
	private int myBufferOffset;
	private int myBufferLength;
	
	public Base64InputStream(InputStream stream) {
		myBaseStream = stream;
	}
	
	@Override
	public int available() throws IOException {
		// TODO: real value might be less than returned one
		return (myBufferLength + myBaseStream.available()) / 2;
	}

	@Override
	public long skip(long n) throws IOException {
		int offset = myBufferOffset;
		int available = myBufferLength;
		byte first = 0;
		for (long skipped = 0; skipped < 2 * n;) {
			while (skipped < 2 * n && available-- > 0) {
				if (decode(myBuffer[offset++]) != -1) {
					++skipped;
				}
			}
			if (skipped < 2 * n) {
				fillBuffer();
				available = myBufferLength;
				if (available == -1) {
					return skipped / 2;
				}
				offset = 0;
			}
		}
		myBufferLength = available;
		myBufferOffset = offset;
		return n;
	}

	@Override
	public int read() throws IOException {
		int first = -1;
		while (myBufferLength >= 0) {
			while (myBufferLength-- > 0) {
				final int digit = decode(myBuffer[myBufferOffset++]);
				if (digit != -1) {
					if (first == -1) {
						first = digit;
					} else {
						return (first << 4) + digit;
					}
				}
			}
			fillBuffer();
		}
		return -1;
	}

	@Override
	public void close() throws IOException {
		myBaseStream.close();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int offset = myBufferOffset;
		int available = myBufferLength;
		int first = -1;
		for (int ready = 0; ready < len;) {
			while (ready < len && available-- > 0) {
				final int digit = decode(myBuffer[offset++]);
				if (digit != -1) {
					if (first == -1) {
						first = digit;
					} else {
						b[off + ready++] = (byte)((first << 4) + digit);
						first = -1;
					}
				}
			}
			if (ready < len) {
				fillBuffer();
				available = myBufferLength;
				if (available == -1) {
					return ready == 0 ? -1 : ready;
				}
				offset = 0;
			}
		}
		myBufferLength = available;
		myBufferOffset = offset;
		return len;
	}

	@Override
	public void reset() throws IOException {
		myBaseStream.reset();
		myBufferOffset = 0;
		myBufferLength = 0;
	}

	private void fillBuffer() throws IOException {
		myBufferLength = myBaseStream.read(myBuffer);
		myBufferOffset = 0;
	}

	private static int decode(byte b) {
		switch (b) {
			default:
				return -1;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				return b - '0';
			case 'a':
			case 'A':
				return 10;
			case 'b':
			case 'B':
				return 11;
			case 'c':
			case 'C':
				return 12;
			case 'd':
			case 'D':
				return 13;
			case 'e':
			case 'E':
				return 14;
			case 'f':
			case 'F':
				return 15;
		}
	}
}
