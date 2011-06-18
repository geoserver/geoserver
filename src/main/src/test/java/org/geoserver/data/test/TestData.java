package org.geoserver.data.test;

import java.io.File;

/**
 * A test data directory creator and accessor, used for functional testing
 * purposes in GeoServer
 * 
 * @author Andrea Aime - TOPP
 */
public interface TestData {
    /**
     * Creates the temporary GeoServer data directory
     */
    public void setUp() throws Exception;

    /**
     * Wipes out the contents of the temporary data directory
     */
    public void tearDown() throws Exception;
    
    /**
     * @return The root of the data directory.
     */
    public File getDataDirectoryRoot();
    
    /**
     * Returns wheter the test data is available. If not the test should be skipped
     */
    public boolean isTestDataAvailable();
}
