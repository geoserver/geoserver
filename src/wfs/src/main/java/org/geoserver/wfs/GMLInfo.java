package org.geoserver.wfs;

import java.io.Serializable;

/**
 * Configuration for gml encoding.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public interface GMLInfo extends Serializable {

    /**
     * Enumeration for srsName style.
     * <p>
     * <ul>
     *   <li>{@link #NORMAL} : EPSG:XXXX
     *   <li>{@link #XML} : http://www.opengis.net/gml/srs/epsg.xml#XXXX
     *   <li>{@link #URN} : urn:x-ogc:def:crs:EPSG:XXXX
     * </ul>
     * <p>
     *
     */
    public static enum SrsNameStyle {
        NORMAL {
            public String getPrefix() {
                return "EPSG:";
            }
        },
        XML {
            public String getPrefix() {
                return "http://www.opengis.net/gml/srs/epsg.xml#";
            }
        },
        URN {
            public String getPrefix() {
                return "urn:x-ogc:def:crs:EPSG:";
            }  
        };
        
        abstract public String getPrefix();
    }
    
    /**
     * The srs name style to be used when encoding the gml 'srsName' attribute.
     */
    SrsNameStyle getSrsNameStyle();
    
    /**
     * Sets the srs name style to be used when encoding the gml 'srsName' attribute.
     */
    void setSrsNameStyle( SrsNameStyle srsNameStyle );
    
    /**
     * Controls how attributes are handled with regard to attributes defined in the schema of
     * AbstractFeatureType, name, description, etc... 
     * <p>
     * When set this flag will cause the attributes to be redefined in the application schema 
     * namespace.
     * </p>
     */
    Boolean getOverrideGMLAttributes();
    
    /**
     * Sets the flag that controls how attributes are handled with regard to attributes defined in 
     * the schema of AbstractFeatureType.
     * 
     * @see {@link #getOverrideGMLAttributes()}
     */
    void setOverrideGMLAttributes(Boolean overrideGMLAttributes);
}
