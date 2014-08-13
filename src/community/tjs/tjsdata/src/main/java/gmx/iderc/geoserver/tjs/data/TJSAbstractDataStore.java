/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.data;

import org.apache.commons.lang.ArrayUtils;
import org.geotools.data.DefaultServiceInfo;
import org.geotools.data.ServiceInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author root
 */
public abstract class TJSAbstractDataStore implements TJSDataStore {

    protected TJSDataAccessFactory dataStoreFactory;
    HashMap<String, TJSDatasource> tjsDatasources = new HashMap<String, TJSDatasource>();

    public TJSAbstractDataStore(Map params, TJSDataStoreFactorySpi factory) {
        dataStoreFactory = factory;
    }

    public ServiceInfo getInfo() {
        DefaultServiceInfo info = new DefaultServiceInfo();
        info.setDescription("Data from " + getClass().getSimpleName());
        return info;
    }


    /**
     * Returns all avaliable Datasource ins a Data Store.
     *
     * @return The String array with the Datasource Names or a empty array.
     */
    public abstract String[] getAllAvaliableDatasources();

    /**
     * Returns the factory used to create the data store.
     *
     * @return The data store factory, possibly <code>null</code>.
     */
    public TJSDataAccessFactory getDataStoreFactory() {
        return dataStoreFactory;
    }

    /**
     * Sets the data store factory used to create the datastore.
     * <p>
     * WARNING: This property should only be set in cases where the datastore factory is
     * stateless and does not maintain any references to created datastores. Setting this
     * property in such a case will result in a memory leak.
     * </p>
     */
    public void setDataStoreFactory(TJSDataStoreFactorySpi dataStoreFactory) {
        this.dataStoreFactory = dataStoreFactory;
    }

    public TJSDatasource getDatasource(String name, Map params) {
        if (tjsDatasources.containsKey(name)) {
            return tjsDatasources.get(name);
        }
        if (ArrayUtils.contains(getAllAvaliableDatasources(), name)) {
            try {
                TJSDatasource dataSource = createDataSource(params);
                if (dataSource != null) {
                    addDatasource(name, dataSource);
                    return dataSource;
                }
            } catch (Exception ex) {
                Logger.getLogger(TJSDatasource.class.getName()).log(Level.SEVERE, ex.getMessage());
            }
        }
        return null;
    }

    protected abstract TJSDatasource createDataSource(Map params) throws Exception;

    public String[] getDatasourceNames() {
        int length = tjsDatasources.size();
        return tjsDatasources.keySet().toArray(new String[length]);
    }

    public void addDatasource(String name, TJSDatasource dataSource) {
        tjsDatasources.put(name, dataSource);
    }

}
