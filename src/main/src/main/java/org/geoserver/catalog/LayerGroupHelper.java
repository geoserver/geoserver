/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.styling.*;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/** Utility class to work with nested layer groups and extract selected sub-parts of it */
public class LayerGroupHelper {

    protected static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.catalog");

    private LayerGroupInfo group;

    /** @param group */
    public LayerGroupHelper(LayerGroupInfo group) {
        this.group = group;
    }

    /** */
    public List<LayerInfo> allLayers() {
        List<LayerInfo> layers = new ArrayList<LayerInfo>();
        allLayers(group, layers);
        return layers;
    }

    private static void allLayers(LayerGroupInfo group, List<LayerInfo> layers) {
        if (LayerGroupInfo.Mode.EO.equals(group.getMode())) {
            layers.add(group.getRootLayer());
        }
        int size = group.getLayers().size();
        for (int i = 0; i < size; i++) {
            PublishedInfo p = group.getLayers().get(i);
            StyleInfo s;
            // Handle incomplete layer groups, especially those constructed by the XStreamPersister
            if (group.getStyles() == null || group.getStyles().size() == 0) {
                s = null;
            } else {
                s = group.getStyles().get(i);
            }

            if (p instanceof LayerInfo) {
                LayerInfo l = (LayerInfo) p;
                layers.add(l);
            } else if (p instanceof LayerGroupInfo) {
                allLayers((LayerGroupInfo) p, layers);
            } else if (p == null && s != null) {
                expandStyleGroup(
                        s,
                        group.getBounds() == null
                                ? null
                                : group.getBounds().getCoordinateReferenceSystem(),
                        layers,
                        null);
            }
        }
    }

    /** Returns top level PublishedInfo, eventually expanding style groups */
    public List<PublishedInfo> allPublished() {
        List<PublishedInfo> publisheds = new ArrayList<>();
        allPublished(group, publisheds);
        return publisheds;
    }

    private List<PublishedInfo> allPublished(LayerGroupInfo group, List<PublishedInfo> publisheds) {
        if (LayerGroupInfo.Mode.EO.equals(group.getMode())) {
            publisheds.add(group.getRootLayer());
        }
        int size = group.getLayers().size();
        for (int i = 0; i < size; i++) {
            PublishedInfo p = group.getLayers().get(i);
            StyleInfo s;
            // Handle incomplete layer groups, especially those constructed by the XStreamPersister
            if (group.getStyles() == null || group.getStyles().size() == 0) {
                s = null;
            } else {
                s = group.getStyles().get(i);
            }

            if (p instanceof LayerInfo) {
                LayerInfo l = (LayerInfo) p;
                publisheds.add(l);
            } else if (p instanceof LayerGroupInfo) {
                publisheds.add(p);
            } else if (p == null && s != null) {
                List<LayerInfo> layers = new ArrayList<>();
                expandStyleGroup(
                        s,
                        group.getBounds() == null
                                ? null
                                : group.getBounds().getCoordinateReferenceSystem(),
                        layers,
                        null);
                publisheds.addAll(layers);
            }
        }

        return publisheds;
    }

    /** Returns all the groups contained in this group (including the group itself) */
    public List<LayerGroupInfo> allGroups() {
        List<LayerGroupInfo> groups = new ArrayList<LayerGroupInfo>();
        allGroups(group, groups);
        return groups;
    }

    private static void allGroups(LayerGroupInfo group, List<LayerGroupInfo> groups) {
        groups.add(group);
        for (PublishedInfo p : group.getLayers()) {
            if (p instanceof LayerGroupInfo) {
                LayerGroupInfo g = (LayerGroupInfo) p;
                allGroups(g, groups);
            }
        }
    }

    /** */
    public List<StyleInfo> allStyles() {
        List<StyleInfo> styles = new ArrayList<StyleInfo>();
        allStyles(group, styles);
        return styles;
    }

    private static void allStyles(LayerGroupInfo group, List<StyleInfo> styles) {
        if (LayerGroupInfo.Mode.EO.equals(group.getMode())) {
            styles.add(group.getRootLayerStyle());
        }

        int size = group.getLayers().size();
        for (int i = 0; i < size; i++) {
            PublishedInfo p = group.getLayers().get(i);
            StyleInfo s = group.getStyles().get(i);
            if (p instanceof LayerInfo) {
                styles.add(group.getStyles().get(i));
            } else if (p instanceof LayerGroupInfo) {
                allStyles((LayerGroupInfo) p, styles);
            } else if (p == null && s != null) {
                expandStyleGroup(
                        s,
                        group.getBounds() == null
                                ? null
                                : group.getBounds().getCoordinateReferenceSystem(),
                        null,
                        styles);
            }
        }
    }

    public List<LayerInfo> allLayersForRendering() {
        List<LayerInfo> layers = new ArrayList<LayerInfo>();
        allLayersForRendering(group, layers, true);
        return layers;
    }

    private static void allLayersForRendering(
            LayerGroupInfo group, List<LayerInfo> layers, boolean root) {
        switch (group.getMode()) {
            case EO:
                layers.add(group.getRootLayer());
                break;
            case CONTAINER:
                if (root) {
                    throw new UnsupportedOperationException(
                            "LayerGroup mode " + Mode.CONTAINER.getName() + " can not be rendered");
                }
                // continue to default behaviour:
            default:
                int size = group.getLayers().size();
                for (int i = 0; i < size; i++) {
                    PublishedInfo p = group.getLayers().get(i);
                    StyleInfo s = group.getStyles().get(i);
                    if (p instanceof LayerInfo) {
                        LayerInfo l = (LayerInfo) p;
                        layers.add(l);
                    } else if (p instanceof LayerGroupInfo) {
                        allLayersForRendering((LayerGroupInfo) p, layers, false);
                    } else if (p == null && s != null) {
                        expandStyleGroup(
                                s,
                                group.getBounds() == null
                                        ? null
                                        : group.getBounds().getCoordinateReferenceSystem(),
                                layers,
                                null);
                    }
                }
        }
    }

    public List<StyleInfo> allStylesForRendering() {
        List<StyleInfo> styles = new ArrayList<StyleInfo>();
        allStylesForRendering(group, styles, true);
        return styles;
    }

    private static void allStylesForRendering(
            LayerGroupInfo group, List<StyleInfo> styles, boolean root) {
        switch (group.getMode()) {
            case EO:
                styles.add(group.getRootLayerStyle());
                break;
            case CONTAINER:
                if (root) {
                    throw new UnsupportedOperationException(
                            "LayerGroup mode " + Mode.CONTAINER.getName() + " can not be rendered");
                }
                // continue to default behaviour:
            default:
                int size = group.getLayers().size();
                for (int i = 0; i < size; i++) {
                    PublishedInfo p = group.getLayers().get(i);
                    StyleInfo s = group.getStyles().get(i);
                    if (p instanceof LayerInfo) {
                        StyleInfo styleInfo = group.getStyles().get(i);
                        if (((LayerInfo) p).getResource() instanceof WMSLayerInfo) {
                            // pre 2.16.2, raster style was by default assigned to wms remote layers
                            // this was not a problem because the default style was always used to
                            // request the remote server, once we introduced the possibility tos
                            // elect remote styles this broke layer groups migrated form old data
                            // directories, we need now to ensure that a valid style is selected
                            WMSLayerInfo wmsLayerInfo =
                                    (WMSLayerInfo) ((LayerInfo) p).getResource();
                            styleInfo = getRemoteWmsLayerStyle(wmsLayerInfo, styleInfo);
                        }
                        styles.add(styleInfo);
                    } else if (p instanceof LayerGroupInfo) {
                        allStylesForRendering((LayerGroupInfo) p, styles, false);
                    } else if (p == null && s != null) {
                        expandStyleGroup(
                                s,
                                group.getBounds() == null
                                        ? null
                                        : group.getBounds().getCoordinateReferenceSystem(),
                                null,
                                styles);
                    }
                }
        }
    }

    /** @param crs */
    public void calculateBounds(CoordinateReferenceSystem crs) throws Exception {
        List<LayerInfo> layers = allLayers();
        if (layers.isEmpty()) {
            return;
        }

        LayerInfo l = layers.get(0);
        ReferencedEnvelope bounds = new ReferencedEnvelope(crs);

        for (int i = 0; i < layers.size(); i++) {
            l = layers.get(i);
            bounds.expandToInclude(transform(l.getResource().getLatLonBoundingBox(), crs));
        }

        group.setBounds(bounds);
    }

    /**
     * Use the CRS's defined bounds to populate the LayerGroup bounds.
     *
     * <p>If the CRS has no bounds then the layer group bounds are set to null instead
     */
    public void calculateBoundsFromCRS(CoordinateReferenceSystem crs) {
        Envelope crsEnvelope = CRS.getEnvelope(crs);
        if (crsEnvelope != null) {
            ReferencedEnvelope refEnvelope = new ReferencedEnvelope(crsEnvelope);
            this.group.setBounds(refEnvelope);
        } else {
            this.group.setBounds(null);
        }
    }

    /** */
    public void calculateBounds() throws Exception {
        List<LayerInfo> layers = allLayers();
        if (layers.isEmpty()) {
            return;
        }

        LayerInfo l = layers.get(0);
        ReferencedEnvelope bounds = l.getResource().boundingBox();
        boolean latlon = false;
        if (bounds == null) {
            bounds = l.getResource().getLatLonBoundingBox();
            latlon = true;
        }

        if (bounds == null) {
            throw new IllegalArgumentException(
                    "Could not calculate bounds from layer with no bounds, " + l.getName());
        }

        for (int i = 1; i < layers.size(); i++) {
            l = layers.get(i);

            ReferencedEnvelope re;
            ResourceInfo resource = l.getResource();
            if (latlon) {
                re = resource.getLatLonBoundingBox();
            } else {
                re = resource.boundingBox();
                if (re == null) {
                    re = resource.getLatLonBoundingBox();
                }
            }

            re = transform(re, bounds.getCoordinateReferenceSystem());
            if (re == null) {
                throw new IllegalArgumentException(
                        "Could not calculate bounds from layer with no bounds, " + l.getName());
            }
            bounds.expandToInclude(re);
        }

        group.setBounds(bounds);
    }

    /** Helper method for transforming an envelope. */
    private static ReferencedEnvelope transform(ReferencedEnvelope e, CoordinateReferenceSystem crs)
            throws TransformException, FactoryException {
        if (!CRS.equalsIgnoreMetadata(crs, e.getCoordinateReferenceSystem())) {
            return e.transform(crs, true);
        }
        return e;
    }

    /**
     * Check if the LayerGroup contains recursive structures
     *
     * @return true if the LayerGroup contains itself, or another LayerGroup contains itself
     */
    public Stack<LayerGroupInfo> checkLoops() {
        Stack<LayerGroupInfo> path = new Stack<LayerGroupInfo>();
        if (checkLoops(group, path)) {
            return path;
        } else {
            return null;
        }
    }

    public String getLoopAsString(Stack<LayerGroupInfo> path) {
        if (path == null) {
            return "";
        }

        StringBuilder s = new StringBuilder();
        for (LayerGroupInfo g : path) {
            s.append("/").append(g.getName());
        }
        return s.toString();
    }

    /**
     * Check if a layer group contains recursive structures
     *
     * @param group The current layer group
     * @param path Stack of each visited/parent LayerGroup
     * @return true if the LayerGroup contains itself, or another LayerGroup contains itself
     */
    private static boolean checkLoops(LayerGroupInfo group, Stack<LayerGroupInfo> path) {
        path.push(group);
        if (group.getLayers() != null) {
            int size = group.getLayers().size();
            for (int i = 0; i < size; i++) {
                PublishedInfo child = group.getLayers().get(i);
                StyleInfo s;
                // Handle incomplete layer groups, especially those constructed by the
                // XStreamPersister
                if (group.getStyles() == null || group.getStyles().size() == 0) {
                    s = null;
                } else {
                    s = group.getStyles().get(i);
                }
                if (child instanceof LayerGroupInfo) {
                    if (isGroupInStack((LayerGroupInfo) child, path)) {
                        path.push((LayerGroupInfo) child);
                        return true;
                    } else if (checkLoops((LayerGroupInfo) child, path)) {
                        return true;
                    }
                } else if (child == null && s != null) {
                    if (checkStyleGroupLoops(s, group, path)) {
                        return true;
                    }
                }
            }
        }

        path.pop();
        return false;
    }

    /**
     * Check if a style group contains the enclosing layer group, or other recursive structures.
     *
     * @param styleGroup The style group
     * @param group The current layer group
     * @param path Stack of each visited/parent LayerGroup
     * @return true if the style group contains itself, or another LayerGroup contains itself
     */
    private static boolean checkStyleGroupLoops(
            StyleInfo styleGroup, LayerGroupInfo group, Stack<LayerGroupInfo> path) {
        try {
            StyledLayerDescriptor sld = styleGroup.getSLD();

            final boolean[] hasLoop = {false};
            sld.accept(
                    new GeoServerSLDVisitorAdapter(
                            (Catalog) GeoServerExtensions.bean("catalog"),
                            group.getBounds() == null
                                    ? null
                                    : group.getBounds().getCoordinateReferenceSystem()) {

                        private final IllegalStateException recursionException =
                                new IllegalStateException(
                                        "Style group contains recursive structure");

                        @Override
                        public void visit(StyledLayerDescriptor sld) {
                            try {
                                super.visit(sld);
                            } catch (IllegalStateException e) {
                                if (recursionException.equals(e)) {
                                    hasLoop[0] = true;
                                } else {
                                    throw e;
                                }
                            }
                        }

                        @Override
                        public PublishedInfo visitNamedLayerInternal(StyledLayer namedLayer) {
                            // If this group hasn't been added to the catalog yet, make sure it is
                            // not referenced by a NamedLayer
                            if (namedLayer.getName() != null
                                    && namedLayer.getName().equals(group.getName())) {
                                throw recursionException;
                            }
                            LayerGroupInfo child =
                                    catalog.getLayerGroupByName(namedLayer.getName());
                            if (child != null) {
                                if (isGroupInStack(child, path)) {
                                    path.push(child);
                                    throw recursionException;
                                } else if (checkLoops(child, path)) {
                                    throw recursionException;
                                }
                                return child;
                            }
                            return null;
                        }
                    });
            return hasLoop[0];
        } catch (IllegalStateException
                | IOException
                | ServiceException
                | UncheckedIOException
                | UnsupportedOperationException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Error extracting layers from Style Group '"
                            + styleGroup.getName()
                            + "'. Skipping...",
                    e);
            return false;
        }
    }

    private static boolean isGroupInStack(LayerGroupInfo group, Stack<LayerGroupInfo> path) {
        for (LayerGroupInfo groupInPath : path) {
            if (groupInPath.getId() != null && groupInPath.getId().equals(group.getId())) {
                return true;
            }
        }

        return false;
    }

    private static void expandStyleGroup(
            StyleInfo styleGroup,
            CoordinateReferenceSystem crs,
            List<LayerInfo> layers,
            List<StyleInfo> styles) {
        if (layers == null) {
            layers = new ArrayList<>();
        }
        if (styles == null) {
            styles = new ArrayList<>();
        }

        try {
            StyledLayerDescriptor sld = styleGroup.getSLD();
            StyleGroupHelper helper =
                    new StyleGroupHelper((Catalog) GeoServerExtensions.bean("catalog"), crs);
            sld.accept(helper);
            layers.addAll(helper.getLayers());
            styles.addAll(helper.getStyles());
        } catch (IllegalStateException
                | IOException
                | ServiceException
                | UncheckedIOException
                | UnsupportedOperationException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Error extracting styles from Style Group '"
                            + styleGroup.getName()
                            + "'. Skipping...",
                    e);
        }
    }

    /**
     * Ensures that cascaded WMS Layer is assigned the correct remote style if the passed style is
     * NULL or unknown to cascaded WMS layer, the default style will returned *
     */
    private static StyleInfo getRemoteWmsLayerStyle(
            WMSLayerInfo wmsLayerInfo, StyleInfo styleInfo) {

        if (styleInfo == null) styleInfo = wmsLayerInfo.getDefaultStyle();
        else if (!wmsLayerInfo.findRemoteStyleByName(styleInfo.getName()).isPresent()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        styleInfo.getName()
                                + " style is not a known remote style for WMS Layer "
                                + wmsLayerInfo
                                + ","
                                + " Re-configure the Resource");
            }
            styleInfo = WMSLayerInfoImpl.DEFAULT_ON_REMOTE;
        }

        return styleInfo;
    }

    /**
     * Converts a style group sld into a flat list of {@link LayerInfo}s and {@link StyleInfo}s.
     *
     * <p>To handle styles and layers that are not in the catalog: User layers are wrapped in {@link
     * GeoServerSLDVisitor.StyleWrappingStyleInfoImpl}. Inline features and remote OWS services are
     * wrapped in {@link GeoServerSLDVisitor.FeatureSourceWrappingFeatureTypeInfoImpl}.
     */
    protected static class StyleGroupHelper extends GeoServerSLDVisitorAdapter {

        List<LayerInfo> layers;
        List<StyleInfo> styles;

        public StyleGroupHelper(Catalog catalog, CoordinateReferenceSystem fallbackCrs) {
            super(catalog, fallbackCrs);
        }

        @Override
        public void visit(StyledLayerDescriptor sld) {
            layers = new ArrayList<>();
            styles = new ArrayList<>();
            super.visit(sld);
        }

        public List<LayerInfo> getLayers() {
            return layers;
        }

        public List<StyleInfo> getStyles() {
            return styles;
        }

        @Override
        public PublishedInfo visitNamedLayerInternal(StyledLayer namedLayer) {
            PublishedInfo p = catalog.getLayerGroupByName(namedLayer.getName());
            if (p == null) {
                p = catalog.getLayerByName(namedLayer.getName());
                if (p == null) {
                    throw new ServiceException(
                            "No layer or layer group with name \"" + namedLayer.getName() + "\"");
                }
            }
            return p;
        }

        @Override
        public StyleInfo visitNamedStyleInternal(NamedStyle namedStyle) {
            StyleInfo s = catalog.getStyleByName(namedStyle.getName());
            layers.add((LayerInfo) info);
            styles.add(s);
            return s;
        }

        @Override
        public void visitUserStyleInternal(Style userStyle) {
            layers.add((LayerInfo) info);
            StyleInfoImpl style = new StyleWrappingStyleInfoImpl(userStyle);
            style.setCatalog(catalog);
            styles.add(style);
        }
    }
}
