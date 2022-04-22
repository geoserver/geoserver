/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.libdeflate;

import it.geosolutions.imageio.compression.libdeflate.LibDeflateCompressorSpi;
import java.io.Serializable;

/** Basic Libdeflate Plugin Settings */
public class LibdeflateSettings implements Serializable {

    public LibdeflateSettings() {}

    public LibdeflateSettings(LibdeflateSettings settings) {
        this.compressionPriority = settings.compressionPriority;
        this.decompressionPriority = settings.decompressionPriority;
        this.minLevel = settings.minLevel;
        this.maxLevel = settings.maxLevel;
    }

    public static final String LIBDEFLATE_SETTINGS_KEY = "LibdeflateSettings.Key";

    protected int compressionPriority = LibDeflateCompressorSpi.getDefaultPriority();

    protected int decompressionPriority = LibDeflateCompressorSpi.getDefaultPriority();

    protected int minLevel = LibDeflateCompressorSpi.getDefaultMinLevel();

    protected int maxLevel = LibDeflateCompressorSpi.getDefaultMaxLevel();

    public int getCompressionPriority() {
        return compressionPriority;
    }

    public void setCompressionPriority(int compressionPriority) {
        this.compressionPriority = compressionPriority;
    }

    public int getDecompressionPriority() {
        return decompressionPriority;
    }

    public void setDecompressionPriority(int decompressionPriority) {
        this.decompressionPriority = decompressionPriority;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public void setMinLevel(int minLevel) {
        this.minLevel = minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }
}
