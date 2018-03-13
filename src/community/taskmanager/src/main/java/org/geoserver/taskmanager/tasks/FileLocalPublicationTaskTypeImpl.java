package org.geoserver.taskmanager.tasks;

import java.io.File;
import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogFactoryImpl;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileLocalPublicationTaskTypeImpl implements TaskType {
    
    public static final String NAME = "LocalFilePublication";
    
    public static final String PARAM_LAYER = "layer";
    
    public static final String PARAM_FILE = "file";
    
    protected final Map<String, ParameterInfo> paramInfo = new LinkedHashMap<String, ParameterInfo>();

    @Autowired
    protected ExtTypes extTypes;
    
    @Autowired
    protected Catalog catalog;

    @Override
    public String getName() {
        return NAME;
    }

    @PostConstruct
    public void initParamInfo() {
        paramInfo.put(PARAM_FILE, new ParameterInfo(PARAM_FILE, ParameterType.FILE, false));
        paramInfo.put(PARAM_LAYER, new ParameterInfo(PARAM_LAYER, extTypes.layerName, true));
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return paramInfo;
    }

    @Override
    public TaskResult run(TaskContext ctx) throws TaskException {
        CatalogFactory catalogFac = new CatalogFactoryImpl(catalog);
        
        final File file = (File) ctx.getParameterValues().get(PARAM_FILE);
        final Name layerName = (Name) ctx.getParameterValues().get(PARAM_LAYER);
        
        final NamespaceInfo ns = catalog.getNamespaceByURI(layerName.getNamespaceURI());
        final WorkspaceInfo ws = catalog.getWorkspaceByName(ns.getName());
        
        final boolean createLayer = catalog.getLayerByName(layerName) == null;
        final boolean createStore;
        final boolean createResource;
        
        final LayerInfo layer;
        final StoreInfo store;
        final ResourceInfo resource;
        
        boolean isShapeFile = file.getName().toUpperCase().endsWith(".SHP");
                
        if (createLayer) {
            final StoreInfo _store = catalog.getStoreByName(ws, layerName.getLocalPart(), StoreInfo.class);
            final CoverageInfo _resource = catalog.getResourceByName(layerName, CoverageInfo.class);
            createStore = _store == null;
            createResource = _resource == null;
            
            if (createStore) {
                store = isShapeFile ? catalogFac.createDataStore() : catalogFac.createCoverageStore();
                store.setWorkspace(ws);
                store.setName(layerName.getLocalPart());
                try {
                    if (isShapeFile) {
                        store.getConnectionParameters().put("url", file.toURI().toURL());
                    } else {
                        ((CoverageStoreInfo) store).setURL(
                                file.toURI().toURL().toString());
                        ((CoverageStoreInfo) store).setType(
                                GridFormatFinder.findFormat(file).getName());
                    }
                } catch (MalformedURLException e) {
                    throw new TaskException(e);
                }
                store.setEnabled(true);
                catalog.add(store);
            } else {
                store = _store;
            }
            
            CatalogBuilder builder = new CatalogBuilder(catalog);
            if (createResource) {
                builder.setStore(store);
                try {
                    if (isShapeFile) {
                        resource = builder.buildFeatureType(new NameImpl(layerName.getLocalPart()));
                    } else {
                        resource = builder.buildCoverage(layerName.getLocalPart());
                    }
                } catch (Exception e) {
                    if (createStore) {
                        catalog.remove(store);
                    }
                    throw new TaskException(e);
                }
                catalog.add(resource);
            } else {
                resource = _resource;
            }
            
            layer = builder.buildLayer(resource);
            layer.setAdvertised(false);
            catalog.add(layer);     
        } else {
            layer = null;
            resource = null;
            store = null;
            createStore = false;
            createResource = false;
        }
        
        return new TaskResult() {

            @Override
            public void commit() throws TaskException {
                if (createLayer) {
                    LayerInfo editLayer = catalog.getLayer(layer.getId());
                    editLayer.setAdvertised(true);
                    catalog.save(editLayer);
                }
            }

            @Override
            public void rollback() throws TaskException {
                if (createLayer) {
                    catalog.remove(layer);
                    if (createResource) {
                        catalog.remove(resource);
                    }
                    if (createStore) {
                        catalog.remove(store);
                    }
                }
            }
            
        };        
       
    }

    @Override
    public void cleanup(TaskContext ctx) throws TaskException {
        final Name layerName = (Name) ctx.getParameterValues().get(PARAM_LAYER);
        final String workspace = catalog.getNamespaceByURI(layerName.getNamespaceURI()).getPrefix();

        final LayerInfo layer = catalog.getLayerByName(layerName);               
        final StoreInfo store = catalog.getStoreByName(workspace, layerName.getLocalPart(), StoreInfo.class);
        final ResourceInfo resource = catalog.getResourceByName(layerName, 
                ResourceInfo.class);
        
        catalog.remove(layer);
        catalog.remove(resource);
        catalog.remove(store);
    }

}
