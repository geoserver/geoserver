/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.io.Serializable;
import org.geoserver.catalog.MetadataMap;

/**
 * Simple class storing the DirectDownload settings such as links creation capability enabled and
 * max download size.
 */
public class DirectDownloadSettings implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public static final String DIRECTDOWNLOAD_KEY = "DirectDownload.Key";

    boolean directDownloadEnabled = false;

    private long maxDownloadSize = 0;

    public DirectDownloadSettings(DirectDownloadSettings that) {
        this.directDownloadEnabled = that.directDownloadEnabled;
        this.maxDownloadSize = that.maxDownloadSize;
    }

    public DirectDownloadSettings() {}

    public boolean isDirectDownloadEnabled() {
        return directDownloadEnabled;
    }

    public void setDirectDownloadEnabled(boolean directDownloadEnabled) {
        this.directDownloadEnabled = directDownloadEnabled;
    }

    public long getMaxDownloadSize() {
        return maxDownloadSize;
    }

    /** Max download size in KB */
    public void setMaxDownloadSize(long maxDownloadSize) {
        this.maxDownloadSize = maxDownloadSize;
    }

    /**
     * Look for a {@link DirectDownloadSettings} instance in the provided metadataMap. If not
     * available, look for the default object stored in {@link CSWInfo} metadataMap as fallback (if
     * provided)
     */
    public static DirectDownloadSettings getSettingsFromMetadata(MetadataMap map, CSWInfo csw) {
        DirectDownloadSettings settings = null;
        if (map != null) {
            settings = (DirectDownloadSettings) map.get(DirectDownloadSettings.DIRECTDOWNLOAD_KEY);
        }
        if (settings == null && csw != null) {
            settings = getSettingsFromMetadata(csw.getMetadata(), null);
        }
        return settings;
    }
}
