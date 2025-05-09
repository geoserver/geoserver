/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;
import org.apache.commons.codec.binary.Base64;
import org.geoserver.util.IOUtils;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.xsd.EncoderDelegate;
import org.xml.sax.ContentHandler;

/**
 * Encodes objects as base64 binaries
 *
 * @author Andrea Aime - OpenGeo
 */
public class RawDataEncoderDelegate implements EncoderDelegate, JSONEncoderDelegate {

    private static final Function<byte[], String> BASE_64_ENCODER = buffer -> new String(Base64.encodeBase64(buffer));
    private static final Function<byte[], String> IDENTITY = buffer -> new String(buffer);
    private RawData rawData;

    public RawDataEncoderDelegate(RawData rawData) {
        this.rawData = rawData;
    }

    public RawData getRawData() {
        return rawData;
    }

    @Override
    public void encode(ContentHandler output) throws Exception {
        writeEncoded(chars -> output.characters(chars, 0, chars.length), BASE_64_ENCODER);
    }

    public void encode(OutputStream os) throws IOException {
        try (InputStream stream = rawData.getInputStream()) {
            IOUtils.copy(stream, os, WPSResourceManager.getCopyBufferSize());
        }
    }

    @Override
    public void encode(JsonGenerator generator) throws Exception {
        generator.writeRaw("\"");
        Function<byte[], String> encoder = BASE_64_ENCODER;
        if (rawData.getMimeType().equals("text/plain")) {
            encoder = IDENTITY;
        }
        writeEncoded(
                chars -> {
                    generator.writeRaw(chars, 0, chars.length);
                },
                encoder);
        generator.writeRaw("\"");
    }

    private interface Writer {
        void accept(char[] chars) throws Exception;
    }

    private void writeEncoded(Writer writer, Function<byte[], String> encoder) throws Exception {
        try (InputStream is = rawData.getInputStream()) {
            byte[] buffer = new byte[4096];
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                String encoded;
                if (read == 4096) {
                    encoded = encoder.apply(buffer);
                } else {
                    byte[] reducedBuffer = new byte[read];
                    System.arraycopy(buffer, 0, reducedBuffer, 0, read);
                    encoded = encoder.apply(reducedBuffer);
                }
                char[] chars = encoded.toCharArray();
                writer.accept(chars);
            }
        }
    }
}
