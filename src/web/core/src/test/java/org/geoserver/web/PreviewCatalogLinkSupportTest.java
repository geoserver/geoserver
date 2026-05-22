/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.geoserver.data.test.CiteTestData.POLYGONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.impl.DataLinkInfoImpl;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.junit.Test;

/** Verifies catalog metadata and data links are exposed as home page preview links. */
public class PreviewCatalogLinkSupportTest extends GeoServerWicketTestSupport {

    private LayerInfo layer;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no default data
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addVectorLayer(POLYGONS, getCatalog());
        layer = getCatalog().getLayerByName(getLayerId(POLYGONS));
        addCatalogLinks(layer);
    }

    @Test
    public void testMetadataLinksFromCatalog() {
        List<PreviewLink> links = PreviewCatalogLinkSupport.metadataLinks(layer);
        assertEquals(1, links.size());
        assertEquals("Dataset description (FGDC)", links.get(0).label());
        assertEquals("http://example.org/metadata", links.get(0).href());
        assertEquals("text/html", links.get(0).title());
        assertEquals(PreviewLink.METADATA, links.get(0).catalogLinkType());
    }

    @Test
    public void testDataLinksFromCatalog() {
        List<PreviewLink> links = PreviewCatalogLinkSupport.dataLinks(layer);
        assertEquals(1, links.size());
        assertEquals("http://example.org/data.zip", links.get(0).href());
        assertFalse(links.get(0).label().isEmpty());
        assertEquals(PreviewLink.DATA, links.get(0).catalogLinkType());
    }

    private void addCatalogLinks(LayerInfo layer) {
        Catalog catalog = getCatalog();
        FeatureTypeInfo featureType =
                catalog.getFeatureTypeByName(((FeatureTypeInfo) layer.getResource()).prefixedName());

        MetadataLinkInfo metadata = new MetadataLinkInfoImpl();
        metadata.setAbout("Dataset description");
        metadata.setMetadataType("FGDC");
        metadata.setType("text/html");
        metadata.setContent("http://example.org/metadata");
        featureType.getMetadataLinks().add(metadata);

        DataLinkInfo data = new DataLinkInfoImpl();
        data.setType("application/zip");
        data.setContent("http://example.org/data.zip");
        featureType.getDataLinks().add(data);

        catalog.save(featureType);
        catalog.save(layer);
    }
}
