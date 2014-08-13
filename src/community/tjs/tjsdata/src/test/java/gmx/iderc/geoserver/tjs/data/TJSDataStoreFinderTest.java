/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.data;

import junit.framework.TestCase;

import java.util.Iterator;

/**
 * @author root
 */
public class TJSDataStoreFinderTest extends TestCase {

    public TJSDataStoreFinderTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testGetDataStore() throws Exception {
    }

    public void testGetAllDataStores() {
        for (Iterator<TJSDataStoreFactorySpi> stores = TJSDataStoreFinder.getAllDataStores(); stores.hasNext(); ) {
            TJSDataStoreFactorySpi store = stores.next();
            System.out.println(store.getDisplayName());
            System.out.println(store.getDescription());
        }
    }

    public void testGetAvailableDataStores() {
    }

    public void testScanForPlugins() {
    }

    public void testReset() {
    }

}
