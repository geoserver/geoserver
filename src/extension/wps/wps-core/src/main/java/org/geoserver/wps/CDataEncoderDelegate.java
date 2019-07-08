/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import org.apache.commons.io.output.WriterOutputStream;
import org.geoserver.wps.ppio.CDataPPIO;
import org.geotools.xsd.EncoderDelegate;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 * Encodes objects as text sections within CDATA markers
 *
 * @author Andrea Aime - OpenGeo
 */
public class CDataEncoderDelegate implements EncoderDelegate {

    CDataPPIO ppio;

    Object object;

    public CDataEncoderDelegate(CDataPPIO ppio, Object object) {
        this.ppio = ppio;
        this.object = object;
    }

    public void encode(ContentHandler output) throws Exception {
        ((LexicalHandler) output).startCDATA();
        try (OutputStream os = new WriterOutputStream(new ContentHandlerWriter(output), "UTF-8")) {
            ppio.encode(object, os);
        }
        ((LexicalHandler) output).endCDATA();
    }

    public void encode(OutputStream os) throws Exception {
        ppio.encode(object, os);
    }

    static class ContentHandlerWriter extends Writer {

        ContentHandler ch;

        public ContentHandlerWriter(ContentHandler ch) {
            this.ch = ch;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            try {
                ch.characters(cbuf, off, len);
            } catch (SAXException e) {
                throw new IOException(e);
            }
        }

        @Override
        public void flush() throws IOException {
            // nothing to do

        }

        @Override
        public void close() throws IOException {
            // nothing to do

        }
    }
}
