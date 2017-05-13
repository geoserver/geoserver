/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.worldwind.etc1;


import java.nio.ByteBuffer;

public class BlockETC1Compressor {

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
