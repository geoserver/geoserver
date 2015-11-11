/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;


/**
 * A map in which the layers grouped together can be referenced as 
 * a regular layer.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public interface LayerGroupInfo extends PublishedInfo {

    /**
     * Enumeration for mode of layer group.
     */
    public enum Mode {
        /**
         * The layer group is seen as a single exposed layer with a name.
         */
        SINGLE {
            public String getName() {
                return "Single";
            }
            
            public Integer getCode() {
                return 0;
            }
        },
        /**
         * The layer group retains a Name in the layer tree, but also exposes its nested layers in the capabilities document.
         */
        NAMED {
            public String getName() {
                return "Named Tree";
            }
                        
            public Integer getCode() {
                return 1;
            }
        },
        /**
         * The layer group is exposed in the tree, but does not have a Name element, showing structure but making it impossible to get all the layers at once.
         */
        CONTAINER {
            public String getName() {
                return "Container Tree";
            }
                                    
            public Integer getCode() {
                return 2;
            }

        },
        /**
         * A special mode created to manage the earth observation requirements.
         */
        EO {
            public String getName() {
                return "Earth Observation Tree";
            }
                            
            public Integer getCode() {
                return 3;
            }
        };

        public abstract String getName();
        public abstract Integer getCode();
    }
        
    /**
     * Layer group mode.
     */
    Mode getMode();

    /**
     * Sets layer group mode.
     */
    void setMode( Mode mode );    

    /**
     * Returns a workspace or <code>null</code> if global.
     */
    WorkspaceInfo getWorkspace();    
    
    /**
     * Get root layer.
     */
    LayerInfo getRootLayer();
    
    /**
     * Set root layer.
     */
    void setRootLayer(LayerInfo rootLayer);
    
    /**
     * Get root layer style.
     */
    StyleInfo getRootLayerStyle();

    /**
     * Set root layer style.
     */
    void setRootLayerStyle(StyleInfo style);
    
    /**
     * The layers and layer groups in the group.
     */
    List<PublishedInfo> getLayers();
    
    /**
     * The styles for the layers in the group.
     * <p>
     * This list is a 1-1 correspondence to {@link #getLayers()}.
     * </p>
     */
    List<StyleInfo> getStyles();
    
    /**
     * 
     * @return
     */
    List<LayerInfo> layers();

    /**
     * 
     * 
     * @return
     */
    List<StyleInfo> styles();  
        
    /**
     * The bounds for the base map.
     */
    ReferencedEnvelope getBounds();

    /**
     * Sets the bounds for the base map.
     */
    void setBounds( ReferencedEnvelope bounds );

    /**
     * Sets the workspace.
     */
    void setWorkspace(WorkspaceInfo workspace);
    

    /**
     * A collection of metadata links for the resource.
     * 
     * @uml.property name="metadataLinks"
     * @see MetadataLinkInfo
     */
    List<MetadataLinkInfo> getMetadataLinks();
    
}
