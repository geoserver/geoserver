/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.data.jdbc.mysql;

import gmx.iderc.geoserver.tjs.data.jdbc.JDBC_TJSDataStore;
import gmx.iderc.geoserver.tjs.data.jdbc.JDBC_TJSDataStoreFactory;

import java.util.Map;

/**
 * @author root
 */
public class MySQL_TJSDataStore extends JDBC_TJSDataStore {

    public MySQL_TJSDataStore(Map params, JDBC_TJSDataStoreFactory factory) {
        super(params, factory);
    }

}
