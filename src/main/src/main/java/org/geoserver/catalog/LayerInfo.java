/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * A map layer.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface LayerInfo extends CatalogInfo {
    
    /**
     * The rendering buffer
     */
    public static final String BUFFER = "buffer";

    /**
     * Enumeration for type of layer.
     */
    public enum Type {
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
        };
        
        
        public abstract Integer getCode();
    }
    
    /**
     * Name of the layer.
     */
    String getName();

    /**
     * Sets the name of the layer.
     */
    void setName( String name );

    /**
     * The derived prefixed name of the layer.
     * <p>
     * If a workspace is set for the layer this method returns:
     * <pre>
     *   getWorkspace().getName() + ":" + getName();
     * </pre>
     * Otherwise it simply returns: <pre>getName()</pre>
     * </p>
     */
    String prefixedName();

    /**
     * The type of the layer.
     */
    Type getType();

    /**
     * Sets the type of the layer.
     */
    void setType( Type type );

    /**
     * The path which this layer is mapped to.
     * <p>
     * This is the same path a client would use to address the layer.
     * </p>
     * 
     * @uml.property name="path"
     */
    String getPath();

    /**
     * Sets the path this layer is mapped to.
     * 
     * @uml.property name="path"
     */
    void setPath(String path);

    /**
     * The default style for the layer.
     * 
     * @uml.property name="defaultStyle"
     * @uml.associationEnd inverse="resourceInfo:org.geoserver.catalog.StyleInfo"
     */
    StyleInfo getDefaultStyle();

    /**
     * Sets the default style for the layer.
     * 
     * @uml.property name="defaultStyle"
     */
    void setDefaultStyle(StyleInfo defaultStyle);

    /**
     * The styles available for the layer.
     * 
     * @uml.property name="styles"
     * @uml.associationEnd multiplicity="(0 -1)" container="java.util.Set"
     *                     inverse="resourceInfo:org.geoserver.catalog.StyleInfo"
     */
    Set<StyleInfo> getStyles();

    /**
     * The resource referenced by this layer.
     * 
     * @uml.property name="resource"
     * @uml.associationEnd inverse="layerInfo:org.geoserver.catalog.ResourceInfo"
     */
    ResourceInfo getResource();

    /**
     * Setter of the property <tt>resource</tt>
     * 
     * @param resource
     *                The getResource to set.
     * @uml.property name="resource"
     */
    void setResource(ResourceInfo resource);

    /**
     * The legend for the layer.
     * 
     * @uml.property name="legend"
     * @uml.associationEnd multiplicity="(0 -1)"
     *                     inverse="legend:org.geoserver.catalog.LegendInfo"
     */
    LegendInfo getLegend();

    /**
     * Sets the legend for the layer.
     * 
     * @uml.property name="legend"
     */
    void setLegend(LegendInfo legend);

    /**
     * Flag indicating wether the layer is enabled or not.
     * 
     * @uml.property name="enabled"
     */
    boolean isEnabled();
    
    /**
     * Derived property indicating whether both this LayerInfo and its ResourceInfo are enabled.
     * <p>
     * Note this is a derived property and hence not part of the model. Consider it equal to {@code
     * getResource() != null && getResouce.enabled() && this.isEnabled()}
     * </p>
     * 
     * @return the chained enabled status considering this object and it's associated ResourceInfo
     * @see #getResource()
     * @see ResourceInfo#enabled()
     */
    boolean enabled();

    /**
     * Sets the flag indicating wether the layer is enabled or not.
     * 
     * @uml.property name="enabled"
     */
    void setEnabled(boolean enabled);

    /**
     * Sets the queryable status
     * 
     * @param {@code true} to set this Layer as queryable and subject of GetFeatureInfo requests,
     *        {@code false} to make the layer not queryable.
     */
    void setQueryable(boolean queryable);

    /**
     * Whether the layer is queryable and hence can be subject of a GetFeatureInfo request.
     * <p>
     * Defaults to {@code true}
     * </p>
     */
    boolean isQueryable();

    /**
     * A persistent map of metadata.
     * <p>
     * Data in this map is intended to be persisted. Common case of use is to
     * have services associate various bits of data with a particular layer. An
     * example might include caching information.
     * </p>
     * <p>
     * The key values of this map are of type {@link String} and values are of
     * type {@link Serializable}.
     * </p>
     * 
     */
    MetadataMap getMetadata();

    /**
     * Gets the attribution information for this layer.  
     *
     * @return an AttributionInfo instance with the layer's attribution information.
     *
     * @see AttributionInfo
     */
    AttributionInfo getAttribution();

    /**
     * Sets the attribution information for this layer.  
     *
     * @param attribution an AttributionInfo instance with the new attribution information.
     *
     * @see AttributionInfo
     */
    void setAttribution(AttributionInfo attribution);
    
    /**
     * Returns true if the layer existence should be advertised (true by default, unless otherwise set)
     * @return
     */
    boolean isAdvertised();
    
    /**
     * Set to true if the layer should be advertised, false otherwise
     * @param advertised
     */
    void setAdvertised(boolean advertised);

    /**
     * @return the list of this layer's authority URLs
     */
    List<AuthorityURLInfo> getAuthorityURLs();
    
    /**
     * @return the list of this layer's identifiers
     */
    List<LayerIdentifierInfo> getIdentifiers();
}
