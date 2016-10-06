/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NetCDFSettingsContainer implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public enum Version {
        NETCDF_3, NETCDF_4C;
    }

    public static final String NETCDFOUT_KEY = "NetCDFOutput.Key";

    public static final int DEFAULT_COMPRESSION = 0;

    public static final boolean DEFAULT_SHUFFLE = true;

    public static final Version DEFAULT_VERSION = Version.NETCDF_3;

    public static final List<GlobalAttribute> DEFAULT_ATTRIBUTES = new ArrayList<GlobalAttribute>();

    private int compressionLevel = DEFAULT_COMPRESSION;

    private boolean shuffle = DEFAULT_SHUFFLE;

    private DataPacking dataPacking = DataPacking.getDefault();

    private List<GlobalAttribute> globalAttributes = DEFAULT_ATTRIBUTES;

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    public DataPacking getDataPacking() {
        return dataPacking;
    }

    public void setDataPacking(DataPacking dataPacking) {
        this.dataPacking = dataPacking;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public List<GlobalAttribute> getGlobalAttributes() {
        if (globalAttributes == null) {
            globalAttributes = DEFAULT_ATTRIBUTES;
        }
        return globalAttributes;
    }

    public void setGlobalAttributes(List<GlobalAttribute> globalAttributes) {
        this.globalAttributes = globalAttributes;
    }

    public static class GlobalAttribute implements Serializable {
        private static final long serialVersionUID = 1L;

        private String key;

        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public GlobalAttribute(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
