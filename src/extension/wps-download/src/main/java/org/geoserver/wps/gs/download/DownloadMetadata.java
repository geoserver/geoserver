/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.util.DimensionWarning;
import org.geoserver.util.HTTPWarningAppender;

/**
 * Secondary result of the map process, collects time related WMS warnings (default, failed match,
 * nearest match)
 */
public class DownloadMetadata {

    List<DimensionWarning> warnings;
    boolean warningsFound;

    public DownloadMetadata() {
        this.warnings = new ArrayList<>();
    }

    public List<DimensionWarning> getWarnings() {
        return warnings;
    }

    public boolean isWarningsFound() {
        return warningsFound;
    }

    public void setWarningsFound(boolean warningsFound) {
        this.warningsFound = warningsFound;
    }

    public void accumulateWarnings() {
        warnings.addAll(HTTPWarningAppender.getWarnings());
    }
}
