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
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.styling.NamedStyle;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
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
        List<LayerInfo> layers = new ArrayList<>();
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
                LayerGroupInfo groupInfo = (LayerGroupInfo) p;
                LayerGroupStyle lgStyle = getStyleOrThrow(groupInfo, s);
                if (lgStyle != null) allLayers(getCrs(groupInfo.getBounds()), lgStyle, layers);
                else allLayers(groupInfo, layers);
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

    public List<LayerInfo> allLayers(String styleName) {
        LayerGroupStyle lgStyle = getStyleOrThrow(group, styleName);
        List<LayerInfo> layerInfos = new ArrayList<>();
        CoordinateReferenceSystem crs = getCrs(group.getBounds());
        allLayers(crs, lgStyle, layerInfos);
        return layerInfos;
    }

    private static void allLayers(
            CoordinateReferenceSystem crs, LayerGroupStyle groupStyle, List<LayerInfo> layers) {
        List<PublishedInfo> published = groupStyle.getLayers();
        List<StyleInfo> styles = groupStyle.getStyles();
        int pSize = published.size();
        int sSize = styles.size();
        for (int i = 0; i < pSize; i++) {
            PublishedInfo p = published.get(i);
            StyleInfo s;
            // Handle incomplete layer groups, especially those constructed by the XStreamPersister
            if (styles == null || sSize == 0) {
                s = null;
            } else {
                s = styles.get(i);
            }

            if (p instanceof LayerInfo) {
                LayerInfo l = (LayerInfo) p;
                layers.add(l);
            } else if (p instanceof LayerGroupInfo) {
                LayerGroupInfo groupInfo = (LayerGroupInfo) p;
                LayerGroupStyle lgStyle = getStyleOrThrow(groupInfo, s);
                if (lgStyle != null) allLayers(getCrs(groupInfo.getBounds()), lgStyle, layers);
                else allLayers(groupInfo, layers);
            } else if (p == null && s != null) {
                expandStyleGroup(s, crs, layers, null);
            }
        }
    }

    private static CoordinateReferenceSystem getCrs(ReferencedEnvelope envelope) {
        CoordinateReferenceSystem crs = null;
        if (envelope != null) crs = envelope.getCoordinateReferenceSystem();
        return crs;
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
                expandStyleGroup(s, getCrs(group.getBounds()), layers, null);
                publisheds.addAll(layers);
            }
        }

        return publisheds;
    }

    /** Returns all the groups contained in this group (including the group itself) */
    public List<LayerGroupInfo> allGroups() {
        List<LayerGroupInfo> groups = new ArrayList<>();
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
        List<StyleInfo> styles = new ArrayList<>();
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
                expandStyleGroup(s, getCrs(group.getBounds()), null, styles);
            }
        }
    }

    public List<LayerInfo> allLayersForRendering() {
        List<LayerInfo> layers = new ArrayList<>();
        allLayersForRendering(group, null, layers, true);
        return layers;
    }

    /**
     * Retrieves all the layers according to the LayerGroup style matching the style name.
     *
     * @param lgStyle the layer group style.
     * @return the list of layers contained, comprising also the ones contained by nested layer
     *     groups.
     */
    public List<LayerInfo> allLayersForRendering(String lgStyle) {
        List<LayerInfo> layers = new ArrayList<>();
        LayerGroupStyle groupStyle = getStyleOrThrow(group, lgStyle);
        CoordinateReferenceSystem crs = getCrs(group.getBounds());
        allLayers(crs, groupStyle, layers);
        return layers;
    }

    private static void allLayersForRendering(
            LayerGroupInfo group,
            LayerGroupStyle groupStyle,
            List<LayerInfo> layers,
            boolean root) {
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
                List<PublishedInfo> publishables =
                        groupStyle == null ? group.getLayers() : groupStyle.getLayers();
                List<StyleInfo> styles =
                        groupStyle == null ? group.getStyles() : groupStyle.getStyles();
                int size = publishables.size();
                for (int i = 0; i < size; i++) {
                    PublishedInfo p = publishables.get(i);
                    StyleInfo s = styles.get(i);
                    if (p instanceof LayerInfo) {
                        LayerInfo l = (LayerInfo) p;
                        layers.add(l);
                    } else if (p instanceof LayerGroupInfo) {
                        LayerGroupStyle gStyle = null;
                        LayerGroupInfo groupInfo = (LayerGroupInfo) p;
                        gStyle = getStyleOrThrow(groupInfo, s);
                        allLayersForRendering(groupInfo, gStyle, layers, false);
                    } else if (p == null && s != null) {
                        expandStyleGroup(s, getCrs(group.getBounds()), layers, null);
                    }
                }
        }
    }

    private static LayerGroupStyle getStyleOrThrow(LayerGroupInfo groupInfo, StyleInfo styleName) {
        LayerGroupStyle groupStyle = null;
        if (styleName != null && styleName.getName() != null && !"".equals(styleName.getName()))
            groupStyle = getStyleOrThrow(groupInfo, styleName.getName());
        return groupStyle;
    }

    // Get a style by name or throws exception if it is null.
    private static LayerGroupStyle getStyleOrThrow(LayerGroupInfo groupInfo, String styleName) {
        LayerGroupStyle groupStyle = getGroupStyleByName(groupInfo, styleName);
        if (groupStyle == null) {
            throw new NoSuchElementException(
                    "No Style with name "
                            + styleName
                            + " found for LayerGroup "
                            + groupInfo.getName());
        }
        return groupStyle;
    }

    public List<StyleInfo> allStylesForRendering() {
        List<StyleInfo> styles = new ArrayList<>();
        allStylesForRendering(group, null, styles, true);
        return styles;
    }

    private static void allStylesForRendering(
            LayerGroupInfo group,
            LayerGroupStyle groupStyle,
            List<StyleInfo> styles,
            boolean root) {
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
                List<PublishedInfo> publishables =
                        groupStyle == null ? group.getLayers() : groupStyle.getLayers();
                List<StyleInfo> stylesList =
                        groupStyle == null ? group.getStyles() : groupStyle.getStyles();
                int size = publishables.size();
                for (int i = 0; i < size; i++) {
                    PublishedInfo p = publishables.get(i);
                    StyleInfo s = stylesList.get(i);
                    if (p instanceof LayerInfo) {
                        if (((LayerInfo) p).getResource() instanceof WMSLayerInfo) {
                            // pre 2.16.2, raster style was by default assigned to wms remote layers
                            // this was not a problem because the default style was always used to
                            // request the remote server, once we introduced the possibility tos
                            // elect remote styles this broke layer groups migrated form old data
                            // directories, we need now to ensure that a valid style is selected
                            WMSLayerInfo wmsLayerInfo =
                                    (WMSLayerInfo) ((LayerInfo) p).getResource();
                            s = getRemoteWmsLayerStyle(wmsLayerInfo, s);
                        }
                        styles.add(s);
                    } else if (p instanceof LayerGroupInfo) {
                        LayerGroupInfo groupInfo = (LayerGroupInfo) p;
                        LayerGroupStyle groupStyle2 = getStyleOrThrow(groupInfo, s);
                        allStylesForRendering(groupInfo, groupStyle2, styles, false);
                    } else if (p == null && s != null) {
                        expandStyleGroup(s, getCrs(group.getBounds()), null, styles);
                    }
                }
        }
    }

    /**
     * Retrieves all styles according to the LayerGroupStyle matching the styleName, including those
     * of eventually present nested LayerGroups.
     *
     * @param styleName the name of the style.
     * @return the List of all the StyleInfo contained by the LayerGroup.
     */
    public List<StyleInfo> allStylesForRendering(String styleName) {
        List<StyleInfo> styles = new ArrayList<>();
        CoordinateReferenceSystem crs = getCrs(group.getBounds());
        LayerGroupStyle groupStyle = getStyleOrThrow(group, styleName);
        allStylesForRendering(crs, groupStyle, styles);
        return styles;
    }

    public void allStylesForRendering(
            CoordinateReferenceSystem crs, LayerGroupStyle groupStyle, List<StyleInfo> styleInfos) {
        List<PublishedInfo> publishable = groupStyle.getLayers();
        List<StyleInfo> styles = groupStyle.getStyles();
        int size = publishable.size();
        for (int i = 0; i < size; i++) {
            PublishedInfo p = publishable.get(i);
            StyleInfo s = styles.get(i);
            if (p instanceof LayerInfo) {
                if (((LayerInfo) p).getResource() instanceof WMSLayerInfo) {
                    WMSLayerInfo wmsLayerInfo = (WMSLayerInfo) ((LayerInfo) p).getResource();
                    s = getRemoteWmsLayerStyle(wmsLayerInfo, s);
                }
                styleInfos.add(s);
            } else if (p instanceof LayerGroupInfo) {
                LayerGroupInfo group = (LayerGroupInfo) p;
                LayerGroupStyle groupStyle2 = getStyleOrThrow(group, s);
                allStylesForRendering((LayerGroupInfo) p, groupStyle2, styleInfos, false);
            } else if (p == null && s != null) {
                expandStyleGroup(s, crs, null, styleInfos);
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

        for (LayerInfo layer : layers) {
            l = layer;
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
        Stack<LayerGroupInfo> path = new Stack<>();
        if (checkLoops(group, group.getLayers(), group.getStyles(), path)) {
            return path;
        } else if (checkLoops(group, group.getLayerGroupStyles(), path)) {
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
     * Check if layer group styles contains recursive structures
     *
     * @param group The current layer group
     * @param groupStyles The list of {@LayerGroupStyle} of the current layer group.
     * @param path Stack of each visited/parent LayerGroup
     * @return true if the LayerGroup contains itself, or another LayerGroup contains itself
     */
    private static boolean checkLoops(
            LayerGroupInfo group, List<LayerGroupStyle> groupStyles, Stack<LayerGroupInfo> path) {
        if (groupStyles != null) {
            for (LayerGroupStyle groupStyle : groupStyles) {
                if (checkLoops(group, groupStyle.getLayers(), groupStyle.getStyles(), path)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if a layer group contains recursive structures
     *
     * @param group The current layer group
     * @param layers the contained layer of the current layer group.
     * @param styles the contained styles of the current layer group.
     * @param path Stack of each visited/parent LayerGroup
     * @return true if the LayerGroup contains itself, or another LayerGroup contains itself
     */
    private static boolean checkLoops(
            LayerGroupInfo group,
            List<PublishedInfo> layers,
            List<StyleInfo> styles,
            Stack<LayerGroupInfo> path) {
        path.push(group);
        if (layers != null) {
            int size = layers.size();
            for (int i = 0; i < size; i++) {
                PublishedInfo child = layers.get(i);
                StyleInfo s;
                // Handle incomplete layer groups, especially those constructed by the
                // XStreamPersister
                if (styles == null || styles.isEmpty()) {
                    s = null;
                } else {
                    s = styles.get(i);
                }
                if (child instanceof LayerGroupInfo) {
                    LayerGroupInfo groupInfo = (LayerGroupInfo) child;
                    if (isGroupInStack((LayerGroupInfo) child, path)) {
                        path.push((LayerGroupInfo) child);
                        return true;
                    } else if (checkLoops(
                            groupInfo, groupInfo.getLayers(), groupInfo.getStyles(), path)) {
                        return true;
                    } else if (checkLoops(groupInfo, groupInfo.getLayerGroupStyles(), path))
                        return true;
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
                                } else if (checkLoops(
                                        child, child.getLayers(), child.getStyles(), path)) {
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
     * Find a {@link LayerGroupStyle} with the specified name if exists.
     *
     * @param group the LayerGroupInfo for which find the style.
     * @param styleName the name of the style to find.
     * @return the {@link LayerGroupStyle} corresponding to the specified name if exists, false
     *     otherwise.
     */
    public static LayerGroupStyle getGroupStyleByName(LayerGroupInfo group, String styleName) {
        List<LayerGroupStyle> styles = group.getLayerGroupStyles();
        LayerGroupStyle result = null;
        if (styleName != null && !"".equals(styleName)) {
            for (LayerGroupStyle s : styles) {
                if (s.getName().getName().equals(styleName)) {
                    result = s;
                    break;
                }
            }
        }
        return result;
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

    /**
     * Check if a LayerGroup has Mode Single or Opaque.
     *
     * @param groupInfo the LayerGroup.
     * @return true if single or opaque, false otherwise.
     */
    public static boolean isSingleOrOpaque(LayerGroupInfo groupInfo) {
        Mode mode = groupInfo.getMode();
        return mode.equals(Mode.SINGLE) || mode.equals(Mode.OPAQUE_CONTAINER);
    }
}
