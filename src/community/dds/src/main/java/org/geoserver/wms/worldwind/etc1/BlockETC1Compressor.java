package org.geoserver.wms.worldwind.etc1;


import java.nio.ByteBuffer;

public class BlockETC1Compressor {
	// Copyright 2009 Google Inc.
	// 			 2011 Nicolas CASTEL
	//
	// Licensed under the Apache License, Version 2.0 (the "License");
	// you may not use this file except in compliance with the License.
	// You may obtain a copy of the License at
	//
	//	     http://www.apache.org/licenses/LICENSE-2.0
	//
	// Unless required by applicable law or agreed to in writing, software
	// distributed under the License is distributed on an "AS IS" BASIS,
	// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	// See the License for the specific language governing permissions and
	// limitations under the License.

	/* From http://www.khronos.org/registry/gles/extensions/OES/OES_compressed_ETC1_RGB8_texture.txt

	 The number of bits that represent a 4x4 texel block is 64 bits if
	 <internalformat> is given by ETC1_RGB8_OES.

	 The data for a block is a number of bytes,

	 {q0, q1, q2, q3, q4, q5, q6, q7}

	 where byte q0 is located at the lowest memory address and q7 at
	 the highest. The 64 bits specifying the block is then represented
	 by the following 64 bit integer:

	 int64bit = 256*(256*(256*(256*(256*(256*(256*q0+q1)+q2)+q3)+q4)+q5)+q6)+q7;

	 ETC1_RGB8_OES:

	 a) bit layout in bits 63 through 32 if diffbit = 0

	 63 62 61 60 59 58 57 56 55 54 53 52 51 50 49 48
	 -----------------------------------------------
	 | base col1 | base col2 | base col1 | base col2 |
	 | R1 (4bits)| R2 (4bits)| G1 (4bits)| G2 (4bits)|
	 -----------------------------------------------

	 47 46 45 44 43 42 41 40 39 38 37 36 35 34  33  32
	 ---------------------------------------------------
	 | base col1 | base col2 | table  | table  |diff|flip|
	 | B1 (4bits)| B2 (4bits)| cw 1   | cw 2   |bit |bit |
	 ---------------------------------------------------


	 b) bit layout in bits 63 through 32 if diffbit = 1

	 63 62 61 60 59 58 57 56 55 54 53 52 51 50 49 48
	 -----------------------------------------------
	 | base col1    | dcol 2 | base col1    | dcol 2 |
	 | R1' (5 bits) | dR2    | G1' (5 bits) | dG2    |
	 -----------------------------------------------

	 47 46 45 44 43 42 41 40 39 38 37 36 35 34  33  32
	 ---------------------------------------------------
	 | base col 1   | dcol 2 | table  | table  |diff|flip|
	 | B1' (5 bits) | dB2    | cw 1   | cw 2   |bit |bit |
	 ---------------------------------------------------


	 c) bit layout in bits 31 through 0 (in both cases)

	 31 30 29 28 27 26 25 24 23 22 21 20 19 18 17 16
	 -----------------------------------------------
	 |       most significant pixel index bits       |
	 | p| o| n| m| l| k| j| i| h| g| f| e| d| c| b| a|
	 -----------------------------------------------

	 15 14 13 12 11 10  9  8  7  6  5  4  3   2   1  0
	 --------------------------------------------------
	 |         least significant pixel index bits       |
	 | p| o| n| m| l| k| j| i| h| g| f| e| d| c | b | a |
	 --------------------------------------------------


	 Add table 3.17.2: Intensity modifier sets for ETC1 compressed textures:

	 table codeword                modifier table
	 ------------------        ----------------------
	 0                     -8  -2  2   8
	 1                    -17  -5  5  17
	 2                    -29  -9  9  29
	 3                    -42 -13 13  42
	 4                    -60 -18 18  60
	 5                    -80 -24 24  80
	 6                   -106 -33 33 106
	 7                   -183 -47 47 183


	 Add table 3.17.3 Mapping from pixel index values to modifier values for
	 ETC1 compressed textures:

	 pixel index value
	 ---------------
	 msb     lsb           resulting modifier value
	 -----   -----          -------------------------
	 1       1            -b (large negative value)
	 1       0            -a (small negative value)
	 0       0             a (small positive value)
	 0       1             b (large positive value)


	 */

	static final int kModifierTable[] = {
	/* 0 */2, 8, -2, -8,
	/* 1 */5, 17, -5, -17,
	/* 2 */9, 29, -9, -29,
	/* 3 */13, 42, -13, -42,
	/* 4 */18, 60, -18, -60,
	/* 5 */24, 80, -24, -80,
	/* 6 */33, 106, -33, -106,
	/* 7 */47, 183, -47, -183 };

	static final int kLookup[] = { 0, 1, 2, 3, -4, -3, -2, -1 };
	
	static short clamp(long x) {
	    return (short) (x >= 0 ? (x < 255 ? x : 255) : 0);
	}

	static
	short convert4To8(int b) {
		int c = b & 0xf;
	    return (short) ((c << 4) | c);
	}
	
	static
	short convert4To8(long b) {
		long c = b & 0xf;
	    return (short) ((c << 4) | c);
	}

	static
	short convert5To8(int b) {
		int c = b & 0x1f;
	    return (short) ((c << 3) | (c >> 2));
	}
	
	static
	short convert5To8(long b) {
		long c = b & 0x1f;
	    return (short) ((c << 3) | (c >> 2));
	}

	static
	short convert6To8(int b) {
		int c = b & 0x3f;
	    return (short) ((c << 2) | (c >> 4));
	}
	
	static
	short convert6To8(long b) {
		long c = b & 0x3f;
	    return (short) ((c << 2) | (c >> 4));
	}

	static
	int divideBy255(int d) {
	    return (d + 128 + (d >> 8)) >> 8;
	}

	static
	int convert8To4(int b) {
	    int c = b & 0xff;
	    return divideBy255(c * 15);
	}

	static
	int convert8To5(int b) {
	    int c = b & 0xff;
	    return divideBy255(c * 31);
	}

	static
	short convertDiff(long base, long diff) {
	    return convert5To8((int) ((0x1f & base) + kLookup[(int) (0x7 & diff)]));
	}

	static
	void decode_subblock(byte [] pOut, int r, int g, int b, int [] table, int tableindice,
	        long low, boolean second, boolean flipped) {
	    int baseX = 0;
	    int baseY = 0;
	    if (second) {
	        if (flipped) {
	            baseY = 2;
	        } else {
	            baseX = 2;
	        }
	    }
	    for (int i = 0; i < 8; i++) {
	        int x, y;
	        if (flipped) {
	            x = baseX + (i >> 1);
	            y = baseY + (i & 1);
	        } else {
	            x = baseX + (i >> 2);
	            y = baseY + (i & 3);
	        }
	        int k = y + (x * 4);
	        long offset = ((low >> k) & 1) | ((low >> (k + 15)) & 2);
	        long delta = table[(int) (tableindice+offset)];
	        
	        int q = 3 * (x + 4 * y);
	        pOut[++q] = (byte) clamp(r + delta);
	        pOut[++q] = (byte) clamp(g + delta);
	        pOut[++q] = (byte) clamp(b + delta);
	    }
	}

	// Input is an ETC1 compressed version of the data.
	// Output is a 4 x 4 square of 3-byte pixels in form R, G, B
	static void etc1_decode_block(ByteBuffer data, byte[]  pOut) {
	    long high = ((data.get() << 24) | (data.get() << 16) | (data.get() << 8) | data.get()) & 0xffffffff;
	    long low = (data.get() << 24) | (data.get() << 16) | (data.get() << 8) | data.get() & 0xffffffff;
	    int r1, r2, g1, g2, b1, b2;
	    if ((high & 2)>0) {
	        // differential
	    	long rBase = high >> 27;
	    	long gBase = high >> 19;
	    	long bBase = high >> 11;
	        r1 = convert5To8((int) rBase);
	        r2 = convertDiff(rBase, high >> 24);
	        g1 = convert5To8((int) gBase);
	        g2 = convertDiff(gBase, high >> 16);
	        b1 = convert5To8((int) bBase);
	        b2 = convertDiff(bBase, high >> 8);
	    } else {
	        // not differential
	        r1 = convert4To8((int) (high >> 28));
	        r2 = convert4To8((int) (high >> 24));
	        g1 = convert4To8((int) (high >> 20));
	        g2 = convert4To8((int) (high >> 16));
	        b1 = convert4To8((int) (high >> 12));
	        b2 = convert4To8((int) (high >> 8));
	    }
	    int tableIndexA = (int) (7 & (high >> 5));
	    int tableIndexB = (int) (7 & (high >> 2));
	    int tableA = tableIndexA * 4;
	    int tableB = tableIndexB * 4;
	    boolean flipped = (high & 1) != 0;
	    decode_subblock(pOut, r1, g1, b1, kModifierTable, tableA, low, false, flipped);
	    decode_subblock(pOut, r2, g2, b2, kModifierTable, tableB, low, true, flipped);
	}

	
	static class etc_compressed {
		public long high;
		public long low;
		public long score; // Lower is more accurate
	}

	static
	etc_compressed take_best(etc_compressed a, etc_compressed b) {
	    if (a.score > b.score) {
	        a = b;
	    }
	    return a;
	}

	static
	void etc_average_colors_subblock(byte[] pIn, int inMask,
			short[] pColors, int icolor, boolean flipped, boolean second) {
	    int r = 0;
	    int g = 0;
	    int b = 0;

	    if (flipped) {
	        int by = 0;
	        if (second) {
	            by = 2;
	        }
	        for (int y = 0; y < 2; y++) {
	            int yy = by + y;
	            for (int x = 0; x < 4; x++) {
	                int i = x + 4 * yy;
	                if ((inMask & (1 << i))>0) {
	                    int p = i * 3;
	                    r += pIn[p++] & 0xff;
	                    g += pIn[p++] & 0xff;
	                    b += pIn[p++] & 0xff;
	                }
	            }
	        }
	    } else {
	        int bx = 0;
	        if (second) {
	            bx = 2;
	        }
	        for (int y = 0; y < 4; y++) {
	            for (int x = 0; x < 2; x++) {
	                int xx = bx + x;
	                int i = xx + 4 * y;
	                if ((inMask & (1 << i))>0) {
	                	int p = i * 3;
	                	r += pIn[p++] & 0xff;
	                	g += pIn[p++] & 0xff;
	                	b += pIn[p++] & 0xff;
	                }
	            }
	        }
	    }
	    pColors[icolor] = (short)((r + 4) >> 3);
	    pColors[icolor+1] = (short)((g + 4) >> 3);
	    pColors[icolor+2] = (short)((b + 4) >> 3);
	}

	static
	long square(int x) {
	    return (long) x * (long) x;
	}

	static long chooseModifier(short[] pBaseColors, int icolor,
			byte[] pIn, int indice, etc_compressed pCompressed, long bitIndex,
			int[] pModifierTable, int iModifierTable) {
		long bestScore = Long.MAX_VALUE;
	    long bestIndex = 0;
	    int pixelR = pIn[indice] & 0xff;
	    int pixelG = pIn[indice+1] & 0xff;
	    int pixelB = pIn[indice+2] & 0xff;
	    int r = pBaseColors[icolor];
	    int g = pBaseColors[icolor+1];
	    int b = pBaseColors[icolor+2];
	    for (int i = 0; i < 4; i++) {
	        int modifier = pModifierTable[iModifierTable+i];
	        int decodedG = clamp(g + modifier);
	        long score = (6l * square(decodedG - pixelG));
	        if (score >= bestScore) {
	            continue;
	        }
	        int decodedR = clamp(r + modifier);
	        score += (3l * square(decodedR - pixelR));
	        if (score >= bestScore) {
	            continue;
	        }
	        int decodedB = clamp(b + modifier);
	        score += square(decodedB - pixelB);
	        if (score < bestScore) {
	            bestScore = score;
	            bestIndex = i;
	        }
	    }
	    long lowMask = (((bestIndex >> 1l) << 16l) | (bestIndex & 1l))
	            << bitIndex;
	    pCompressed.low |= lowMask;
	    return bestScore;
	}

	static
	void etc_encode_subblock_helper(byte[] pIn, long inMask,
	        etc_compressed pCompressed, boolean flipped, boolean second,
	        short[] pBaseColors, int icolor, int[] pModifierTable, int indice) {
	    long score = pCompressed.score;
	    if (flipped) {
	        int by = 0;
	        if (second) {
	            by = 2;
	        }
	        for (int y = 0; y < 2; y++) {
	            int yy = by + y;
	            for (int x = 0; x < 4; x++) {
	                long i = x + 4 * yy;
	                if ((inMask & (1l << i))>0) {
	                    score += chooseModifier(pBaseColors, icolor, pIn, (int) (i * 3),
	                            pCompressed, yy + x * 4, pModifierTable, indice);
	                }
	            }
	        }
	    } else {
	        int bx = 0;
	        if (second) {
	            bx = 2;
	        }
	        for (int y = 0; y < 4; y++) {
	            for (int x = 0; x < 2; x++) {
	                int xx = bx + x;
	                long i = xx + 4 * y;
	                if ((inMask & (1l << i))>0) {
	                    score += chooseModifier(pBaseColors, icolor, pIn, (int) (i * 3),
	                            pCompressed, y + xx * 4, pModifierTable, indice);
	                }
	            }
	        }
	    }
	    pCompressed.score = score;
	}

	static boolean inRange4bitSigned(int color) {
	    return color >= -4 && color <= 3;
	}
	
	static boolean inRange4bitSigned(long color) {
	    return color >= -4 && color <= 3;
	}

	static void etc_encodeBaseColors(short[] pBaseColors,
			short[] pColors, etc_compressed pCompressed) {
	    int r1 = 0, g1 = 0, b1 = 0, r2 = 0, g2 = 0, b2 = 0; 
	    	// 8 bit base colors for sub-blocks
	    boolean differential;
	    {
	        long r51 = convert8To5(pColors[0]);
	        long g51 = convert8To5(pColors[1]);
	        long b51 = convert8To5(pColors[2]);
	        long r52 = convert8To5(pColors[3]);
	        long g52 = convert8To5(pColors[4]);
	        long b52 = convert8To5(pColors[5]);

	        r1 = convert5To8(r51);
	        g1 = convert5To8(g51);
	        b1 = convert5To8(b51);

	        long dr = r52 - r51;
	        long dg = g52 - g51;
	        long db = b52 - b51;

	        differential = inRange4bitSigned(dr) && inRange4bitSigned(dg)
	                && inRange4bitSigned(db);
	        if (differential) {
	            r2 = convert5To8(r51 + dr);
	            g2 = convert5To8(g51 + dg);
	            b2 = convert5To8(b51 + db);
	            pCompressed.high |= (r51 << 27l) | ((7l & dr) << 24l) | (g51 << 19l)
	                    | ((7l & dg) << 16l) | (b51 << 11l) | ((7l & db) << 8l) | 2l;
	        }
	    }

	    if (!differential) {
	        long r41 = convert8To4(pColors[0]);
	        long g41 = convert8To4(pColors[1]);
	        long b41 = convert8To4(pColors[2]);
	        long r42 = convert8To4(pColors[3]);
	        long g42 = convert8To4(pColors[4]);
	        long b42 = convert8To4(pColors[5]);
	        r1 = convert4To8(r41);
	        g1 = convert4To8(g41);
	        b1 = convert4To8(b41);
	        r2 = convert4To8(r42);
	        g2 = convert4To8(g42);
	        b2 = convert4To8(b42);
	        pCompressed.high |= (r41 << 28l) | (r42 << 24l) | (g41 << 20l) | (g42
	                << 16l) | (b41 << 12l) | (b42 << 8l);
	    }
	    pBaseColors[0] = (short) r1;
	    pBaseColors[1] = (short) g1;
	    pBaseColors[2] = (short) b1;
	    pBaseColors[3] = (short) r2;
	    pBaseColors[4] = (short) g2;
	    pBaseColors[5] = (short) b2;
	}

	static
	etc_compressed etc_encode_block_helper(byte[] pIn, long inMask,
			short[] pColors, etc_compressed pCompressed, boolean flipped) {
	    pCompressed.score = Long.MAX_VALUE;
	    pCompressed.high = (flipped ? 1 : 0);
	    pCompressed.low = 0;

	    short [] pBaseColors = new short[6];

	    etc_encodeBaseColors(pBaseColors, pColors, pCompressed);

	    long originalHigh = pCompressed.high;

	    int pModifierTable = 0;
	    for (long i = 0; i < 8; i++, pModifierTable += 4) {
	        etc_compressed temp = new etc_compressed();
	        temp.score = 0;
	        temp.high = originalHigh | (i << 5);
	        temp.low = 0;
	        etc_encode_subblock_helper(pIn, inMask, temp, flipped, false,
	                pBaseColors, 0, kModifierTable, pModifierTable);
	        pCompressed = take_best(pCompressed, temp);
	    }
	    pModifierTable = 0;
	    etc_compressed firstHalf = pCompressed;
	    for (long i = 0; i < 8; i++, pModifierTable += 4) {
	        etc_compressed temp = new etc_compressed();
	        temp.score = firstHalf.score;
	        temp.high = firstHalf.high | (i << 2);
	        temp.low = firstHalf.low;
	        etc_encode_subblock_helper(pIn, inMask, temp, flipped, true,
	                pBaseColors, 3, kModifierTable, pModifierTable);
	        if (i == 0) {
	            pCompressed = temp;
	        } else {
	        	pCompressed = take_best(pCompressed,  temp);
	        }
	    }
	    return pCompressed;
	}

	static void writeBigEndian(byte[] pOut, int i, long d) {
	    pOut[i] = (byte) (d >> 24l);
	    pOut[i+1] = (byte)(d >> 16l);
	    pOut[i+2] = (byte)(d >> 8l);
	    pOut[i+3] = (byte) d;
	}

	/**
	 * Input is a 4 x 4 square of 3-byte pixels in form R, G, B
	 * inmask is a 16-bit mask where bit (1 << (x + y * 4)) tells whether the corresponding (x,y)
	 * pixel is valid or not. Invalid pixel color values are ignored when compressing.
	 * output is an ETC1 compressed version of the data.
	 */
	public static void encodeBlock(byte[] pIn, int inMask,
			byte[] pOut) {
		short[] colors = new short[6];
		short[] flippedColors = new short[6];
	    etc_average_colors_subblock(pIn, inMask, colors, 0, false, false);
	    etc_average_colors_subblock(pIn, inMask, colors, 3, false, true);
	    etc_average_colors_subblock(pIn, inMask, flippedColors, 0, true, false);
	    etc_average_colors_subblock(pIn, inMask, flippedColors, 3, true, true);

	    etc_compressed a = new etc_compressed(), b = new etc_compressed();
	    a = etc_encode_block_helper(pIn, inMask, colors, a, false);
	    b = etc_encode_block_helper(pIn, inMask, flippedColors, b, true);
	    a = take_best(a, b);
	    writeBigEndian(pOut, 0, a.high);
	    writeBigEndian(pOut, 4, a.low);
	}

	/**
	 * Return the size of the encoded image data (does not include size of PKM header).
	 */
	public static int getEncodedDataSize(int width, int height) {
	    return (((width + 3) & ~3) * ((height + 3) & ~3)) >> 1;
	}
	
	static boolean memcmp(ByteBuffer headerBuffer, byte [] b, int lenght) {
		for(int i = 0; i<lenght; i++) {
			if(headerBuffer.get(i)!=b[i]) {
				return true;
			}
		}
		return false;
	}

}
