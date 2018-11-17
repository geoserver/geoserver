/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.apache.commons.codec.binary.Base64;
import org.geoserver.wps.ppio.BinaryPPIO;
import org.geotools.xsd.EncoderDelegate;
import org.xml.sax.ContentHandler;

/**
 * Encodes objects as base64 binaries
 *
 * @author Andrea Aime - OpenGeo
 */
public class BinaryEncoderDelegate implements EncoderDelegate {

    BinaryPPIO ppio;

    Object object;

    public BinaryEncoderDelegate(BinaryPPIO ppio, Object object) {
        this.ppio = ppio;
        this.object = object;
    }

    public void encode(ContentHandler output) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ppio.encode(object, bos);

        char[] chars = new String(Base64.encodeBase64(bos.toByteArray())).toCharArray();
        output.characters(chars, 0, chars.length);
    }

    public void encode(OutputStream os) throws Exception {
        ppio.encode(object, os);
    }
}
