/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pngwind.config;

import java.util.Collections;
import java.util.Set;

/**
 * Configuration for matching bands to wind speed, direction, and components based on their names. Each type (speed,
 * direction, u, v) can be configured with a set of exact matches and a set of substring matches for containment check.
 * The matching logic will first check for exact matches, and if none are found, it will check if any of the configured
 * substrings are contained in the band name.
 *
 * <p>Wind layers are usually made of 2 bands. The PngWind format tries to guess which band is which based on the name
 * of the bands. This class encapsulates the configuration for that matching process. In the event that the matching
 * process fails, the original bands are returned as-is, with a warning logged, assuming they are already in U/V form in
 * the order of the bands.
 */
public final class BandMatchingConfig {

    private final BandTypeMatcher speed;
    private final BandTypeMatcher direction;
    private final BandTypeMatcher u;
    private final BandTypeMatcher v;

    public BandMatchingConfig(BandTypeMatcher speed, BandTypeMatcher direction, BandTypeMatcher u, BandTypeMatcher v) {
        this.speed = speed;
        this.direction = direction;
        this.u = u;
        this.v = v;
    }

    public BandTypeMatcher getSpeed() {
        return speed;
    }

    public BandTypeMatcher getDirection() {
        return direction;
    }

    public BandTypeMatcher getU() {
        return u;
    }

    public BandTypeMatcher getV() {
        return v;
    }

    public static final class BandTypeMatcher {
        private final Set<String> exact;
        private final Set<String> contains;

        public BandTypeMatcher(Set<String> exact, Set<String> contains) {
            this.exact = Collections.unmodifiableSet(exact);
            this.contains = Collections.unmodifiableSet(contains);
        }

        public boolean matches(String name) {
            if (exact.contains(name)) {
                return true;
            }
            for (String token : contains) {
                if (name.contains(token)) {
                    return true;
                }
            }
            return false;
        }
    }
}
