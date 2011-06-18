/** 
 * Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Arne Kepp / OpenGeo
 */
package org.geoserver.gwc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;

class FakeServletOutputStream extends ServletOutputStream {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(20480);

    public void write(int b) throws IOException {
        outputStream.write(b);
    }

    public byte[] getBytes() {
        return outputStream.toByteArray();
    }
}
