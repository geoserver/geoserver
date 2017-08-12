/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.ServiceException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.styling.*;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;


/**
 * Utility class to work with nested layer groups and extract selected sub-parts of it
 */
public class LayerGroupHelper {

    protected static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.catalog");

    private LayerGroupInfo group;
    
    
    /**
     * 
     * @param group
     */
    public LayerGroupHelper(LayerGroupInfo group) {
        this.group = group;
    }

    
    /**
     * 
     *
     */
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
            StyleInfo s = group.getStyles().get(i);
            if (p instanceof LayerInfo) {
                LayerInfo l = (LayerInfo) p;
                layers.add(l);
            } else if (p instanceof LayerGroupInfo) {
                allLayers((LayerGroupInfo) p, layers);
            } else if (p == null && s != null) {
                expandStyleGroup(s, group.getBounds() == null ? null : group.getBounds().getCoordinateReferenceSystem(),
                        layers, null);
            }
        }        
    }
    
    /**
     * Returns all the groups contained in this group (including the group itself)
     * @return
     */
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
    
    /**
     * 
     *
     */
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
            } else if (p instanceof LayerGroupInfo){
                allStyles((LayerGroupInfo) p, styles);
            } else if (p == null && s != null) {
                expandStyleGroup(s, group.getBounds() == null ? null : group.getBounds().getCoordinateReferenceSystem(),
                        null, styles);
            }
        }
    }    
    
    public List<LayerInfo> allLayersForRendering() {
        List<LayerInfo> layers = new ArrayList<LayerInfo>();
        allLayersForRendering(group, layers, true);
        return layers;
    }

    private static void allLayersForRendering(LayerGroupInfo group, List<LayerInfo> layers, boolean root) {
        switch (group.getMode()) {
        case EO:
            layers.add(group.getRootLayer());
            break;        
        case CONTAINER:
            if (root) {
                throw new UnsupportedOperationException("LayerGroup mode " + Mode.CONTAINER.getName()
                        + " can not be rendered");
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
                }  else if (p == null && s != null) {
                    expandStyleGroup(s, group.getBounds() == null ? null : group.getBounds().getCoordinateReferenceSystem(),
                            layers, null);
                }
            }
        }
    }

    public List<StyleInfo> allStylesForRendering() {
        List<StyleInfo> styles = new ArrayList<StyleInfo>();
        allStylesForRendering(group, styles, true);
        return styles;
    }

    private static void allStylesForRendering(LayerGroupInfo group, List<StyleInfo> styles, boolean root) {
        switch (group.getMode()) {
        case EO:
            styles.add(group.getRootLayerStyle());
            break;        
        case CONTAINER:
            if (root) {
                throw new UnsupportedOperationException("LayerGroup mode " + Mode.CONTAINER.getName()
                        + " can not be rendered");
            }
            // continue to default behaviour:
        default:
            int size = group.getLayers().size();
            for (int i = 0; i < size; i++) {
                PublishedInfo p = group.getLayers().get(i);
                StyleInfo s = group.getStyles().get(i);
                if (p instanceof LayerInfo) {
                    styles.add(group.getStyles().get(i));
                } else if (p instanceof LayerGroupInfo) {
                    allStylesForRendering((LayerGroupInfo) p, styles, false);
                }  else if (p == null && s != null) {
                    expandStyleGroup(s, group.getBounds() == null ? null : group.getBounds().getCoordinateReferenceSystem(),
                            null, styles);
                }
            }
        }
    }   
    
    /**
     * 
     * @param crs
     */
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
     * If the CRS has no bounds then the layer group bounds are set to null instead
     * 
     * @param crs
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
    
    /**
     * 
     */
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
                if(re == null) {
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
    
    /**
     * Helper method for transforming an envelope.
     */
    private static ReferencedEnvelope transform(ReferencedEnvelope e, CoordinateReferenceSystem crs) throws TransformException, FactoryException {
        if (!CRS.equalsIgnoreMetadata(crs, e.getCoordinateReferenceSystem())) {
            return e.transform(crs, true);
        }
        return e;
    }    
    
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
    
    private static boolean checkLoops(LayerGroupInfo group, Stack<LayerGroupInfo> path) {
        path.push(group);
        if (group.getLayers() != null) {
            int size = group.getLayers().size();
            for (int i = 0; i < size; i++) {
                PublishedInfo child = group.getLayers().get(i);
                StyleInfo s = group.getStyles().get(i);
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

    private static boolean checkStyleGroupLoops(StyleInfo styleGroup, LayerGroupInfo group, Stack<LayerGroupInfo> path) {
        try {
            Catalog catalog = getCatalogFromStyle(styleGroup);
            StyledLayerDescriptor sld = new GeoServerDataDirectory(catalog.getResourceLoader()).parsedSld(styleGroup);

            final boolean[] hasLoop = {false};
            new SLDVisitorAdapter(catalog, group.getBounds() == null ? null : group.getBounds().getCoordinateReferenceSystem()) {
                @Override
                public SLDVisitor apply(StyledLayerDescriptor sld) throws IOException {
                    try {
                        super.apply(sld);
                    } catch (IllegalStateException e) {
                        hasLoop[0] = true;
                    }
                    return this;
                }

                @Override
                public PublishedInfo visitNamedLayer(StyledLayer namedLayer) {
                    //If this group hasn't been added to the catalog yet, make sure it is not referenced by a NamedLayer
                    if (namedLayer.getName() != null && namedLayer.getName().equals(group.getName())) {
                        throw new IllegalStateException("Style group contains recursive structure");
                    }
                    LayerGroupInfo child = catalog.getLayerGroupByName(namedLayer.getName());
                    if (child != null) {
                        if (isGroupInStack(child, path)) {
                            path.push(child);
                            throw new IllegalStateException("Style group contains recursive structure");
                        } else if (checkLoops(child, path)) {
                            throw new IllegalStateException("Style group contains recursive structure");
                        }
                        return child;
                    }
                    return null;
                }
            }.apply(sld);
            return hasLoop[0];
        } catch (IOException | ServiceException e) {
            LOGGER.log(Level.WARNING, "Error extracting layers from Style Group '" + styleGroup.getName() + "'. Skipping...", e);
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

    private static Catalog getCatalogFromStyle(StyleInfo info) {
        info = ModificationProxy.unwrap(info);
        if (info instanceof StyleInfoImpl) {
            return ((StyleInfoImpl) info).getCatalog();
        }
        throw new IllegalStateException("Could not get catalog from style group");
    }

    private static void expandStyleGroup(StyleInfo styleGroup, CoordinateReferenceSystem crs, List<LayerInfo> layers, List<StyleInfo> styles) {
        if (layers == null) {
            layers = new ArrayList<>();
        }
        if (styles == null) {
            styles = new ArrayList<>();
        }

        try {
            Catalog catalog = getCatalogFromStyle(styleGroup);
            StyledLayerDescriptor sld = ResourcePool.create(catalog).dataDir().parsedSld(styleGroup);
            StyleGroupHelper helper = new StyleGroupHelper(catalog, crs);
            helper.apply(sld);
            layers.addAll(helper.getLayers());
            styles.addAll(helper.getStyles());
        } catch (IOException | ServiceException e) {
            LOGGER.log(Level.WARNING, "Error extracting styles from Style Group '" + styleGroup.getName() + "'. Skipping...", e);
        }
    }

    protected static class StyleGroupHelper extends SLDVisitorAdapter {

        List<LayerInfo> layers;
        List<StyleInfo> styles;
        public StyleGroupHelper(Catalog catalog, CoordinateReferenceSystem fallbackCrs) {
            super(catalog, fallbackCrs);
        }

        @Override
        public StyleGroupHelper apply(StyledLayerDescriptor sld) throws IOException {
            layers = new ArrayList<>();
            styles = new ArrayList<>();
            super.apply(sld);
            return this;
        }

        public List<LayerInfo> getLayers() {
            return layers;
        }

        public List<StyleInfo> getStyles() {
            return styles;
        }

        @Override
        public PublishedInfo visitNamedLayer(StyledLayer namedLayer) {
            LayerGroupInfo lg = catalog.getLayerGroupByName(namedLayer.getName());
            if (lg == null) {
                return catalog.getLayerByName(namedLayer.getName());
            }
            return lg;
        }

        @Override
        public Style visitNamedStyle(StyledLayer layer, NamedStyle namedStyle, LayerInfo info) {
            StyleInfo s = catalog.getStyleByName(namedStyle.getName());
            layers.add(info);
            styles.add(s);
            try {
                return s.getStyle();
            } catch (IOException e) {
                throw new ServiceException(e);
            }
        }

        @Override
        public void visitUserStyle(StyledLayer layer, Style userStyle, LayerInfo info) {
            layers.add(info);
            styles.add(new StyleWrappingStyleInfoImpl(userStyle));
        }
    }
}