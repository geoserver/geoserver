/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;


/**
 * 
 */
public class LayerGroupHelper {

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
     * @return
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
        
        for (PublishedInfo p : group.getLayers()) {
            if (p instanceof LayerInfo) {
                LayerInfo l = (LayerInfo) p;
                layers.add(l);
            } else {
                allLayers((LayerGroupInfo) p, layers);
            }
        }        
    }     
    
    /**
     * 
     * @return
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
            if (p instanceof LayerInfo) {
                styles.add(group.getStyles().get(i));
            } else {
                allStyles((LayerGroupInfo) p, styles);
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
            for (PublishedInfo p : group.getLayers()) {
                if (p instanceof LayerInfo) {
                    LayerInfo l = (LayerInfo) p;
                    layers.add(l);
                } else {
                    allLayersForRendering((LayerGroupInfo) p, layers, false);
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
                if (p instanceof LayerInfo) {
                    styles.add(group.getStyles().get(i));
                } else {
                    allStylesForRendering((LayerGroupInfo) p, styles, false);
                }
            }
        }
    }   
    
    /**
     * 
     * @param crs
     * @throws Exception
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
     * @throws Exception
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
            for (PublishedInfo child : group.getLayers()) {
                if (child instanceof LayerGroupInfo) {
                    if (isGroupInStack((LayerGroupInfo) child, path)) {
                        path.push((LayerGroupInfo) child);
                        return true;
                    } else if (checkLoops((LayerGroupInfo) child, path)) {
                        return true;
                    }                
                }
            }
        }
        
        path.pop();
        return false;
    }
    
    private static boolean isGroupInStack(LayerGroupInfo group, Stack<LayerGroupInfo> path) {
        for (LayerGroupInfo groupInPath : path) {
            if (groupInPath.getId() != null && groupInPath.getId().equals(group.getId())) {
                return true;
            }
        }
        
        return false;
    }
}