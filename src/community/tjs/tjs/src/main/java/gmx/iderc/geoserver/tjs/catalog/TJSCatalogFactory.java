/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.catalog;

/**
 * @author root
 */
public interface TJSCatalogFactory {

    public FrameworkInfo newFrameworkInfo();

    public DatasetInfo newDataSetInfo();

    public DataStoreInfo newDataStoreInfo();

}
