/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.config.ServiceInfo;
import org.geotools.util.Version;

public interface WFSInfo extends ServiceInfo {

    static enum Version {
        V_10("1.0.0"),
        V_11("1.1.0"),
        V_20("2.0.0");

        org.geotools.util.Version version;

        Version(String ver) {
            this.version = new org.geotools.util.Version(ver);
        }

        public org.geotools.util.Version getVersion() {
            return version;
        }

        static Version get(String v) {
            if (v.startsWith("1.0")) {
                return V_10;
            }
            if (v.startsWith("1.1")) {
                return V_11;
            }
            if (v.startsWith("2.0")) {
                return V_20;
            }
            return null;
        }

        public static Version negotiate(String ver) {
            if (ver == null) {
                return null;
            }

            org.geotools.util.Version version = new org.geotools.util.Version(ver);
            if (version.compareTo(V_10.version) <= 0) {
                return V_10;
            }
            if (version.compareTo(V_11.version) <= 0) {
                return V_11;
            }
            return V_20;
        }

        /** Compares this value with a given version as string */
        public int compareTo(String version) {
            if (version == null) {
                return (this == latest()) ? 0 : -1;
            }
            return this.version.compareTo(new org.geotools.util.Version(version));
        }

        public static Version latest() {
            return V_20;
        }
    };

    static enum Operation {
        GETCAPABILITIES {
            @Override
            public int getCode() {
                return 0;
            }
        },
        DESCRIBEFEATURETYPE {
            @Override
            public int getCode() {
                return 0;
            }
        },
        GETFEATURE {
            @Override
            public int getCode() {
                return 1;
            }
        },
        LOCKFEATURE {
            @Override
            public int getCode() {
                return 2;
            }
        },
        TRANSACTION_INSERT {
            @Override
            public int getCode() {
                return 4;
            }
        },
        TRANSACTION_UPDATE {
            @Override
            public int getCode() {
                return 8;
            }
        },
        TRANSACTION_DELETE {
            @Override
            public int getCode() {
                return 16;
            }
        },
        TRANSACTION_REPLACE {
            @Override
            public int getCode() {
                return 32;
            }
        };

        public abstract int getCode();
    }

    static enum ServiceLevel {
        BASIC {
            @Override
            public int getCode() {
                return 1;
            }

            @Override
            public List<Operation> getOps() {
                return Arrays.asList(
                        Operation.GETCAPABILITIES,
                        Operation.DESCRIBEFEATURETYPE,
                        Operation.GETFEATURE);
            }
        },
        TRANSACTIONAL {
            @Override
            public int getCode() {
                return 15;
            }

            @Override
            public List<Operation> getOps() {
                return Arrays.asList(
                        Operation.GETCAPABILITIES,
                        Operation.DESCRIBEFEATURETYPE,
                        Operation.GETFEATURE,
                        Operation.TRANSACTION_INSERT,
                        Operation.TRANSACTION_UPDATE,
                        Operation.TRANSACTION_DELETE,
                        Operation.TRANSACTION_REPLACE);
            }
        },
        COMPLETE {
            @Override
            public int getCode() {
                return 31;
            }

            @Override
            public List<Operation> getOps() {
                return Arrays.asList(
                        Operation.GETCAPABILITIES, Operation.DESCRIBEFEATURETYPE,
                        Operation.GETFEATURE, Operation.TRANSACTION_INSERT,
                        Operation.TRANSACTION_UPDATE, Operation.TRANSACTION_DELETE,
                        Operation.TRANSACTION_REPLACE, Operation.LOCKFEATURE);
            }
        };

        public abstract int getCode();

        public abstract List<Operation> getOps();

        boolean contains(ServiceLevel other) {
            return getOps().containsAll(other.getOps());
        }

        public static ServiceLevel get(int code) {
            for (ServiceLevel s : values()) {
                if (s.getCode() == code) {
                    return s;
                }
            }

            return null;
        }
    };

    /** A map of wfs version to gml encoding configuration. */
    Map<Version, GMLInfo> getGML();

    /**
     * A global cap on the number of features to allow when processing a request.
     *
     * @uml.property name="maxFeatures"
     */
    int getMaxFeatures();

    /**
     * Sets the global cap on the number of features to allow when processing a request.
     *
     * @uml.property name="maxFeatures"
     */
    void setMaxFeatures(int maxFeatures);

    /** The level of service provided by the WFS. */
    ServiceLevel getServiceLevel();

    /** Sets the level of service provided by the WFS. */
    void setServiceLevel(ServiceLevel serviceLevel);

    /**
     * The flag which determines if gml:bounds elements should be encoded at the feature level in
     * gml output.
     */
    boolean isFeatureBounding();

    /**
     * Sets the flag which determines if gml:bounds elements should be encoded at the feature level
     * in gml output.
     */
    void setFeatureBounding(boolean featureBounding);

    /**
     * Get the flag that determines the encoding of the WFS schemaLocation. True if the WFS
     * schemaLocation should refer to the canonical location, false if the WFS schemaLocation should
     * refer to a copy served by GeoServer.
     */
    boolean isCanonicalSchemaLocation();

    /**
     * Set the flag that determines the encoding of the WFS schemaLocation. True if the WFS
     * schemaLocation should refer to the canonical location, false if the WFS schemaLocation should
     * refer to a copy served by GeoServer.
     */
    void setCanonicalSchemaLocation(boolean canonicalSchemaLocation);

    /**
     * Get the flag that determines encoding of featureMember or featureMembers True if the
     * featureMember should be encoded False if the featureMembers should be encoded
     *
     * @return encodingFeatureMember
     */
    boolean isEncodeFeatureMember();

    /** set the response encoding option, featureMembers or featureMember */
    void setEncodeFeatureMember(boolean encodeFeatureMember);

    /**
     * Get the flag that determines if WFS hit requests (counts) will ignore the maximum features
     * limit for this server
     *
     * @return hitsIgnoreMaxFeatures
     */
    boolean isHitsIgnoreMaxFeatures();

    /** Set the option to ignore the maximum feature limit for WFS hit counts */
    void setHitsIgnoreMaxFeatures(boolean hitsIgnoreMaxFeatures);

    /**
     * Get the maximum number of features to be displayed in a layer preview. Can be defined by the
     * user. By default, 50.
     *
     * @return maxNumberOfFeaturesForPreview
     */
    Integer getMaxNumberOfFeaturesForPreview();

    /** Set the maximum number of features to be displayed in a layer preview */
    void setMaxNumberOfFeaturesForPreview(Integer maxNumberOfFeaturesForPreview);

    /** The srs's that the WFS service will advertise in the capabilities document */
    List<String> getSRS();

    /** Flag that determines if global stored queries are allowed. Default true. */
    Boolean getAllowGlobalQueries();

    void setAllowGlobalQueries(Boolean allowGlobalQueries);

    /**
     * Flag that determines if complex features will be converted to simple feature for compatible
     * output formats.
     */
    boolean isSimpleConversionEnabled();

    /**
     * Sets the flag that determines if complex features will be converted to simple feature for
     * compatible output formats.
     */
    void setSimpleConversionEnabled(boolean simpleConversionEnabled);
    /**
     * Flag that determines if the wfsRequest.txt dump file should be included in shapefile/zip
     * output.
     */
    boolean getIncludeWFSRequestDumpFile();
    /**
     * Sets the flag that determines if the wfsRequest.txt dump file should be included in
     * shapefile/zip output
     */
    void setIncludeWFSRequestDumpFile(boolean includeWFSRequestDumpFile);

    /**
     * Flag that determines if Output Type checking is enforced
     *
     * @return whether checking is enforced
     */
    boolean isGetFeatureOutputTypeCheckingEnabled();

    /**
     * Sets the flag that determines if Output Type checking is enforced
     *
     * @param getFeatureOutputTypeCheckingEnabled whether checking is enforced
     */
    void setGetFeatureOutputTypeCheckingEnabled(boolean getFeatureOutputTypeCheckingEnabled);

    /**
     * Set of Output Types allowed for GetFeature and GetFeatureWithLock
     *
     * @return set of Output Types
     */
    Set<String> getGetFeatureOutputTypes();

    /**
     * Set the Set of Output Types allowed for GetFeature and GetFeatureWithLock
     *
     * @param getFeatureOutputTypes set of Output Types to be enforced
     */
    void setGetFeatureOutputTypes(Set<String> getFeatureOutputTypes);

    /**
     * Get the Date Format for csv
     *
     * @return Csv Date Format pattern
     */
    public String getCsvDateFormat();

    /**
     * Set the Date Format for csv
     *
     * @param csvDateFormat Date Format pattern
     */
    public void setCsvDateFormat(String csvDateFormat);
}
