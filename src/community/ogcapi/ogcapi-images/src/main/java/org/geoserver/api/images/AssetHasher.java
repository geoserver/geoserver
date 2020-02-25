/**
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * <p>Copyright 2019
 */
package org.geoserver.api.images;

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
