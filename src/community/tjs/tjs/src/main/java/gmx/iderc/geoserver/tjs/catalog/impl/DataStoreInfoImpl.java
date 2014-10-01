/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.catalog.impl;

import gmx.iderc.geoserver.tjs.catalog.DataStoreInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalogVisitor;
import gmx.iderc.geoserver.tjs.data.TJSDataAccessFactory;
import gmx.iderc.geoserver.tjs.data.TJSDataAccessFinder;
import gmx.iderc.geoserver.tjs.data.TJSDataStore;
import org.geoserver.catalog.MetadataMap;
import org.geotools.util.ProgressListener;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author capote
 */
public class DataStoreInfoImpl extends TJSCatalogObjectImpl implements DataStoreInfo, Serializable {

    String type;

    protected Map<String, Serializable> connectionParameters = new HashMap<String, Serializable>();

    protected MetadataMap metadata = new MetadataMap();

    transient protected Throwable error;

    transient TJSDataStore dataStore;

    //TODO: sobreescribir aqui no hace falta?
    //esto lo puse aqui para ser consecuente con FrameworkInfoImpl y DatsetInfoImpl que tambien lo hacen
    //Alvaro Javier Fuentes Suarez, 11:28 p.m. 1/8/13
    @Override
    public void accept(TJSCatalogVisitor visitor) {
        visitor.visit((DataStoreInfo) this);
    }

    public DataStoreInfoImpl(TJSCatalog catalog) {
        super(catalog);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public MetadataMap getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataMap metadata) {
        this.setMetadata(metadata);
    }

    public static TJSDataAccessFactory aquireFactory(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (Iterator<TJSDataAccessFactory> i = TJSDataAccessFinder.getAvailableDataStores(); i.hasNext(); ) {
            TJSDataAccessFactory factory = i.next();
//            initializeDataStoreFactory( factory );
            if (displayName.equals(factory.getDisplayName())) {
                return factory;
            }

            if (displayName.equals(factory.getClass().toString())) {
                return factory;
            }
        }

        return null;
    }

    public static TJSDataAccessFactory aquireFactory(Map params) {
        for (Iterator<TJSDataAccessFactory> i = TJSDataAccessFinder.getAvailableDataStores(); i.hasNext(); ) {
            TJSDataAccessFactory factory = i.next();
//            initializeDataStoreFactory( factory );

            if (factory.canProcess(params)) {
                return factory;
            }
        }

        return null;
    }

    public TJSDataAccessFactory getDataStoreFactory(DataStoreInfo info) throws IOException {

        TJSDataAccessFactory factory = null;

        if (info.getType() != null) {
            factory = aquireFactory(info.getType());
        }

        if (factory == null) {
            factory = aquireFactory(info.getConnectionParameters());
        }

        return factory;
    }

    public TJSDataStore getTJSDataStore(ProgressListener listener) {
        Map params = getConnectionParameters();

        if (dataStore != null) {
            return dataStore;
        }
        try {

            TJSDataAccessFactory factory = getDataStoreFactory(this);
            dataStore = factory.createDataStore(params);

            if (dataStore == null) {
                throw new NullPointerException("Could not acquire data access '" + getName() + "'");
            }
        } catch (IOException ex) {
            Logger.getLogger(DataStoreInfoImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return dataStore;
    }

    public void setDataStore(TJSDataStore dataStore) {
        this.dataStore = dataStore;
    }

    /*
            private void updateNewDataSets(TJSDataStore dataStore){
                TJSCatalogFactory factory = getCatalog().getFactory();
                for(String dataSourceName : dataStore.getDatasourceNames()){
                    DatasetInfo dset = factory.newDataSetInfo();
                    dset.setName(dataSourceName);
                    dset.setPublished(false);
                }
            }

            private void updateSavedDataSets(TJSDataStore dataStore){
                String[] dsNames = dataStore.getDatasourceNames();
                List<String> dsNameList = Arrays.asList(dsNames);

                for(DatasetInfo dset : getDatasets()){
                    if (!dsNameList.contains(dset.getName())){
                        datasets.remove(dset.getName());
                    }
                }
            }
        */
    public Map<String, Serializable> getConnectionParameters() {
        return connectionParameters;
    }

    public void setConnectionParameters(Map<String, Serializable> connectionParameters) {
        this.connectionParameters = connectionParameters;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable t) {
        this.error = t;
    }
/*
    public DatasetInfo getDataset(String name) {
        if (datasets != null){
            if (datasets.containsKey(id)){
                return datasets.get(id);
            }
        }
        return null;
    }

    public List<DatasetInfo> getDatasets() {
        ArrayList<DatasetInfo> retDataSets = new ArrayList<DatasetInfo>();
        if (datasets != null){
            retDataSets.addAll(datasets.values());
        }
        return retDataSets;
    }
 */

}
