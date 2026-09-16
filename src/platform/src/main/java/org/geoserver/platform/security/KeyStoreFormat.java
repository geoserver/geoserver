/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.security;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;

/**
 * Keystore formats GeoServer names its keystore file after and recognizes on disk.
 *
 * <p>The file name says which format it should hold, and the first bytes say which one it does hold. GeoServer compares
 * the two before opening the file, so a keystore of the wrong format is reported instead of replaced.
 */
public enum KeyStoreFormat {
    JCEKS(0xCE, 0xCE, 0xCE, 0xCE),
    JKS(0xFE, 0xED, 0xFE, 0xED),
    BCFKS(0x30, 0x82);

    private final byte[] header;

    KeyStoreFormat(int... header) {
        this.header = new byte[header.length];
        for (int i = 0; i < header.length; i++) {
            this.header[i] = (byte) header[i];
        }
    }

    /** File name GeoServer uses for a keystore of the given type, for example {@code geoserver.bcfks}. */
    public static String fileName(String keyStoreType) {
        return "geoserver." + keyStoreType.toLowerCase(Locale.ROOT);
    }

    /** The format the file holds, or null for a missing file, a file too short to tell, or an unknown header. */
    public static KeyStoreFormat detect(Resource resource) throws IOException {
        if (resource.getType() == Type.UNDEFINED) {
            return null;
        }
        byte[] start = new byte[4];
        int read;
        try (InputStream in = resource.in()) {
            read = in.readNBytes(start, 0, start.length);
        }
        for (KeyStoreFormat format : values()) {
            if (format.matches(start, read)) {
                return format;
            }
        }
        return null;
    }

    private boolean matches(byte[] start, int read) {
        if (read < header.length) {
            return false;
        }
        for (int i = 0; i < header.length; i++) {
            if (start[i] != header[i]) {
                return false;
            }
        }
        return true;
    }
}
