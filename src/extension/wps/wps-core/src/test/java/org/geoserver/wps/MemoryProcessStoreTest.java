/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

/**
 * Tests the memory based implementation of {@link ProcessStatusStore}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MemoryProcessStoreTest extends AbstractProcessStoreTest {

    @Override
    protected ProcessStatusStore buildStore() {
        return new MemoryProcessStatusStore();
    }
}
