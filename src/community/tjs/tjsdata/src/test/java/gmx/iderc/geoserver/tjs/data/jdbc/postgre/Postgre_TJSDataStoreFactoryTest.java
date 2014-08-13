/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.data.jdbc.postgre;

import gmx.iderc.geoserver.tjs.data.TJSDataStore;
import gmx.iderc.geoserver.tjs.data.TJSDatasource;
import gmx.iderc.geoserver.tjs.data.jdbc.JDBC_TJSDataStoreFactory;
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
 * @author root
 */
public class Postgre_TJSDataStoreFactoryTest extends TestCase {

    public Postgre_TJSDataStoreFactoryTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testCreateDataStore() throws Exception {
        Postgre_TJSDataStoreFactory factory = new Postgre_TJSDataStoreFactory();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(JDBC_TJSDataStoreFactory.HOST.key, "localhost");
        params.put(JDBC_TJSDataStoreFactory.DATABASE.key, "tjs");
        params.put(JDBC_TJSDataStoreFactory.MAXCONN.key, 10);
        params.put(JDBC_TJSDataStoreFactory.MINCONN.key, 1);
        params.put(JDBC_TJSDataStoreFactory.DBTYPE.key, "PostgreSQL");
        params.put(JDBC_TJSDataStoreFactory.PORT.key, 5432);
        params.put(JDBC_TJSDataStoreFactory.SCHEMA.key, "public");
        params.put(JDBC_TJSDataStoreFactory.USER.key, "postgres");
        params.put(JDBC_TJSDataStoreFactory.PASSWD.key, "pgadmin");
        params.put(JDBC_TJSDataStoreFactory.DATASOURCENAME.key, "poblacionprovincias");
        TJSDataStore tjsds = factory.createDataStore(params);
        TJSDatasource ds = tjsds.getDatasource("poblacionprovincias", params);
        RowSet rst = ds.getRowSet();
        ResultSetMetaData meta = ds.getResultSetMetaData();
        serialize(rst, meta);

        FilterFactory2 filterFactory = CommonFactoryFinder.getFilterFactory2(null);
        Filter filter = filterFactory.equals(filterFactory.property("field27032012_042150"), filterFactory.literal(new String("2423.00")));

        System.out.println("Filtered rowset");
        rst = ds.getRowSet(filter);
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

    public void testGetDescription() {

    }


}
