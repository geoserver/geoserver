/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.images;

import java.io.File;
import java.security.MessageDigest;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

/**
 * Manages creation and "decoding" of the file URLs so that the actual file system path is hidden
 */
@Component
class AssetHasher {

    public boolean matches(File file, String hash) {
        String actualHash = hashFile(file);
        return actualHash.equals(hash);
    }

    /**
     * Return a SHA-1 based hash for the specified file, by appending the file's base name to the
     * hashed full path. This allows to hide the underlying file system structure.
     */
    public String hashFile(File file) {
        try {
            String canonicalPath = file.getCanonicalPath();
            String mainFilePath = FilenameUtils.getPath(canonicalPath);

            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(mainFilePath.getBytes());
            return Hex.encodeHexString(md.digest()) + "-" + file.getName();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
