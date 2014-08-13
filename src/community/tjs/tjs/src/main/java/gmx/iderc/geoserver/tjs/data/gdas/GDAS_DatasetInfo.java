package gmx.iderc.geoserver.tjs.data.gdas;

import gmx.iderc.geoserver.tjs.catalog.*;
import gmx.iderc.geoserver.tjs.catalog.impl.DataStoreInfoImpl;
import gmx.iderc.geoserver.tjs.data.ReadonlyDatasetInfo;
import gmx.iderc.geoserver.tjs.data.TJSDataStore;
import gmx.iderc.geoserver.tjs.data.TJSDatasource;
import gmx.iderc.geoserver.tjs.data.jdbc.JDBC_TJSDataStoreFactory;
import gmx.iderc.geoserver.tjs.data.jdbc.hsql.HSQLDB_GDAS_Cache;
import net.opengis.tjs10.ColumnType;
import net.opengis.tjs10.ColumnType1;
import net.opengis.tjs10.ColumnType2;
import net.opengis.tjs10.GDASType;
import org.geotools.feature.NameImpl;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.type.Name;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 29/07/13
 * Time: 11:24
 * To change this template use File | Settings | File Templates.
 */
public class GDAS_DatasetInfo extends ReadonlyDatasetInfo {

    // avoid that these types are serialized ?
    // use transient or not?
    transient GDASType gdasType;
    DataStoreInfo dataStoreInfo;

    String tableName;

    /* public GDAS_DatasetInfo() {
        // dummy implementation to avoid serialization
    }  */

    public GDAS_DatasetInfo(GDASType gdasType, TJSCatalog catalog, String url) {
        this.tjsCatalog = catalog;
        this.gdasType = gdasType;

        // TODO: use something else than the tablename?
        tableName = HSQLDB_GDAS_Cache.importGDAS(gdasType, url);
        DataStoreInfo dataStore = catalog.getDataStore("gdas_cache");
        if (dataStore == null){
            DataStoreInfoImpl dataStoreInfo = new DataStoreInfoImpl(catalog);
            dataStoreInfo.setId("gdas_cache");
            dataStoreInfo.setDataStore(HSQLDB_GDAS_Cache.getCacheDataStore());
            setDataStore(dataStoreInfo);
        }else{
            setDataStore(dataStore);
        }
    }

    @Override
    public void setDataStore(DataStoreInfo dataStoreInfo) {
        this.dataStoreInfo = dataStoreInfo;
    }

    @Override
    public DataStoreInfo getDataStore() {
        return dataStoreInfo;
    }

    @Override
    public FrameworkInfo getFramework() {
        // Thijs: improve because of reloading config.xml
       try {
           return tjsCatalog.getFrameworkByUri(gdasType.getFramework().getFrameworkURI());
       } catch (Exception e) {
           return null;
       }
    }

    @Override
    public String getDatasetUri() {
        return gdasType.getFramework().getDataset().getDatasetURI();  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getDatasetName() {
        return gdasType.getFramework().getDataset().getTitle();
    }

    @Override
    public String getGeoKeyField() {
        ColumnType2 column = (ColumnType2)gdasType.getFramework().getDataset().getColumnset().getFrameworkKey().getColumn().get(0);
        /*ColumnType column = (ColumnType)gdasType.getFramework().getFrameworkKey().getColumn().get(0);*/
        return column.getName();
    }

    @Override
    public String getOrganization() {
        return gdasType.getFramework().getOrganization();
    }

    @Override
    public Date getReferenceDate() {
        //Hay que ver cómo se parsea el valor de ReferenceDate para asignarlo al tipo Date que se requiere
        return new Date();//gdasType.getFramework().getReferenceDate().;
    }

    @Override
    public String getVersion() {
        return gdasType.getFramework().getVersion();
    }

    @Override
    public String getDocumentation() {
        return gdasType.getFramework().getDocumentation();
    }

    @Override
    public TJSDatasource getTJSDatasource() {
        TJSDataStore tjsDataStore = getDataStore().getTJSDataStore(new NullProgressListener());
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(JDBC_TJSDataStoreFactory.DATASOURCENAME.key, tableName);
        return tjsDataStore.getDatasource(tableName, params);
    }

    HashMap<String, ColumnInfo> columns = new HashMap<String, ColumnInfo>();

    @Override
    public List<ColumnInfo> getColumns() {
        if (columns.isEmpty()){
            for (int index = 0; index < gdasType.getFramework().getDataset().getColumnset().getAttributes().getColumn().size(); index++){
                ColumnType1 column = (ColumnType1)gdasType.getFramework().getDataset().getColumnset().getAttributes().getColumn().get(index);
                columns.put(column.getName(), new GDAS_ColumnInfo(column));
            }
        }
        return new ArrayList<ColumnInfo>(columns.values());
    }

    @Override
    public ColumnInfo getColumn(String name) {
        return columns.get(name);
    }

    @Override
    public String getDefaultStyle() {
        DatasetInfo hostedDataset = tjsCatalog.getDatasetByUri(gdasType.getFramework().getDataset().getDatasetURI());
        if (hostedDataset != null){
            return hostedDataset.getDefaultStyle();
        }
        return null;
    }

    @Override
    public boolean getAutoJoin() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getId() {
        return "";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setId(String id) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getName() {
        return tableName;
    }

    @Override
    public String getDescription() {
        return gdasType.getFramework().getDataset().getAbstract().toString();  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean getEnabled() {
        return true;
    }

    @Override
    public Name getQualifiedName() {
        return new NameImpl(getName());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void accept(TJSCatalogVisitor visitor) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void loadDefault() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TJSCatalog getCatalog() {
        return tjsCatalog;
    }
}
