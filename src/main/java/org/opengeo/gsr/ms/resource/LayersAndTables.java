/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
 package org.opengeo.gsr.ms.resource;

import org.opengeo.gsr.core.geometry.GeometryTypeEnum;
import org.opengeo.gsr.core.renderer.Renderer;
import org.opengeo.gsr.core.renderer.StyleEncoder;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.LayerInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LayersAndTables {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(LayersAndTables.class);

    public final List<LayerOrTable> layers;
    public final List<LayerOrTable> tables;

    private LayersAndTables(List<LayerOrTable> layers, List<LayerOrTable> tables) {
        this.layers = layers;
        this.tables = tables;
    }
    
    /**
     * Look up a single GSR layer (with at least one geometry column) or table.
     * 
     * @param catalog GeoServer Catalog
     * @param workspaceName GeoServer workspace name
     * @param layerId Index of Layer (based on sorting by layer name )
     * @return LayerOrTable from workspaceName identified by layerId
     * @throws IOException 
     */
    public static LayerOrTable find(Catalog catalog, String workspaceName, Integer id) throws IOException{
    	// short list all layers
        List<LayerInfo> layersInWorkspace = new ArrayList<LayerInfo>();
        for (LayerInfo l : catalog.getLayers()) {
            if (l.enabled() && l.getType() == LayerInfo.Type.VECTOR &&
            		l.getResource().getStore().getWorkspace().getName().equals(workspaceName)) {
                layersInWorkspace.add(l);
            }
        }
        // sort for "consistent" order
        Collections.sort(layersInWorkspace, LayerNameComparator.INSTANCE);
        
        // retrieve indicated layer as LayerOrTable
        if( id < layersInWorkspace.size() ){
        	LayerInfo resource = layersInWorkspace.get(id);
        	return entry( resource, id );
        }
        return null; // not found
    }
    
    static ReferencedEnvelope sphericalMercator( LayerInfo layer, ReferencedEnvelope boundingBox ){
    	if (boundingBox == null) {
    		return null; // bounds not available
    	}
    	try {
            CoordinateReferenceSystem lonLat = CRS.decode("EPSG:4326");
            CoordinateReferenceSystem WEB_MERCATOR = CRS.decode("EPSG:3857");
            double minx = Math.max(boundingBox.getMinX(),  -180);
            double maxx = Math.min(boundingBox.getMaxX(),   180);
            double miny = Math.max(boundingBox.getMinY(), -85);
            double maxy = Math.min(boundingBox.getMaxY(),  85);
            ReferencedEnvelope sphericalMercatorBoundingBox = new ReferencedEnvelope(minx, maxx, miny, maxy, lonLat);
            sphericalMercatorBoundingBox = sphericalMercatorBoundingBox.transform(WEB_MERCATOR, true);
            return sphericalMercatorBoundingBox;
    	}
    	catch (FactoryException factoryException){
    		LOGGER.log(Level.WARNING, "EPSG definition unavailable for transform to EPSG:3857:" + layer, factoryException);
    	} catch (TransformException transformException) {
    		LOGGER.log(Level.WARNING, "EPSG Database unable to transform to Spherical Mercator:" + layer, transformException);
		}
        return null; // bounds not available
    }
    /**
     * Create LayerOrTable entry for layer.
     * <p>
     * Will return null, and log a warning if layer could not be represented
     * as LayerOrTable.
     * 
     * @param layer
     * @param idCounter
     * @return LayerOrTable, or null if layer could not be represented
     */
    static LayerOrTable entry( LayerInfo layer, int idCounter ) throws IOException {
    	ResourceInfo resource = layer.getResource();
        ReferencedEnvelope boundingBox = sphericalMercator( layer, resource.getLatLonBoundingBox() );
        Renderer renderer = StyleEncoder.effectiveRenderer(layer);
        
        if (resource instanceof CoverageInfo) {
            return new LayerOrTable(layer, idCounter, GeometryTypeEnum.POLYGON, boundingBox, renderer);
        } else if (resource instanceof FeatureTypeInfo) {
            final GeometryTypeEnum gtype;
            GeometryDescriptor gDesc = ((FeatureTypeInfo)resource).getFeatureType().getGeometryDescriptor();
            
            if (gDesc == null) {
                gtype = null;
            } else { 
                gtype = GeometryTypeEnum.forJTSClass(gDesc.getType().getBinding());
            }
            
            if (gtype == null) {
            	return new LayerOrTable(layer, idCounter, gtype, boundingBox, renderer);
            } else {
                return new LayerOrTable(layer, idCounter, gtype, boundingBox, renderer);
            }
        }
        return null; // Skipping layer
    }
    /**
     * LayersAndTables lookup for GeoServer workspace.
     * @param catalog
     * @param workspaceName
     * @return GeoServer Layers gathered into GSR layers (with at least one geometry column) or tables.
     */
    public static LayersAndTables find(Catalog catalog, String workspaceName) {
        List<LayerOrTable> layers = new ArrayList<LayerOrTable>();
        List<LayerOrTable> tables = new ArrayList<LayerOrTable>();
        int idCounter = 0;
        List<LayerInfo> layersInWorkspace = new ArrayList<LayerInfo>();
        for (LayerInfo l : catalog.getLayers()) {
            if (l.enabled() && l.getType() == LayerInfo.Type.VECTOR && l.getResource().getStore().getWorkspace().getName().equals(workspaceName)) {
                layersInWorkspace.add(l);
            }
        }
        Collections.sort(layersInWorkspace, LayerNameComparator.INSTANCE);
        for (LayerInfo l : layersInWorkspace) {
        	try {
        		LayerOrTable entry = entry( l, idCounter );
	        	if( entry != null ){
	        		if( entry.gtype != null ){
	        			layers.add( entry );
	        		}
	        		else {
	        			tables.add( entry );
	        		}
	        	}
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Skipping layer " + l, e);
            } 
            idCounter++;
        }
        return new LayersAndTables(Collections.unmodifiableList(layers), Collections.unmodifiableList(tables));
    }

    @Override
    public String toString() {
        return layers.toString() + ";" + tables.toString();
    }
}
