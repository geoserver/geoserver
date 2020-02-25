/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

public enum ProjectionPolicy {

    /** Force the declared projection, ignoring the native one. */
    FORCE_DECLARED {
        public Integer getCode() {
            return 0;
        }
    },

    /** Reproject from the native projection to the declared one. */
    REPROJECT_TO_DECLARED {
        public Integer getCode() {
            return 1;
        }
    },
    /** Do nothing. */
    NONE {
        public Integer getCode() {
            return 2;
        }
    };

    public abstract Integer getCode();

    /**
     * Looks up the projection policy by code, if the lookup fails the default, {@link
     * #FORCE_DECLARED}, is returned.
     */
    public static ProjectionPolicy get(Integer code) {
        if (code == null) return FORCE_DECLARED;

        for (ProjectionPolicy p : ProjectionPolicy.values()) {
            if (code.equals(p.getCode())) {
                return p;
            }
        }

        return FORCE_DECLARED;
    }
}
