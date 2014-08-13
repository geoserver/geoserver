/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.data;

import org.geotools.data.ServiceInfo;

import java.util.Map;

/**
 * @author Jos'e Luis Capote
 */
public interface TJSDataStore {

    ServiceInfo getInfo();

    TJSDataAccessFactory getDataStoreFactory();

    String[] getAllAvaliableDatasources();

    String[] getDatasourceNames();

    TJSDatasource getDatasource(String name, Map params);

}
