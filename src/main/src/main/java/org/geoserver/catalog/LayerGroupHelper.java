/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2013, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
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
        allLayersForRendering(group, layers);
        return layers;
    }

    private static void allLayersForRendering(LayerGroupInfo group, List<LayerInfo> layers) {
        switch (group.getMode()) {
        case CONTAINER:
            throw new UnsupportedOperationException("LayerGroup mode " + Mode.CONTAINER.getName()
                    + " can not be rendered");
        case EO:
            layers.add(group.getRootLayer());
            break;
        default:
            for (PublishedInfo p : group.getLayers()) {
                if (p instanceof LayerInfo) {
                    LayerInfo l = (LayerInfo) p;
                    layers.add(l);
                } else {
                    allLayersForRendering((LayerGroupInfo) p, layers);
                }
            }
        }
    }

    public List<StyleInfo> allStylesForRendering() {
        List<StyleInfo> styles = new ArrayList<StyleInfo>();
        allStylesForRendering(group, styles);
        return styles;
    }

    private static void allStylesForRendering(LayerGroupInfo group, List<StyleInfo> styles) {
        switch (group.getMode()) {
        case CONTAINER:
            throw new UnsupportedOperationException("LayerGroup mode " + Mode.CONTAINER.getName()
                    + " can not be rendered");
        case EO:
            styles.add(group.getRootLayerStyle());
            break;
        default:
            int size = group.getLayers().size();
            for (int i = 0; i < size; i++) {
                PublishedInfo p = group.getLayers().get(i);
                if (p instanceof LayerInfo) {
                    styles.add(group.getStyles().get(i));
                } else {
                    allStylesForRendering((LayerGroupInfo) p, styles);
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
        ReferencedEnvelope bounds = transform(l.getResource().getLatLonBoundingBox(), crs);

        for (int i = 1; i < layers.size(); i++) {
            l = layers.get(i);
            bounds.expandToInclude(transform(l.getResource().getLatLonBoundingBox(), crs));
        }
        
        group.setBounds(bounds);
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
            if (latlon) {
                re = l.getResource().getLatLonBoundingBox();
            } else {
                re = l.getResource().boundingBox();
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
        
        for (PublishedInfo child : group.getLayers()) {
            if (child instanceof LayerGroupInfo) {
                if (path.contains(child)) {
                    path.push((LayerGroupInfo) child);
                    return true;
                } else if (checkLoops((LayerGroupInfo) child, path)) {
                    return true;
                }                
            }
        }
        
        path.pop();
        return false;
    }
}