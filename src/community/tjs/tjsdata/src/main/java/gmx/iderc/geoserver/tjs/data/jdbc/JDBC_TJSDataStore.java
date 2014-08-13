/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gmx.iderc.geoserver.tjs.data.jdbc;

import gmx.iderc.geoserver.tjs.data.TJSAbstractDataStore;
import gmx.iderc.geoserver.tjs.data.TJSDatasource;
import org.apache.commons.dbcp.BasicDataSource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author root
 */
public abstract class JDBC_TJSDataStore extends TJSAbstractDataStore {

    Map params;
    BasicDataSource dataSource;
    JDBC_TJSDataStoreFactory factory;

    Connection connection = null;

    public JDBC_TJSDataStore(Map params, JDBC_TJSDataStoreFactory factory) {
        super(params, factory);
        this.params = params;
        this.factory = factory;
        init();
    }

    private void init() {
        try {
            dataSource = (BasicDataSource) factory.DATASOURCE.lookUp(params);
            if (dataSource == null) {
                dataSource = factory.createDataSource(params);
                getConnection();
            }
        } catch (IOException ex) {
            Logger.getLogger(JDBC_TJSDataStore.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private Connection getConnection() {
        if (connection != null) {
            return connection;
        }
        try {
            connection = dataSource.getConnection();
        } catch (Exception error) {
            Logger.getLogger(JDBC_TJSDataStore.class.getName(), error.getMessage());
        }
        return connection;
    }

    @Override
    public String[] getAllAvaliableDatasources() {
        ArrayList<String> tableList = new ArrayList<String>();
        try {
            ResultSet tables = getConnection().getMetaData().getTables(null, null, null, new String[]{"TABLE", "VIEW"});
            while (tables.next()) {
                int ischema = tables.findColumn("TABLE_SCHEM");
                String sschema = tables.getString(ischema);
                int iname = tables.findColumn("TABLE_NAME");
                String sname = tables.getString(iname);
                // Thijs Brentjens: to get the featuretypename working, without the database schema, don't add the schema to the featuretypename
                // Therefore: don't add the schema to the tablelist, but just add the name
                //
                // TODO: improve handling of the schema in tablenames for the GDAS cache?
                // System.out.println("In getAllAvaliableDatasources: " + sschema + "." +sname);
                /*
                if (sschema == null){
//                    tableList.add(sname.toUpperCase());
                    tableList.add(sname);
                }else{
                    tableList.add(sschema + "." + sname);
                } */
                tableList.add(sname);
            }
            tables.close();
        } catch (SQLException ex) {
            Logger.getLogger(JDBC_TJSDataStore.class.getName()).log(Level.SEVERE, null, ex);
        }
        return tableList.toArray(new String[tableList.size()]);
    }

    protected TJSDatasource createDataSource(Map params) throws Exception {
        JDBC_TJSDatasource ds = new JDBC_TJSDatasource(dataSource, params);
        return ds;
    }

    @Override
    public TJSDatasource getDatasource(String name, Map params) {
        try {
            String paramsDsName = (String)JDBC_TJSDataStoreFactory.DATASOURCENAME.lookUp(params);
            if (!paramsDsName.equalsIgnoreCase(name)){
                params.put(JDBC_TJSDataStoreFactory.DATASOURCENAME.key, name);
            }
        } catch (IOException ex) {
            params.put(JDBC_TJSDataStoreFactory.DATASOURCENAME.key, name);
        }
        return super.getDatasource(name, params);
    }

}
