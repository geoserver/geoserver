package org.geoserver.h2;

import java.util.HashMap;

import org.geoserver.test.GeoServerTestSupport;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.h2.H2DataStoreFactory;
import org.vfny.geoserver.util.DataStoreUtils;

public class H2DataStoreInitializerTest extends GeoServerTestSupport {

    public void testDataStoreFactoryInitialized() {
        HashMap params = new HashMap();
        params.put( H2DataStoreFactory.DBTYPE.key, "h2");
        params.put( H2DataStoreFactory.DATABASE.key, "test" );
        
        DataAccessFactory f = DataStoreUtils.aquireFactory( params );
        assertNotNull( f );
        assertTrue( f instanceof H2DataStoreFactory );
        
        assertEquals( testData.getDataDirectoryRoot(), ((H2DataStoreFactory)f).getBaseDirectory() );
        
    }
}
