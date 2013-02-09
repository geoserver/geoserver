/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
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
package org.geoserver.wms.eo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.util.logging.Logging;

/**
 * Builder class which provides convenience methods for managing EO stores, resources, layers and layer groups.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public class EoCatalogBuilder {

    private Catalog catalog;
    private static final Logger LOGGER = Logging.getLogger(EoCatalogBuilder.class);   
    
    
    public EoCatalogBuilder(Catalog catalog) {
        this.catalog = catalog;
    }
    
    
    public LayerGroupInfo createEoLayerGroup(WorkspaceInfo ws, String groupName, 
            String outlineLayerName,
            String browseLayerName, 
            String browseImageUrl,
            String bandsLayerName,
            String bandsUrl,
            String masksLayerName,
            String masksUrl,
            String parametersLayerName,
            String parametersUrl) {
        
        LayerInfo browseLayer = createEoMosaicLayer(ws, browseLayerName, EoLayerType.EO_PRODUCT, browseImageUrl);
        LayerInfo bandsLayer = createEoMosaicLayer(ws, bandsLayerName, EoLayerType.BAND_COVERAGE, bandsUrl);
        LayerInfo masksLayer = createEoMosaicLayer(ws, masksLayerName, EoLayerType.BITMASK, masksUrl);
        LayerInfo paramsLayer = createEoMosaicLayer(ws, parametersLayerName, EoLayerType.GEOPHYSICAL_PARAMETER, parametersUrl);
        
        LayerInfo outlineLayer;
        try {
            outlineLayer = createEoOutlineLayer(bandsUrl, ws, outlineLayerName);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("The layer '" + outlineLayerName + "' could not be created. Failure message: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("The layer '" + outlineLayerName + "' could not be created. Failure message: " + e.getMessage(), e);
        }

        // create layer group
        LayerGroupInfo layerGroup = catalog.getFactory().createLayerGroup();
        layerGroup.setWorkspace(ws);
        layerGroup.setName(groupName);
        layerGroup.setMode(LayerGroupInfo.Mode.EO);
        layerGroup.setRootLayer(browseLayer);
        layerGroup.setRootLayerStyle(browseLayer.getDefaultStyle());
        layerGroup.getLayers().add(outlineLayer);
        layerGroup.getStyles().add(outlineLayer.getDefaultStyle());        
        layerGroup.getLayers().add(bandsLayer);
        layerGroup.getStyles().add(bandsLayer.getDefaultStyle());
        if (masksLayer != null) {
            layerGroup.getLayers().add(masksLayer);    
            layerGroup.getStyles().add(masksLayer.getDefaultStyle());
        }
        if (paramsLayer != null) {
            layerGroup.getLayers().add(paramsLayer);            
            layerGroup.getStyles().add(paramsLayer.getDefaultStyle());            
        }
        
        try {
            CatalogBuilder builder = new CatalogBuilder(catalog);
            builder.calculateLayerGroupBounds(layerGroup);
            
            catalog.add(layerGroup);
            return layerGroup;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("The layer group '" + groupName + "' could not be created. Failure message: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("The layer group '" + groupName + "' could not be created. Failure message: " + e.getMessage(), e);
        }        
    }
    
    private Properties loadProperties(File propertiesFile) throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = new BufferedInputStream(new FileInputStream(propertiesFile));
        try {
            properties.load(inputStream);
        } finally {
            inputStream.close();
        }
        return properties;
    }
    
    protected DataStoreFactorySpi getOutlineDataStoreFactory(File dir) throws Exception {
        File datastorePropertiesFile = new File(dir, "datastore.properties");
        if (datastorePropertiesFile.exists()) {
            Properties datastoreProperties = loadProperties(datastorePropertiesFile);
            String SPIClass = datastoreProperties.getProperty("SPI");
            return (DataStoreFactorySpi) Class.forName(SPIClass).newInstance();
        } else {
            return new ShapefileDataStoreFactory();
        }        
    }
    
    protected Map<String, Serializable> getOutlineDataStoreParameters(File dir, DataStoreFactorySpi dataStoreFactory) throws IOException {
        File datastorePropertiesFile = new File(dir, "datastore.properties");
        if (datastorePropertiesFile.exists()) {
            Properties datastoreProperties = loadProperties(datastorePropertiesFile);
            // TODO H2 workaround?
            return Utils.createDataStoreParamsFromPropertiesFile(datastoreProperties, dataStoreFactory);
        } else {
            // shp store
            File shpFile = new File(dir, dir.getName() + ".shp");
            
            Map<String,Serializable> params = new HashMap<String,Serializable>();
            
            // TODO is there a better way to convert a path to a URL?
            // DataUtilities.fileToURL(file) doesn't work: GeoServer saves an empty url
            params.put(ShapefileDataStoreFactory.URLP.key, "file://" + shpFile.getAbsolutePath());
            
            params.put(ShapefileDataStoreFactory.MEMORY_MAPPED.key, true);
            // params.put(ShapefileDataStoreFactory.DBFTIMEZONE.key, Utils.UTC_TIME_ZONE);
            // TODO other params?
            return params;
        }                
    }
    
    protected LayerInfo createEoOutlineLayer(String url, WorkspaceInfo ws, String name) throws Exception {
        File dir = DataUtilities.urlToFile(new URL(url));
        
        CatalogBuilder builder = new CatalogBuilder(catalog);
        DataStoreInfo storeInfo = builder.buildDataStore(name);
        
        DataStoreFactorySpi dataStoreFactory = getOutlineDataStoreFactory(dir);
        Map<String,Serializable> parameters = getOutlineDataStoreParameters(dir, dataStoreFactory);
        
        storeInfo.setType(dataStoreFactory.getDisplayName());     
        storeInfo.setWorkspace(ws);
        storeInfo.getConnectionParameters().putAll(parameters);
        catalog.add(storeInfo);

        builder.setStore(storeInfo);        
        
        DataStore dataStore = dataStoreFactory.createDataStore(parameters);
        // TODO is it correct to use dir name?
        @SuppressWarnings("rawtypes")
        FeatureSource featureSource = dataStore.getFeatureSource(dir.getName());
        
        FeatureTypeInfo featureType = builder.buildFeatureType(featureSource);
        featureType.setName(name + " Resource");
        builder.setupBounds(featureType, featureSource);
        catalog.add(featureType);
        
        LayerInfo layer = builder.buildLayer(featureType);
        layer.setName(name);
        layer.setTitle(name);
        layer.setEnabled(true);
        layer.setQueryable(true);
        layer.setType(LayerInfo.Type.VECTOR);
        layer.getMetadata().put(EoLayerType.KEY, EoLayerType.COVERAGE_OUTLINE);
        catalog.add(layer);
        
        return layer;
    }
    
    protected CoverageStoreInfo createEoMosaicStore(WorkspaceInfo ws, String name, String url) {
        CoverageStoreInfo storeInfo = catalog.getFactory().createCoverageStore();
        storeInfo.setWorkspace(ws);
        storeInfo.setType("ImageMosaic");
        storeInfo.setEnabled(true);
        storeInfo.setName(name);
        storeInfo.setURL(url);
        
        try {
            catalog.add(storeInfo);
            return storeInfo;
        } catch (RuntimeException e) {
            String msg = "The coverage store '" + name + "' could not be created. Failure message: " + e.getMessage();
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, msg, e);
            }
            
            throw new IllegalArgumentException(msg, e);
        }        
    }
    
    public LayerInfo createEoMosaicLayer(WorkspaceInfo ws, String name, EoLayerType type, String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        
        CoverageStoreInfo store = createEoMosaicStore(ws, name, url);
        
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setStore(store);
        try {
            CoverageInfo resource = builder.buildCoverage();
            resource.setName(name);
            catalog.add(resource);
            
            LayerInfo layer = builder.buildLayer(resource);
            layer.setName(name);
            layer.setTitle(name);
            layer.setEnabled(true);
            layer.setQueryable(true);
            layer.setType(LayerInfo.Type.RASTER);
            layer.getMetadata().put(EoLayerType.KEY, type.toString());
            catalog.add(layer);
            
            return layer;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("The layer '" + name + "' could not be created. Failure message: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("The layer '" + name + "' could not be created. Failure message: " + e.getMessage(), e);
        }
    }

    private void delete(LayerInfo layer) {
        ResourceInfo resource = layer.getResource();
        StoreInfo store = resource.getStore();
        catalog.remove(layer);
        catalog.remove(resource);
        catalog.remove(store);                    
    }
    
    public void delete(LayerGroupInfo group) {
        // load layers in group
        group = catalog.getLayerGroupByName(group.getWorkspace(), group.getName());
        try {
            catalog.remove(group);
            delete(group.getRootLayer());            
            for (PublishedInfo p : group.getLayers()) {
                if (p instanceof LayerGroupInfo) {
                    delete(group);
                } else {
                    delete((LayerInfo) p);
                }
            }            
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("The group '" + group.getName() + "' could not be removed. Failure message: " + e.getMessage(), e);
        }
    }   
}