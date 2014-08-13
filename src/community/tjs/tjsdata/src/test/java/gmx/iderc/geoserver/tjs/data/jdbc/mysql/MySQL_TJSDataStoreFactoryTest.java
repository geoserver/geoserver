package gmx.iderc.geoserver.tjs.data.jdbc.mysql;

import gmx.iderc.geoserver.tjs.data.TJSDataStore;
import gmx.iderc.geoserver.tjs.data.TJSDatasource;
import gmx.iderc.geoserver.tjs.data.jdbc.JDBC_TJSDataStoreFactory;
import gmx.iderc.geoserver.tjs.data.jdbc.postgre.Postgre_TJSDataStoreFactory;
import junit.framework.TestCase;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import javax.sql.RowSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 2/05/13
 * Time: 15:14
 * To change this template use File | Settings | File Templates.
 */
public class MySQL_TJSDataStoreFactoryTest extends TestCase {

    public void testCreateDataStore() throws Exception {
        MySQL_TJSDataStoreFactory factory = new MySQL_TJSDataStoreFactory();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(JDBC_TJSDataStoreFactory.HOST.key, "localhost");
        params.put(JDBC_TJSDataStoreFactory.DATABASE.key, "elam");
        params.put(JDBC_TJSDataStoreFactory.MAXCONN.key, 10);
        params.put(JDBC_TJSDataStoreFactory.MINCONN.key, 1);
        params.put(JDBC_TJSDataStoreFactory.DBTYPE.key, "MySQL");
        params.put(JDBC_TJSDataStoreFactory.PORT.key, 3306);
        params.put(JDBC_TJSDataStoreFactory.SCHEMA.key, "");
        params.put(JDBC_TJSDataStoreFactory.USER.key, "root");
        params.put(JDBC_TJSDataStoreFactory.PASSWD.key, "");
        params.put(JDBC_TJSDataStoreFactory.DATASOURCENAME.key, "estudiantes");
        TJSDataStore tjsds = factory.createDataStore(params);
        TJSDatasource ds = tjsds.getDatasource("estudiantes", params);
        RowSet rst = ds.getRowSet();
        ResultSetMetaData meta = ds.getResultSetMetaData();
        serialize(rst, meta);
    }

    void serialize(RowSet rst, ResultSetMetaData meta) throws SQLException {
        int ccount = meta.getColumnCount();
        System.out.println("<rowset>");
        while (rst.next()) {
            System.out.println("\t<row>");
            for (int index = 1; index <= ccount; index++) {
                String row = "\t\t<" + meta.getColumnName(index) + ">" + rst.getString(index) + "</" + meta.getColumnName(index) + ">";
                System.out.println(row);
            }
            System.out.println("\t</row>");
        }
        System.out.println("</rowset>");
    }

}
