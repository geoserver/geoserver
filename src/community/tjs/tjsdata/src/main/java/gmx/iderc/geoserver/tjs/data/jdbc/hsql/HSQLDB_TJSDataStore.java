/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.data.jdbc.hsql;

import gmx.iderc.geoserver.tjs.data.jdbc.JDBC_TJSDataStore;
import gmx.iderc.geoserver.tjs.data.jdbc.JDBC_TJSDataStoreFactory;

import java.util.Map;

/**
 * @author root
 */
public class HSQLDB_TJSDataStore extends JDBC_TJSDataStore {

    public HSQLDB_TJSDataStore(Map params, JDBC_TJSDataStoreFactory factory) {
        super(params, factory);
    }

}
