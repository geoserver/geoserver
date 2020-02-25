/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.mosaic;

import java.util.Date;
import org.geoserver.importer.Dates;

/**
 * Enumeration for handling timestamps for granules.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public enum TimeMode {
    /** Extract the timestamp from the filename, via {@link FilenameTimeHandler} */
    FILENAME,
    MANUAL,
    AUTO,
    NONE;

    public TimeHandler createHandler() {
        if (this == FILENAME) {
            return new FilenameTimeHandler();
        }

        return new TimeHandler() {

            private static final long serialVersionUID = 1L;

            @Override
            public Date computeTimestamp(Granule g) {
                switch (TimeMode.this) {
                    case AUTO:
                        return Dates.matchAndParse(g.getFile().getName());
                    case MANUAL:
                        return g.getTimestamp();
                }
                return null;
            }
        };
    }
}
