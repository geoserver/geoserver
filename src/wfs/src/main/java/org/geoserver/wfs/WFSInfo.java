/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

        static Version get( String v ) {
            if ( v.startsWith( "1.0") ) {
                return V_10;
            }
            if ( v.startsWith( "1.1") ) {
                return V_11;
            }
            if ( v.startsWith( "2.0") ) {
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

        /**
         * Compares this value with a given version as string
         * 
         * @param version
         * @return
         */
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
            public int getCode() {
                return 0;
            }
        },
        DESCRIBEFEATURETYPE {
            public int getCode() {
                return 0;
            }
        },
        GETFEATURE{
            public int getCode() {
                return 1;
            }
        } ,
        LOCKFEATURE{
            public int getCode() {
                return 2;
            }
        } ,
        TRANSACTION_INSERT {
            public int getCode() {
                return 4;
            }
        },
        TRANSACTION_UPDATE {
            public int getCode() {
                return 8;
            }
        },
        TRANSACTION_DELETE {
            public int getCode() {
                return 16;
            }
        }, 
        TRANSACTION_REPLACE {
            public int getCode() {
                return 32;
            }
        };
        
        abstract public int getCode();
    }
    
    static enum ServiceLevel {
        BASIC {
            public int getCode() {
                return 1;
            } 
            public List<Operation> getOps() {
                return Arrays.asList(
                    Operation.GETCAPABILITIES,Operation.DESCRIBEFEATURETYPE,
                    Operation.GETFEATURE
                );
            }
        }, 
        TRANSACTIONAL {
            public int getCode() {
                return 15;
            }
            public List<Operation> getOps() {
                return Arrays.asList(
                    Operation.GETCAPABILITIES,Operation.DESCRIBEFEATURETYPE,
                    Operation.GETFEATURE, Operation.TRANSACTION_INSERT, 
                    Operation.TRANSACTION_UPDATE, Operation.TRANSACTION_DELETE,
                    Operation.TRANSACTION_REPLACE
                );
            }
        }, 
        COMPLETE {
            public int getCode() {
                return 31;
            }
            public List<Operation> getOps() {
                return Arrays.asList(
                    Operation.GETCAPABILITIES,Operation.DESCRIBEFEATURETYPE,
                    Operation.GETFEATURE, Operation.TRANSACTION_INSERT, 
                    Operation.TRANSACTION_UPDATE, Operation.TRANSACTION_DELETE,
                    Operation.TRANSACTION_REPLACE, Operation.LOCKFEATURE
                );
            }
        };
        
        abstract public int getCode();
        abstract public List<Operation> getOps();
        
        boolean contains(ServiceLevel other) {
            return getOps().containsAll( other.getOps() );
        }
    
        static public ServiceLevel get( int code ) {
            for ( ServiceLevel s : values() ) {
                if ( s.getCode() == code ) {
                    return s;
                }
            }
            
            return null;
        }
    };
    
    
    
    /**
     * A map of wfs version to gml encoding configuration.
     */
    Map<Version,GMLInfo> getGML();
    
    /**
     * A global cap on the number of features to allow when processing a request.
     * 
     * @uml.property name="maxFeatures"
     */
    int getMaxFeatures();

    /**
     * Sets the global cap on the number of features to allow when processing a 
     * request.
     * @uml.property name="maxFeatures"
     */
    void setMaxFeatures(int maxFeatures);
    
    /**
     * The level of service provided by the WFS.
     */
    ServiceLevel getServiceLevel();

    /**
     * Sets the level of service provided by the WFS. 
     */
    void setServiceLevel( ServiceLevel serviceLevel );
    
    /**
     * The flag which determines if gml:bounds elements should be encoded
     * at the feature level in gml output.
     */
    boolean isFeatureBounding();
    
    /**
     * Sets the flag which determines if gml:bounds elements should be encoded
     * at the feature level in gml output.
     * 
     */
    void setFeatureBounding( boolean featureBounding);
    
    /**
     * Get the flag that determines the encoding of the WFS schemaLocation. 
     * True if the WFS schemaLocation should refer to the canonical location,
     * false if the WFS schemaLocation should refer to a copy served by GeoServer.
     */
    boolean isCanonicalSchemaLocation();

    /**
     * Set the flag that determines the encoding of the WFS schemaLocation. 
     * True if the WFS schemaLocation should refer to the canonical location,
     * false if the WFS schemaLocation should refer to a copy served by GeoServer.
     */
    void setCanonicalSchemaLocation(boolean canonicalSchemaLocation);

    /**
     * Get the flag that determines encoding of featureMember or featureMembers 
     * True if the featureMember should be encoded 
     * False if the featureMembers should be encoded
     * 
     * @return encodingFeatureMember
     */
    boolean isEncodeFeatureMember();

    /**
     * set the response encoding option, featureMembers or featureMember
     */
    void setEncodeFeatureMember(boolean encodeFeatureMember);
    
    
    /**
     * Get the flag that determines if WFS hit requests (counts) will ignore
     * the maximum features limit for this server
     * @return hitsIgnoreMaxFeatures
     */ 
    boolean isHitsIgnoreMaxFeatures();
    
    /**
     * Set the option to ignore the maximum feature limit for WFS hit counts
     */
    void setHitsIgnoreMaxFeatures(boolean hitsIgnoreMaxFeatures);
    
    /**
     * Get the maximum number of features to be displayed in a layer preview.
     * Can be defined by the user. By default, 50.
     * @return maxNumberOfFeaturesForPreview
     */
    Integer getMaxNumberOfFeaturesForPreview();
    
    /**
     * Set the maximum number of features to be displayed in a layer preview
     */
    void setMaxNumberOfFeaturesForPreview(Integer maxNumberOfFeaturesForPreview);
    
    /**
     * The srs's that the WFS service will advertise in the capabilities document
     */
    List<String> getSRS();

    
}
