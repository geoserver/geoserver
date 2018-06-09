/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/** Enumeration for type of published object. */
public enum PublishedType {
    VECTOR {
        public Integer getCode() {
            return 0;
        }
    },
    RASTER {
        public Integer getCode() {
            return 1;
        }
    },
    REMOTE {
        public Integer getCode() {
            return 2;
        }
    },
    WMS {
        public Integer getCode() {
            return 3;
        }
    },
    GROUP {
        public Integer getCode() {
            return 4;
        }
    },
    WMTS {
        public Integer getCode() {
            return 5;
        }
    };

    public abstract Integer getCode();
}
