package org.geoserver.config;

import junit.framework.TestCase;

import org.geoserver.config.impl.GeoServerFactoryImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.LegacyConfigurationImporter;
import org.geotools.data.DataUtilities;

public class LegacyConfigurationImporterTest extends TestCase {

    LegacyConfigurationImporter importer;
    
    protected void setUp() throws Exception {
        GeoServer gs = new GeoServerImpl();
        gs.setFactory( new GeoServerFactoryImpl(gs) );
        
        importer = new LegacyConfigurationImporter( gs );
        importer.imprt(DataUtilities.urlToFile(getClass().getResource("services.xml")).getParentFile());
    }
    
    public void testGlobal() throws Exception {
        GeoServerInfo info = importer.getConfiguration().getGlobal();
        assertNotNull( info );
        
        LoggingInfo logging = importer.getConfiguration().getLogging();
        assertNotNull( logging );
        
        assertEquals( "DEFAULT_LOGGING.properties", logging.getLevel() );
        assertTrue( logging.isStdOutLogging() );
        assertEquals( "logs/geoserver.log", logging.getLocation() );
        assertFalse( info.isVerbose() );
        assertFalse( info.isVerboseExceptions() );  
        assertEquals( 8, info.getNumDecimals() );
        assertEquals( "UTF-8", info.getCharset() );
        assertEquals( 3, info.getUpdateSequence() );
    }
}
