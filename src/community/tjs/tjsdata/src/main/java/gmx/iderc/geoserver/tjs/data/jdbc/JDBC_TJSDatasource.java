/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.data.jdbc;

import com.sun.rowset.CachedRowSetImpl;
import gmx.iderc.geoserver.tjs.data.TJSDatasource;
import org.apache.commons.dbcp.BasicDataSource;
import org.opengis.filter.Filter;

import javax.sql.RowSet;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author root
 */
public class JDBC_TJSDatasource implements TJSDatasource {

    BasicDataSource dataSource;
    Map params;
    String dataSourceName;
    Connection connection;

    public JDBC_TJSDatasource(BasicDataSource dataSource, Map params) {
        this.dataSource = dataSource;
        this.params = params;
        //buscar el nombre del data source si no no adelantamos nada, Alvaro Javier Fuentes Suarez
        this.dataSourceName = (String) params.get("Data Source Name");
    }

    private Connection getConnection() {
        if (connection != null) {
            return connection;
        }
        try {
            connection = dataSource.getConnection();
        } catch (Exception error) {
            Logger.getLogger(JDBC_TJSDatasource.class.getName(), error.getMessage());
        }
        return connection;
    }

    public RowSet getRowSet() throws SQLException {
        try {
            CachedRowSetImpl cachedRowSet = new CachedRowSetImpl();
            cachedRowSet.setCommand(getSQL());
            cachedRowSet.execute(getConnection());
            return cachedRowSet;
        } catch (IOException ex) {
            Logger.getLogger(JDBC_TJSDatasource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public RowSet getRowSet(Filter filter) throws SQLException {
        RowSet rst = getRowSet();
        if (filter != null) {//ojo!!! el filtro no puede ser null poruqe explota!!!!, Alvaro Javier
            while (rst.next()) {
                if (!filter.evaluate(rst)) {
                    rst.deleteRow();
                }
            }
        }
        rst.beforeFirst();
        return rst;
    }

    public String[] getFields() {
        ArrayList<String> fieldList = new ArrayList<String>();
        try {
            //buscar el esquema y la tabla, Alvaro Javier Fuentes Suarez
            String parts[] = dataSourceName.split("\\.");
            String schema, tableName;
            if (parts.length == 1) {
                schema = null;
                tableName = parts[0];
            } else {
                schema = parts[0];
                tableName = parts[1];
            }
            //a la hora de buscar los campos usar el esquema y la tabla deseada,
            //Alvaro Javier Fuentes Suarez
            ResultSet fields = getConnection().getMetaData().getColumns(null, schema, tableName, null);
            while (fields.next()) {
                int iname = fields.findColumn("COLUMN_NAME");
                String sname = fields.getString(iname);
                fieldList.add(sname);
            }
            fields.close();
        } catch (SQLException ex) {
            Logger.getLogger(JDBC_TJSDataStore.class.getName()).log(Level.SEVERE, null, ex);
        }
        return fieldList.toArray(new String[fieldList.size()]);
    }

    private String getSQL() throws IOException {
        String dsName = (String) JDBC_TJSDataStoreFactory.DATASOURCENAME.lookUp(params);
        return "SELECT * FROM " + dsName;
    }

    public ResultSetMetaData getResultSetMetaData() throws SQLException {
        RowSet rst = getRowSet();
        ResultSetMetaData meta = rst.getMetaData();
        rst.close();
        return meta;
    }

}
