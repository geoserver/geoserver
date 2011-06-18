/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.filters;

import org.geoserver.test.GeoServerTestSupport;

/**
 * Wrap a String up as a ServletInputStream so we can read it multiple times.
 * @author David Winslow <dwinslow@openplans.org>
 */
public class BufferedRequestStreamTest extends GeoServerTestSupport {
    BufferedRequestStream myBRS;
	String myTestString;

	@Override
    public void setUpInternal() throws Exception{
		super.setUpInternal();
		myTestString = "Hello, this is a test";
		myBRS = new BufferedRequestStream(myTestString);
	}

    public void testReadLine() throws Exception{
		byte[] b = new byte[1024];
		int off = 0;
		int len = 1024;
		int amountRead = myBRS.readLine(b, off, len);
		String s = new String(b, 0, amountRead);
		assertEquals(s, myTestString);;
    }
}
