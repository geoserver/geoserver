/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.data;

import java.io.IOException;
import java.util.Map;

/**
 * @author root
 */
public interface TJSDataStoreFactorySpi extends TJSDataAccessFactory {

    /**
     * Construct a live data source using the params specifed.
     * <p/>
     * <p>
     * You can think of this as setting up a connection to the back end data
     * source.
     * </p>
     * <p/>
     * <p>
     * Magic Params: the following params are magic and are honoured by
     * convention by the GeoServer and uDig application.
     * <p/>
     * <ul>
     * <li>
     * "user": is taken to be the user name
     * </li>
     * <li>
     * "passwd": is taken to be the password
     * </li>
     * <li>
     * "namespace": is taken to be the namespace prefix (and will be kept in
     * sync with GeoServer namespace management.
     * </li>
     * </ul>
     * <p/>
     * When we eventually move over to the use of OpperationalParam we will
     * have to find someway to codify this convention.
     * </p>
     *
     * @param params The full set of information needed to construct a live
     *               data store. Typical key values for the map include: url -
     *               location of a resource, used by file reading datasources. dbtype
     *               - the type of the database to connect to, e.g. postgis, mysql
     * @return The created TJSDataStore, this may be null if the required resource
     *         was not found or if insufficent parameters were given. Note
     *         that canProcess() should have returned false if the problem is
     *         to do with insuficent parameters.
     * @throws IOException if there were any problems setting up (creating or
     *                     connecting) the datasource.
     */
    TJSDataStore createDataStore(Map params) throws IOException;

}
