/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * Configuration for brute force attack preventer
 *
 * @author Andrea Aime - GeoSolutions
 */
public class BruteForcePreventionConfig implements SecurityConfig {

    static final Logger LOGGER = Logging.getLogger(BruteForcePreventionConfig.class);

    private static final long serialVersionUID = 5774047555637121124L;

    /** Default brute force attack configuration */
    public static final BruteForcePreventionConfig DEFAULT = new BruteForcePreventionConfig();

    boolean enabled;

    int minDelaySeconds;

    int maxDelaySeconds;

    int maxBlockedThreads;

    List<String> whitelistedMasks;

    transient List<IpAddressMatcher> whitelistedAddressMatchers;

    /** Configuration based on defaults */
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public BruteForcePreventionConfig() {
        this.enabled = true;
        this.minDelaySeconds = 1;
        this.maxDelaySeconds = 5;
        this.whitelistedMasks = new ArrayList<>();
        this.whitelistedMasks.add("127.0.0.1");
        this.maxBlockedThreads = 100;
    }

    public BruteForcePreventionConfig(BruteForcePreventionConfig other) {
        this.enabled = other.enabled;
        this.minDelaySeconds = other.minDelaySeconds;
        this.maxDelaySeconds = other.maxDelaySeconds;
        this.whitelistedMasks =
                other.whitelistedMasks != null ? new ArrayList<>(other.whitelistedMasks) : null;
        this.maxBlockedThreads = other.maxBlockedThreads;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMinDelaySeconds() {
        return minDelaySeconds;
    }

    public void setMinDelaySeconds(int minConfig) {
        this.minDelaySeconds = minConfig;
    }

    public int getMaxDelaySeconds() {
        return maxDelaySeconds;
    }

    public void setMaxDelaySeconds(int maxConfig) {
        this.maxDelaySeconds = maxConfig;
    }

    public List<String> getWhitelistedMasks() {
        return whitelistedMasks;
    }

    public void setWhitelistedMasks(List<String> whitelistedMasks) {
        this.whitelistedMasks = whitelistedMasks;
        if (whitelistedMasks == null) {
            this.whitelistedAddressMatchers = null;
        }
    }

    public List<IpAddressMatcher> getWhitelistAddressMatchers() {
        try {
            if (this.getWhitelistedMasks() != null && this.whitelistedAddressMatchers == null) {
                this.whitelistedAddressMatchers =
                        whitelistedMasks
                                .stream()
                                .map(mask -> new IpAddressMatcher(mask))
                                .collect(Collectors.toList());
            }
        } catch (Exception e) {
            // an error here and no request can be made, best be cautious (yes, it actually
            // happened to me)
            LOGGER.log(Level.SEVERE, "Invalid netmask configuration, will skip it", e);
        }
        return this.whitelistedAddressMatchers;
    }

    public int getMaxBlockedThreads() {
        return maxBlockedThreads;
    }

    public void setMaxBlockedThreads(int maxBlockedThreads) {
        this.maxBlockedThreads = maxBlockedThreads;
    }

    @Override
    public SecurityConfig clone(boolean allowEnvParametrization) {
        BruteForcePreventionConfig clone = new BruteForcePreventionConfig(this);

        // allow parametrization of the whitelisted masks
        final GeoServerEnvironment gsEnvironment =
                GeoServerExtensions.bean(GeoServerEnvironment.class);
        if (clone != null) {
            if (allowEnvParametrization
                    && gsEnvironment != null
                    && GeoServerEnvironment.ALLOW_ENV_PARAMETRIZATION) {
                List<String> resolvedMasks = new ArrayList<>();
                for (String mask : whitelistedMasks) {
                    String resolved = (String) gsEnvironment.resolveValue(mask);
                    if (resolved != null) {
                        Arrays.stream(resolved.split("\\s*,\\s*"))
                                .filter(s -> s != null && !s.trim().isEmpty())
                                .forEach(s -> resolvedMasks.add(s));
                    }
                }
                clone.setWhitelistedMasks(resolvedMasks);
            }
        }

        return clone;
    }
}
