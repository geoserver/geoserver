/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.catalog;

import gmx.iderc.geoserver.tjs.TJSTestSupport;
import junit.framework.Test;

/**
 * @author root
 */
public class TJSCatalogPersistenceTest extends TJSTestSupport {

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new TJSCatalogPersistenceTest());
    }

    /* Thijs Brentjens (Geonovum): FIXME compilation errors of this test
    WORKAROUND: exclude this test
    
    public void testLoad() throws Exception {
        TJSCatalog cat = TJSExtensionTestSupport.getTJSCatalog(getTestPersistenceDirectory());
        assertNotNull(cat);
    }*/

}
