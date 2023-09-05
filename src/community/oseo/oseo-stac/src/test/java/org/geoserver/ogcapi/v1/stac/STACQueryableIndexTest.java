/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.geoserver.featurestemplating.configuration.Template;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OSEOInfoImpl;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.OseoEvent;
import org.geoserver.opensearch.eo.OseoEventType;
import org.geoserver.opensearch.eo.store.JDBCOpenSearchAccessTest;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class STACQueryableIndexTest extends STACTestSupport {
    private static final String FAKE_ID = "foobar";
    private static final String TYPE_NUMBER = "number";
    private static final String TYPE_STRING = "string";
    private static final String TYPE_INTEGER = "integer";
    static OpenSearchAccess data;

    @BeforeClass
    public static void setupClass() throws IOException, SQLException {
        data = JDBCOpenSearchAccessTest.setupAndReturnStore();
    }

    @AfterClass
    public static void clearExtensions() {
        GeoServerExtensionsHelper.clear();
    }

    @Test
    public void testCreateAndDeleteIndices() throws Exception {
        STACOseoListener stacOseoListener = getStacOseoListener();
        List<String> indices = stacOseoListener.getIndexListByTable("product");
        assertEquals(49, indices.size());
        OseoEvent oseoEvent = new OseoEvent();
        oseoEvent.setType(OseoEventType.POST_INSERT);
        oseoEvent.setCollectionName("SAS1");
        stacOseoListener.dataStoreChange(oseoEvent);
        indices = stacOseoListener.getIndexListByTable("product");
        assertEquals(66, indices.size());
        // check a few of them
        assertThat(indices, CoreMatchers.hasItem("sas1_geometry_idx")); // geometry
        assertThat(indices, CoreMatchers.hasItem("sas1_view_sun_azimuth_idx")); // plain field
        assertThat(indices, CoreMatchers.hasItem("sas1_jsontest1_idx")); // JSON index
        assertThat(indices, CoreMatchers.hasItem("sas1_s1_ipf_version_idx")); // JSON index
        assertThat(indices, CoreMatchers.hasItem("sas1_keywords_idx")); // JSON index
        // this one cannot be created, strToLowercase is used and no machinery to recognize it
        assertThat(indices, CoreMatchers.not(CoreMatchers.hasItem("sas1_constellation_idx")));

        OseoEvent oseoEvent2 = new OseoEvent();
        oseoEvent2.setType(OseoEventType.POST_INSERT);
        oseoEvent2.setCollectionName("SAS9");
        stacOseoListener.dataStoreChange(oseoEvent2);
        indices = stacOseoListener.getIndexListByTable("product");
        // there should not be any new indices because SAS9 fields are the same as SAS1
        assertEquals(66, indices.size());
        oseoEvent.setType(OseoEventType.PRE_DELETE);
        oseoEvent.setCollectionName("SAS1");
        stacOseoListener.dataStoreChange(oseoEvent);
        indices = stacOseoListener.getIndexListByTable("product");
        // None of the indices were deleted because they are still needed for SAS9
        assertEquals(66, indices.size());
        oseoEvent2.setType(OseoEventType.PRE_DELETE);
        oseoEvent2.setCollectionName("SAS9");
        stacOseoListener.dataStoreChange(oseoEvent2);
        indices = stacOseoListener.getIndexListByTable("product");
        assertEquals(49, indices.size());
    }

    private STACOseoListener getStacOseoListener() throws IOException {
        FileSystemResourceStore resourceStore =
                new FileSystemResourceStore(new File("./src/test/resources"));
        Resource templateDefinition = resourceStore.get("items-SAS1.json");
        FeatureSource<FeatureType, Feature> products = data.getProductSource();
        FeatureSource<FeatureType, Feature> collections = data.getCollectionSource();
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Filter sampleFilter =
                ff.equals(ff.property("identifier"), ff.literal("SAS1_20180226102021.01"));
        Feature sampleFeature = DataUtilities.first(products.getFeatures(sampleFilter));
        Filter collectionSampleFilter = ff.equals(ff.property("identifier"), ff.literal("SAS1"));
        Feature sampleCollectionFeature =
                DataUtilities.first(collections.getFeatures(collectionSampleFilter));
        TemplateReaderConfiguration config =
                new TemplateReaderConfiguration(STACTemplates.getNamespaces(products));
        Template template = new Template(templateDefinition, config);
        OSEOInfo service = new OSEOInfoImpl();
        service.getGlobalQueryables().addAll(Arrays.asList("id", "geometry", "collection"));
        STACTemplates templates = mock(STACTemplates.class);
        expect(templates.getItemTemplate("SAS1")).andReturn(template.getRootBuilder()).anyTimes();
        expect(templates.getItemTemplate("SAS9")).andReturn(template.getRootBuilder()).anyTimes();
        replay(templates);
        CollectionsCache collectionsCache = mock(CollectionsCache.class);
        expect(collectionsCache.getCollection("SAS1"))
                .andReturn(sampleCollectionFeature)
                .anyTimes();
        expect(collectionsCache.getCollection("SAS9"))
                .andReturn(sampleCollectionFeature)
                .anyTimes();
        SampleFeatures sampleFeatures = mock(SampleFeatures.class);
        expect(sampleFeatures.getSample("SAS1")).andReturn(sampleFeature).anyTimes();
        expect(sampleFeatures.getSample("SAS9")).andReturn(sampleFeature).anyTimes();
        expect(sampleFeatures.getSchema()).andReturn(sampleFeature.getType()).anyTimes();
        replay(collectionsCache);
        replay(sampleFeatures);
        OpenSearchAccessProvider accessProvider = mock(OpenSearchAccessProvider.class);
        expect(accessProvider.getOpenSearchAccess()).andReturn(data).anyTimes();
        replay(accessProvider);
        STACOseoListener stacOseoListener =
                new STACOseoListener(
                        getGeoServer(),
                        templates,
                        sampleFeatures,
                        collectionsCache,
                        accessProvider);
        return stacOseoListener;
    }
}
