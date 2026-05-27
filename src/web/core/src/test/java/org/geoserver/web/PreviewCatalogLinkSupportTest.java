/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.geoserver.data.test.CiteTestData.POLYGONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.junit.Test;

/** Verifies catalog metadata and data links are exposed as home page preview links. */
public class PreviewCatalogLinkSupportTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no default data
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addVectorLayer(POLYGONS, getCatalog());
        addCatalogLinks();
    }

    private LayerInfo layer() {
        LayerInfo layer = getCatalog().getLayerByName(getLayerId(POLYGONS));
        assertNotNull(layer);
        return layer;
    }

    @Test
    public void testMetadataLinksFromCatalog() {
        List<PreviewLink> links = PreviewCatalogLinkSupport.metadataLinks(layer());
        assertEquals(1, links.size());
        assertEquals("Dataset description (FGDC)", links.get(0).label());
        assertEquals("http://example.org/metadata", links.get(0).href());
        assertEquals("text/html", links.get(0).title());
        assertEquals(PreviewLink.METADATA, links.get(0).catalogLinkType());
    }

    @Test
    public void testDataLinksFromCatalog() {
        List<PreviewLink> links = PreviewCatalogLinkSupport.dataLinks(layer());
        assertEquals(1, links.size());
        assertEquals("http://example.org/data.zip", links.get(0).href());
        assertFalse(links.get(0).label().isEmpty());
        assertEquals(PreviewLink.DATA, links.get(0).catalogLinkType());
    }

    @Test
    public void testCatalogLinkProvidersArePublishedAsExtensions() {
        List<HomePagePreviewSectionProvider> providers =
                GeoServerExtensions.extensions(HomePagePreviewSectionProvider.class);

        HomePagePreviewSectionProvider metadataProvider = null;
        HomePagePreviewSectionProvider dataProvider = null;
        for (HomePagePreviewSectionProvider provider : providers) {
            if (provider instanceof PreviewCatalogLinkSupport.MetadataPreviewSectionProvider) {
                metadataProvider = provider;
            } else if (provider instanceof PreviewCatalogLinkSupport.DataPreviewSectionProvider) {
                dataProvider = provider;
            }
        }

        assertNotNull(metadataProvider);
        assertNotNull(dataProvider);
        assertEquals(PreviewCatalogLinkSupport.METADATA_LINKS, metadataProvider.getTitleKey());
        assertEquals(PreviewCatalogLinkSupport.DATA_LINKS, dataProvider.getTitleKey());
        assertEquals(1, metadataProvider.getLinks(layer()).size());
        assertEquals(1, dataProvider.getLinks(layer()).size());
    }

    @Test
    public void testCatalogLinkProviderPriorityOrdering() {
        List<HomePagePreviewSectionProvider> providers =
                GeoServerExtensions.extensions(HomePagePreviewSectionProvider.class);

        int metadataIndex = -1;
        int dataIndex = -1;
        for (int i = 0; i < providers.size(); i++) {
            HomePagePreviewSectionProvider provider = providers.get(i);
            if (provider instanceof PreviewCatalogLinkSupport.MetadataPreviewSectionProvider) {
                metadataIndex = i;
                assertEquals(PreviewCatalogLinkSupport.METADATA_LINKS_PRIORITY, provider.getPriority());
            } else if (provider instanceof PreviewCatalogLinkSupport.DataPreviewSectionProvider) {
                dataIndex = i;
                assertEquals(PreviewCatalogLinkSupport.DATA_LINKS_PRIORITY, provider.getPriority());
            }
        }

        assertTrue(metadataIndex >= 0);
        assertTrue(dataIndex >= 0);
        assertTrue(metadataIndex < dataIndex);
    }

    private void addCatalogLinks() {
        Catalog catalog = getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(POLYGONS));
        assertNotNull(layer);
        String prefixedName = layer.getResource().prefixedName();
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName(prefixedName);
        assertNotNull(featureType);

        MetadataLinkInfo metadata = catalog.getFactory().createMetadataLink();
        metadata.setAbout("Dataset description");
        metadata.setMetadataType("FGDC");
        metadata.setType("text/html");
        metadata.setContent("http://example.org/metadata");
        featureType.getMetadataLinks().add(metadata);

        DataLinkInfo data = catalog.getFactory().createDataLink();
        data.setType("application/zip");
        data.setContent("http://example.org/data.zip");
        featureType.getDataLinks().add(data);

        catalog.save(featureType);
        assertEquals(
                1, catalog.getFeatureTypeByName(prefixedName).getDataLinks().size());
    }
}
