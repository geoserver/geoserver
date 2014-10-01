/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.catalog;

import gmx.iderc.geoserver.tjs.data.TJSDataAccessFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;

import java.util.List;

/**
 * @author root
 */
public interface TJSCatalog {

    TJSCatalogFactory getFactory();

    void save();

    void add(JoinedMapInfo joinedMapInfo);

    void remove(JoinedMapInfo joinedMapInfo);

    void save(JoinedMapInfo joinedMapInfo);

    JoinedMapInfo getJoinedMap(String id);

    List<JoinedMapInfo> getJoinedMaps();

    List<JoinedMapInfo> getJoinedMapsByGetDataURL(String getDataURL);

    List<JoinedMapInfo> getJoinedMapsByFrameworkURI(String frameworkURI);

    void add(FrameworkInfo frameworkInfo);

    void remove(FrameworkInfo frameworkInfo);

    void save(FrameworkInfo frameworkInfo);

    FrameworkInfo getFramework(String id);

    FrameworkInfo getFrameworkByUri(String Uri);

    FrameworkInfo getFrameworkByName(String name);

    List<FrameworkInfo> getFrameworks();

    void add(DataStoreInfo dataStoreInfo);

    void remove(DataStoreInfo dataStoreInfo);

    void save(DataStoreInfo dataStoreInfo);

    <T extends DataStoreInfo> T getDataStore(String id);

    <T extends DataStoreInfo> T getDataStoreByName(String name);

    <T extends DataStoreInfo> List<T> getDataStores();

    void add(DatasetInfo datasetInfo);

    void remove(DatasetInfo datasetInfo);

    void save(DatasetInfo datasetInfo);

    DatasetInfo getDataset(String dataStoreId, String name);

    List<DatasetInfo> getDatasets(String dataStoreId);

    List<DatasetInfo> getDatasetsByFramework(String frameworkId);

    <T extends DatasetInfo> T getDatasetByUri(String uri);

    DatasetInfo getDatasetByFramework(String frameworkId, String name);


    List<NamespaceInfo> getNamespaces();

    WorkspaceInfo getDefaultWorkspace();

    TJSCatalogObject getCatalogObject(String id);

    Catalog getGeoserverCatalog();

    void init();

    TJSDataAccessFactory getDataStoreFactory(String name);
}
