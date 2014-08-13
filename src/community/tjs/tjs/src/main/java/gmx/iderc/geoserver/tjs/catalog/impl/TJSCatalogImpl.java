/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.catalog.impl;

import gmx.iderc.geoserver.tjs.catalog.*;
import gmx.iderc.geoserver.tjs.data.TJSDataAccessFactory;
import gmx.iderc.geoserver.tjs.data.TJSDataAccessFinder;
import gmx.iderc.geoserver.tjs.data.TJSDataStore;
import org.apache.commons.collections.MultiHashMap;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.NullProgressListener;

import java.util.*;

/**
 * @author root
 */
public class TJSCatalogImpl implements TJSCatalog {

//    private Catalog catalog;

    protected Map<String, FrameworkInfo> frameworks = new HashMap<String, FrameworkInfo>();
    protected Map<String, DataStoreInfo> dataStores = new HashMap<String, DataStoreInfo>();
    protected Map<String, DatasetInfo> dataSets = new HashMap<String, DatasetInfo>();
    //nuevo!, Alvaro Javier
    //aqui se guardan los joined maps con llave <FrameworkURI>+<GetDataURL>
    //hasta hora en la implementación no se tiene en cuenta el frameworkURI, que siempre viene vacío,
    //lo que hace que la llave sólo dependa del GetDataURL, :)
    protected Map<String, JoinedMapInfo> joinedMaps = new HashMap<String, JoinedMapInfo>();


    transient protected MultiHashMap storeDataSets = new MultiHashMap();
    transient protected MultiHashMap frameworkDataSets = new MultiHashMap();


    transient protected Map<String, TJSDataAccessFactory> dataStoreFactories = new HashMap<String, TJSDataAccessFactory>();


    public Catalog getGeoserverCatalog() {
        return (Catalog) GeoServerExtensions.bean("catalog");
    }

    public void add(FrameworkInfo frameworkInfo) {
//        if ( frameworkInfo.getWorkspace() == null ) {
//            frameworkInfo.setWorkspace( getCatalog().getDefaultWorkspace() );
//        }

//        validate(store, true);

        synchronized (frameworks) {
            frameworks.put(frameworkInfo.getName(), frameworkInfo);
        }
    }

    public void remove(FrameworkInfo frameworkInfo) {
        synchronized (frameworks) {
            frameworks.remove(frameworkInfo.getName());
        }
    }

    public void save() {
        for (DataStoreInfo dataStore : dataStores.values()) {
            TJSDataStore dst = dataStore.getTJSDataStore(new NullProgressListener());
            Map parameters = dst.getDataStoreFactory().filterParamsForSave(dataStore.getConnectionParameters());
            Map targetParams = dataStore.getConnectionParameters();
            targetParams.clear();
            targetParams.putAll(parameters);
        }
        TJSCatalogPersistence.save(this);
    }

    @Override
    public void add(JoinedMapInfo joinedMapInfo) {
        synchronized (joinedMaps) {
            joinedMaps.put(joinedMapInfo.getFrameworkURI() + "+" + joinedMapInfo.getGetDataURL(), joinedMapInfo);
        }
    }

    @Override
    public void remove(JoinedMapInfo joinedMapInfo) {
        synchronized (joinedMaps) {
            joinedMaps.remove(joinedMapInfo.getFrameworkURI() + "+" + joinedMapInfo.getGetDataURL());
        }
    }

    @Override
    public void save(JoinedMapInfo joinedMapInfo) {
        synchronized (joinedMaps) {
            joinedMaps.put(joinedMapInfo.getFrameworkURI() + "+" + joinedMapInfo.getGetDataURL(), joinedMapInfo);
        }
        save();
    }

    @Override
    public JoinedMapInfo getJoinedMap(String id) {
        for (JoinedMapInfo joinedMap : joinedMaps.values()) {
            if (joinedMap.getId().equals(id)) {
                return joinedMap;
            }
        }
        return null;
    }

    @Override
    public List<JoinedMapInfo> getJoinedMaps() {
        return new ArrayList<JoinedMapInfo>(joinedMaps.values());
    }

    @Override
    public List<JoinedMapInfo> getJoinedMapsByGetDataURL(String getDataURL) {
        List<JoinedMapInfo> res = new ArrayList<JoinedMapInfo>();
        for (JoinedMapInfo joinedMap : joinedMaps.values()) {
            if (joinedMap.getGetDataURL().equals(getDataURL)) {
                res.add(joinedMap);
            }
        }
        return res;
    }

    @Override
    public List<JoinedMapInfo> getJoinedMapsByFrameworkURI(String frameworkURI) {
        List<JoinedMapInfo> res = new ArrayList<JoinedMapInfo>();
        for (JoinedMapInfo joinedMap : joinedMaps.values()) {
            if (joinedMap.getFrameworkURI().equals(frameworkURI)) {
                res.add(joinedMap);
            }
        }
        return res;
    }

    public void save(FrameworkInfo frameworkInfo) {
        frameworks.put(frameworkInfo.getName(), frameworkInfo);
        save();
    }

    public FrameworkInfo getFramework(String id) {
        for (Iterator<String> it = frameworks.keySet().iterator(); it.hasNext(); ) {
            String frname = it.next();
            FrameworkInfo framework = frameworks.get(frname);
            if (framework.getId().equals(id)) {
                return framework;
            }
        }
        return null;
    }

    public FrameworkInfo getFrameworkByUri(String uri) {
        for (Iterator<String> it = frameworks.keySet().iterator(); it.hasNext(); ) {
            String frname = it.next();
            FrameworkInfo framework = frameworks.get(frname);
            if (framework.getUri().equals(uri)) {
                return framework;
            }
        }
        return null;
    }

    public FrameworkInfo getFrameworkByName(String name) {
        return frameworks.get(name);
    }

    public List<FrameworkInfo> getFrameworks() {
        List<FrameworkInfo> frameworkList = new ArrayList<FrameworkInfo>();
        frameworkList.addAll(frameworks.values());
        return frameworkList;
    }

    public void add(DatasetInfo datasetInfo) {
        frameworkDataSets.put(datasetInfo.getFramework().getId(), datasetInfo);
        storeDataSets.put(datasetInfo.getDataStore().getId(), datasetInfo);
        dataSets.put(datasetInfo.getId(), datasetInfo);
    }


    public void remove(DatasetInfo datasetInfo) {
        frameworkDataSets.remove(datasetInfo.getFramework().getId(), datasetInfo);
        storeDataSets.remove(datasetInfo.getDataStore().getId(), datasetInfo);
        dataSets.remove(datasetInfo.getId());
    }

    public void save(DatasetInfo datasetInfo) {
        this.save();
//        getFramework(datasetInfo.getFrameworkId()).save(datasetInfo);
    }

    private List lookup(String key, MultiHashMap map) {
        ArrayList result = new ArrayList();
        if (map != null) {
            if (map.getCollection(key) != null) {
                result.addAll(map.getCollection(key));
            }
        }
        return result;
    }

    public DatasetInfo getDataset(String dataStoreId, String name) {
        List l = lookup(dataStoreId, storeDataSets);
        for (Iterator i = l.iterator(); i.hasNext(); ) {
            DatasetInfo dataSet = (DatasetInfo) i.next();
            if (name.equals(dataSet.getName())) {
                return dataSet;
            }
        }
        return null;
    }

    public List<DatasetInfo> getDatasets(String dataStoreId) {
        if (dataStoreId != null) {
            return (List<DatasetInfo>) lookup(dataStoreId, storeDataSets);
        } else {
            ArrayList<DatasetInfo> result = new ArrayList<DatasetInfo>();
            result.addAll(storeDataSets.values());
            return result;
        }
    }

    public List<NamespaceInfo> getNamespaces() {
        List<NamespaceInfo> geoserverNamespaces = getGeoserverCatalog().getNamespaces();
        //Aqui habr'ia que a~nadir los namespaces propios de TJS
        return geoserverNamespaces;
    }

    public void loadDefault() {
        FrameworkInfoImpl fi = (FrameworkInfoImpl) getFactory().newFrameworkInfo();
        fi.loadDefault();
        add(fi);
    }

    public TJSCatalogFactory getFactory() {
        return new TJSCatalogFactoryImpl(this);
    }

    public WorkspaceInfo getDefaultWorkspace() {
        return getGeoserverCatalog().getDefaultWorkspace();
    }

    public TJSCatalogObject getCatalogObject(String id) {
        TJSCatalogResourceListVisitor rlVisitor = new TJSCatalogResourceListVisitor();
        rlVisitor.visit(this);
        return rlVisitor.getObjectMap().get(id);
    }

    public void add(DataStoreInfo dataStoreInfo) {
        synchronized (dataStores) {
            dataStores.put(dataStoreInfo.getName(), dataStoreInfo);
        }
    }

    public void remove(DataStoreInfo dataStoreInfo) {
        synchronized (dataStores) {
            dataStores.remove(dataStoreInfo.getName());
        }
    }

    public void save(DataStoreInfo dataStoreInfo) {
        this.save();
    }

    public DatasetInfo getDatasetByUri(String uri) {
        for (FrameworkInfo frameworkInfo : getFrameworks()) {
            for (DatasetInfo datasetInfo : this.getDatasetsByFramework(frameworkInfo.getId())) {
                if (datasetInfo.getDatasetUri().equals(uri)) {
                    return datasetInfo;
                }
            }
        }
        return null;
    }

    public DataStoreInfo getDataStore(String id) {
        for (Iterator<String> it = dataStores.keySet().iterator(); it.hasNext(); ) {
            String frname = it.next();
            DataStoreInfo dataStoreInfo = dataStores.get(frname);
            if (dataStoreInfo.getId().equals(id)) {
                return dataStoreInfo;
            }
        }
        return null;
    }

    public DataStoreInfo getDataStoreByName(String name) {
        return dataStores.get(name);
    }

    public List<DataStoreInfo> getDataStores() {
        List<DataStoreInfo> dataStoreList = new ArrayList<DataStoreInfo>();
        dataStoreList.addAll(dataStores.values());
        return dataStoreList;
    }

    public TJSDataAccessFactory getDataStoreFactory(String name) {
        if (dataStoreFactories.containsKey(name)) {
            return dataStoreFactories.get(name);
        } else {

            for (Iterator<TJSDataAccessFactory> it = TJSDataAccessFinder.getAvailableDataStores(); it.hasNext(); ) {
                TJSDataAccessFactory factory = it.next();
                if (factory.getDisplayName().equals(name)) {
                    return factory;
                }
            }
            return null;
        }
    }

    public void init() {
        if (frameworks == null) {
            frameworks = new HashMap<String, FrameworkInfo>();
        }
        if (dataStores == null) {
            dataStores = new HashMap<String, DataStoreInfo>();
        }
        if (dataStoreFactories == null) {
            dataStoreFactories = new HashMap<String, TJSDataAccessFactory>();
        }
        if (storeDataSets == null) {
            storeDataSets = new MultiHashMap();
        }
        if (frameworkDataSets == null) {
            frameworkDataSets = new MultiHashMap();
        }
        //Assign catalog as Parent to all objects when load;
        for (FrameworkInfo framework : frameworks.values()) {
            framework.setCatalog(this);
        }
        for (DataStoreInfo dataStore : dataStores.values()) {
            dataStore.setCatalog(this);
        }
        for (DatasetInfo dataset : dataSets.values()) {
            dataset.setCatalog(this);
        }
        updateDatasetsIndex();
    }

    private void updateDatasetsIndex() {
        // Thijs: add a try/catch to avoid errors reloading the confix.xml if JoinData requests have been processed
        for (DatasetInfo dataSet : dataSets.values()) {
            try {
                storeDataSets.put(dataSet.getDataStore().getId(), dataSet);
                frameworkDataSets.put(dataSet.getFramework().getId(), dataSet);
            } catch (Exception e) {
                // ??
            }
        }
    }

    public List<DatasetInfo> getDatasetsByFramework(String frameworkId) {
        return (List<DatasetInfo>) lookup(frameworkId, frameworkDataSets);
    }

    public DatasetInfo getDatasetByFramework(String frameworkId, String name) {
        List l = lookup(frameworkId, frameworkDataSets);
        for (Iterator i = l.iterator(); i.hasNext(); ) {
            DatasetInfo dataSet = (DatasetInfo) i.next();
            if (name.equals(dataSet.getName())) {
                return dataSet;
            }
        }
        return null;
    }

    private class TJSCatalogResourceListVisitor implements TJSCatalogVisitor {
        HashMap<String, TJSCatalogObject> allObj = new HashMap<String, TJSCatalogObject>();
        TJSCatalog catalog;


        public void visit(TJSCatalog catalog) {
            this.catalog = catalog;
            for (Iterator<FrameworkInfo> it = catalog.getFrameworks().iterator(); it.hasNext(); ) {
                FrameworkInfo object = it.next();
                visit(object);
            }
            for (Iterator<DataStoreInfo> it = catalog.getDataStores().iterator(); it.hasNext(); ) {
                DataStoreInfo object = it.next();
                visit(object);
            }
        }

        public void visit(FrameworkInfo framework) {
            allObj.put(framework.getId(), framework);
        }

        public void visit(DataStoreInfo dataStore) {
            allObj.put(dataStore.getId(), dataStore);
            for (Iterator<DatasetInfo> it = catalog.getDatasets(dataStore.getId()).iterator(); it.hasNext(); ) {
                DatasetInfo object = it.next();
                visit(object);
            }
        }

        public void visit(DatasetInfo dataset) {
            allObj.put(dataset.getId(), dataset);
        }

        @Override
        public void visit(JoinedMapInfo joinedMap) {
            //nada por ahora, Alvaro Javier
        }

        public Map<String, TJSCatalogObject> getObjectMap() {
            return allObj;
        }

        public void visit(TJSCatalogObject object) {
            //throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    ;

}
