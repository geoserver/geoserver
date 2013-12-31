/**
 *
 *  #%L
 *  geoserver-sync-core
 *  $Id:$
 *  $HeadURL:$
 *  %%
 *  Copyright (C) 2013 Moebius Solutions Inc.
 *  %%
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program.  If not, see
 *  <http://www.gnu.org/licenses/gpl-2.0.html>.
 *  #L%
 *
 */

package com.moesol.keys;



import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import junit.framework.TestCase;

import org.apache.commons.io.output.ByteArrayOutputStream;

//import com.skjegstad.utils.BloomFilter;

/**
 * @author hastings
 * Some testing to see what sizes we'd get by stringing together
 * comma separated uuid's and then compressing them...
 * We need this state transfer to allow any client to determine what
 * to delete.
 */
public class EncodeUidsTest extends TestCase {
	private double E = 19.0;
	private int N = 128;
	
	public void testCompress() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 10000; i++) {
			UUID id = UUID.randomUUID();
			sb.append(id);
			sb.append(',');
		}
		String result = sb.toString();
//		System.out.println("val=" + result);
		
		Deflater deflate = new Deflater();
		try {
			byte[] compressed = new byte[512000];
			deflate.setInput(result.getBytes());
			deflate.finish();
			System.out.printf("in=%d out=%d%n", deflate.getBytesRead(), deflate.getBytesWritten());
			deflate.deflate(compressed);
			System.out.printf("in=%d out=%d%n", deflate.getBytesRead(), deflate.getBytesWritten());
		} finally {
			deflate.end();
		}
	}
	
	public void testSha1Compression() throws NoSuchAlgorithmException, IOException {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStream out = makeCompressStream(baos);
		try {
			for (int i = 0; i < 128; i++) {
				Random r = new Random();
				byte[] bytes = new byte[20];
				r.nextBytes(bytes);
				byte[] sha1 = digest.digest(bytes);
				out.write(sha1);
			}
		} finally {
			out.close();
		}
		System.out.println("sha1 deflate: " + baos.toByteArray().length);
	}
	

	

	
	private OutputStream makeCompressStream(OutputStream out) throws IOException {
		return new DeflaterOutputStream(out);
//		return new GZIPOutputStream(out);
	}
}
