/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl.util;

import java.io.File;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.util.LegacyCatalogImporter;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.TestData;
import org.geoserver.test.GeoServerAbstractTestSupport;

public class LegacyCatalogImporterTest extends GeoServerAbstractTestSupport {

    private static final QName typeName = MockData.BASIC_POLYGONS;

    @Override
    protected void tearDownInternal() throws Exception {
        super.tearDownInternal();
        // reset the legacy flag so that other tests are not getting affected by it
        GeoServerLoader.setLegacy(false);
    }

    @Override
    protected TestData buildTestData() throws Exception {
        // create the data directory
        MockData dataDirectory = new MockData();
        dataDirectory.addWellKnownTypes(new QName[] {typeName});
        return dataDirectory;
    }

    public void testMissingFeatureTypes() throws Exception {
        MockData mockData = (MockData) getTestData();

        mockData.getFeatureTypesDirectory().delete();
        LegacyCatalogImporter importer = new LegacyCatalogImporter(new CatalogImpl());
        importer.imprt(mockData.getDataDirectoryRoot());
    }

    public void testMissingCoverages() throws Exception {
        MockData mockData = (MockData) getTestData();

        mockData.getCoveragesDirectory().delete();
        LegacyCatalogImporter importer = new LegacyCatalogImporter(new CatalogImpl());
        importer.imprt(mockData.getDataDirectoryRoot());
    }

    /**
     * As per GEOS-3513, make sure the old SRS codes are imported by adding the EPSG: prefix where
     * needed
     */
    public void testCRSPrefix() throws Exception {
        MockData mockData = (MockData) getTestData();

        mockData.getCoveragesDirectory().delete();
        Catalog catalog = new CatalogImpl();
        LegacyCatalogImporter importer = new LegacyCatalogImporter(catalog);

        File dataDirectoryRoot = mockData.getDataDirectoryRoot();
        importer.imprt(dataDirectoryRoot);

        FeatureTypeInfo typeInfo =
                catalog.getFeatureTypeByName(typeName.getNamespaceURI(), typeName.getLocalPart());
        assertEquals("EPSG:4326", typeInfo.getSRS());
    }
}
