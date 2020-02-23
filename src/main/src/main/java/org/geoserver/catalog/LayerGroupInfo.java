/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.ArrayList;
import java.util.List;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * A map in which the layers grouped together can be referenced as a regular layer.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface LayerGroupInfo extends PublishedInfo {

    /** Enumeration for mode of layer group. */
    public enum Mode {
        /**
         * The layer group is seen as a single exposed layer with a name, does not actually contain
         * the layers it's referencing
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
         * The layer group is seen as a single exposed layer with a name, but contains the layers
         * it's referencing, thus hiding them from the caps document unless also shown in other tree
         * mode layers
         */
        OPAQUE_CONTAINER {
            public String getName() {
                return "Opaque Container";
            }

            public Integer getCode() {
                // added last, but a cross in between SINGLE and NAMED semantically,
                // so added in this position
                return 4;
            }
        },
        /**
         * The layer group retains a Name in the layer tree, but also exposes its nested layers in
         * the capabilities document.
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
         * The layer group is exposed in the tree, but does not have a Name element, showing
         * structure but making it impossible to get all the layers at once.
         */
        CONTAINER {
            public String getName() {
                return "Container Tree";
            }

            public Integer getCode() {
                return 2;
            }
        },
        /** A special mode created to manage the earth observation requirements. */
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

    /** Layer group mode. */
    Mode getMode();

    /** Sets layer group mode. */
    void setMode(Mode mode);

    /**
     * Get whether the layer group is forced to be not queryable and hence can not be subject of a
     * GetFeatureInfo request.
     *
     * <p>In order to preserve current default behavior (A LayerGroup is queryable when at least a
     * child layer is queryable), this flag allows explicitly indicate that it is not queryable
     * independently how the child layers are configured.
     *
     * <p>Default is {@code false}
     */
    boolean isQueryDisabled();

    /**
     * Set the layer group to be not queryable and hence can not be subject of a GetFeatureInfo
     * request.
     */
    void setQueryDisabled(boolean queryDisabled);

    /** Returns a workspace or <code>null</code> if global. */
    WorkspaceInfo getWorkspace();

    /** Get root layer. */
    LayerInfo getRootLayer();

    /** Set root layer. */
    void setRootLayer(LayerInfo rootLayer);

    /** Get root layer style. */
    StyleInfo getRootLayerStyle();

    /** Set root layer style. */
    void setRootLayerStyle(StyleInfo style);

    /** The layers and layer groups in the group. */
    List<PublishedInfo> getLayers();

    /**
     * The styles for the layers in the group.
     *
     * <p>This list is a 1-1 correspondence to {@link #getLayers()}.
     */
    List<StyleInfo> getStyles();

    /** */
    List<LayerInfo> layers();

    /** */
    List<StyleInfo> styles();

    /** The bounds for the base map. */
    ReferencedEnvelope getBounds();

    /** Sets the bounds for the base map. */
    void setBounds(ReferencedEnvelope bounds);

    /** Sets the workspace. */
    void setWorkspace(WorkspaceInfo workspace);

    /**
     * A collection of metadata links for the resource.
     *
     * @uml.property name="metadataLinks"
     * @see MetadataLinkInfo
     */
    List<MetadataLinkInfo> getMetadataLinks();

    /**
     * Return the keywords associated with this layer group. If no keywords are available an empty
     * list should be returned.
     *
     * @return a non NULL list containing the keywords associated with this layer group
     */
    default List<KeywordInfo> getKeywords() {
        return new ArrayList<>();
    }

    /**
     * A way to compare two LayerGroupInfo instances that works around all the wrappers we have
     * around (secured, decorating ecc) all changing some aspects of the bean and breaking usage of
     * "common" equality). This method only uses getters to fetch the fields. Could have been build
     * using EqualsBuilder and reflection, but would have been very slow and we do lots of these
     * calls on large catalogs.
     */
    public static boolean equals(LayerGroupInfo lg, Object obj) {
        if (lg == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof LayerGroupInfo)) return false;
        LayerGroupInfo other = (LayerGroupInfo) obj;
        if (lg.getBounds() == null) {
            if (other.getBounds() != null) return false;
        } else if (!lg.getBounds().equals(other.getBounds())) return false;
        if (lg.getId() == null) {
            if (other.getId() != null) return false;
        } else if (!lg.getId().equals(other.getId())) return false;
        if (lg.getLayers() == null) {
            if (other.getLayers() != null) return false;
        } else if (!lg.getLayers().equals(other.getLayers())) return false;
        if (lg.getMetadata() == null) {
            if (other.getMetadata() != null) return false;
        } else if (!lg.getMetadata().equals(other.getMetadata())) return false;
        if (lg.getName() == null) {
            if (other.getName() != null) return false;
        } else if (!lg.getName().equals(other.getName())) return false;
        if (lg.getMode() == null) {
            if (other.getMode() != null) return false;
        } else if (!lg.getMode().equals(other.getMode())) return false;
        if (lg.getTitle() == null) {
            if (other.getTitle() != null) {
                return false;
            }
        } else if (!lg.getTitle().equals(other.getTitle())) return false;
        if (lg.getAbstract() == null) {
            if (other.getAbstract() != null) {
                return false;
            }
        } else if (!lg.getAbstract().equals(other.getAbstract())) return false;
        if (lg.getWorkspace() == null) {
            if (other.getWorkspace() != null) return false;
        } else if (!lg.getWorkspace().equals(other.getWorkspace())) return false;

        List<StyleInfo> styles = canonicalStyles(lg.getStyles(), lg.getLayers());
        List<StyleInfo> otherStyles = canonicalStyles(other.getStyles(), other.getLayers());
        if (styles == null) {
            if (otherStyles != null) return false;
        } else if (!styles.equals(otherStyles)) return false;
        if (lg.getAuthorityURLs() == null) {
            if (other.getAuthorityURLs() != null) return false;
        } else if (!lg.getAuthorityURLs().equals(other.getAuthorityURLs())) return false;

        if (lg.getIdentifiers() == null) {
            if (other.getIdentifiers() != null) return false;
        } else if (!lg.getIdentifiers().equals(other.getIdentifiers())) return false;

        if (lg.getRootLayer() == null) {
            if (other.getRootLayer() != null) return false;
        } else if (!lg.getRootLayer().equals(other.getRootLayer())) return false;

        if (lg.getRootLayerStyle() == null) {
            if (other.getRootLayerStyle() != null) return false;
        } else if (!lg.getRootLayerStyle().equals(other.getRootLayerStyle())) return false;

        if (lg.getAttribution() == null) {
            if (other.getAttribution() != null) return false;
        } else if (!lg.getAttribution().equals(other.getAttribution())) return false;

        if (lg.getMetadataLinks() == null) {
            if (other.getMetadataLinks() != null) return false;
        } else if (!lg.getMetadataLinks().equals(other.getMetadataLinks())) return false;

        if (!lg.isQueryDisabled() == other.isQueryDisabled()) return false;

        return true;
    }

    /**
     * Styles, especially when using defaults, can be represented in too many ways (null, list of
     * nulls, and so on). This returns a single canonical representation for those cases, trying not
     * to allocate new objects.
     */
    static List<StyleInfo> canonicalStyles(List<StyleInfo> styles, List<PublishedInfo> layers) {
        if (styles == null || styles.isEmpty()) {
            return null;
        }
        boolean allNull = true;
        for (StyleInfo s : styles) {
            if (s != null) {
                allNull = false;
                break;
            }
        }
        if (allNull) {
            return null;
        }

        // at least one non null element, are they at least aligned with layers?
        if (styles.size() == layers.size()) {
            return styles;
        }

        // not aligned, build a new representation
        List<StyleInfo> canonical = new ArrayList<>(layers.size());
        for (int i = 0; i < layers.size(); i++) {
            StyleInfo s = styles.size() > i ? styles.get(i) : null;
            canonical.add(s);
        }
        return canonical;
    }

    /**
     * A way to build a hash code based only on LayerGroupInfo instances that works around all the
     * wrappers we have around (secured, decorating ecc) all changing some aspects of the bean and
     * breaking usage o "common" equality). This method only uses getters to fetch the fields. Could
     * have been build using HashCodeBuilder and reflection, but would have been very slow and we do
     * lots of these calls on large catalogs.
     */
    public static int hashCode(LayerGroupInfo lg) {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((lg.getBounds() == null) ? 0 : lg.getBounds().hashCode());
        result = prime * result + ((lg.getId() == null) ? 0 : lg.getId().hashCode());
        result = prime * result + ((lg.getLayers() == null) ? 0 : lg.getLayers().hashCode());
        result = prime * result + ((lg.getMetadata() == null) ? 0 : lg.getMetadata().hashCode());
        result = prime * result + ((lg.getName() == null) ? 0 : lg.getName().hashCode());
        result = prime * result + ((lg.getMode() == null) ? 0 : lg.getMode().hashCode());
        result = prime * result + ((lg.getTitle() == null) ? 0 : lg.getTitle().hashCode());
        result = prime * result + ((lg.getAbstract() == null) ? 0 : lg.getAbstract().hashCode());
        result = prime * result + ((lg.getWorkspace() == null) ? 0 : lg.getWorkspace().hashCode());
        result = prime * result + ((lg.getStyles() == null) ? 0 : lg.getStyles().hashCode());
        result = prime * result + ((lg.getRootLayer() == null) ? 0 : lg.getRootLayer().hashCode());
        result =
                prime * result
                        + ((lg.getRootLayerStyle() == null)
                                ? 0
                                : lg.getRootLayerStyle().hashCode());
        result =
                prime * result
                        + ((lg.getAuthorityURLs() == null) ? 0 : lg.getAuthorityURLs().hashCode());
        result =
                prime * result
                        + ((lg.getIdentifiers() == null) ? 0 : lg.getIdentifiers().hashCode());
        result =
                prime * result
                        + ((lg.getAttribution() == null) ? 0 : lg.getAttribution().hashCode());
        result =
                prime * result
                        + ((lg.getMetadataLinks() == null) ? 0 : lg.getMetadataLinks().hashCode());
        result = prime * result + Boolean.hashCode(lg.isQueryDisabled());
        return result;
    }
}
