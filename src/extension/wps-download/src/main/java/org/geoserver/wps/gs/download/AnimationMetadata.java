/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.util.HTTPWarningAppender;

/**
 * Secondary result of the animation process, collects time related WMS warnings (default, failed
 * match, nearest match)
 */
public class AnimationMetadata {

    List<FrameWarning> warnings;
    boolean warningsFound;

    public AnimationMetadata() {
        this.warnings = new ArrayList<>();
    }

    public List<FrameWarning> getWarnings() {
        return warnings;
    }

    public boolean isWarningsFound() {
        return warningsFound;
    }

    public void setWarningsFound(boolean warningsFound) {
        this.warningsFound = warningsFound;
    }

    public void accumulateWarnings(int frameCounter) {
        HTTPWarningAppender.getWarnings()
                .stream()
                .map(w -> new FrameWarning(w, frameCounter))
                .forEach(fw -> warnings.add(fw));
    }
}
