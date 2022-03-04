/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.filter.Filter;

public class TemplatePropertyMapperTest extends STACTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        copyTemplate("/mandatoryLinks.json");
        copyTemplate("/items-LANDSAT8.json");
    }

    public TemplatePropertyMapper getPropertyMapper() {
        STACTemplates templates = GeoServerExtensions.bean(STACTemplates.class);
        SampleFeatures sampleFeatures = GeoServerExtensions.bean(SampleFeatures.class);
        CollectionsCache collectionsCache = GeoServerExtensions.bean(CollectionsCache.class);
        OSEOInfo oseoInfo = GeoServerExtensions.bean(OSEOInfo.class);
        return new TemplatePropertyMapper(templates, sampleFeatures, collectionsCache, oseoInfo);
    }

    @Test
    public void testCommonProperties() throws Exception {
        // shared property, should result in a single filter
        TemplatePropertyMapper mapper = getPropertyMapper();
        Filter mapped = mapper.mapProperties(null, ECQL.toFilter("datetime < 2020-01-01"));
        assertEquals(ECQL.toFilter("timeStart < 2020-01-01"), mapped);
    }

    @Test
    public void testLS8Specific() throws Exception {
        // property that exists only on LS8, should remove all other collections
        // even if they were not specified in the list
        TemplatePropertyMapper mapper = getPropertyMapper();
        Filter mapped = mapper.mapProperties(null, ECQL.toFilter("\"landsat:orbit\" < 50"));
        assertEquals(
                ECQL.toFilter("parentIdentifier = 'LANDSAT8' and \"eop:orbitNumber\" < 50"),
                mapped);
    }

    @Test
    public void testLS8SpecificSL() throws Exception {
        // property that exists only on LS8, we provide both SENTINEL and LS8, only the
        // second should come out
        TemplatePropertyMapper mapper = getPropertyMapper();
        Filter mapped =
                mapper.mapProperties(
                        Arrays.asList("SENTINEL2", "LANDSAT8"),
                        ECQL.toFilter("\"landsat:orbit\" < 50"));
        assertEquals(
                ECQL.toFilter("parentIdentifier = 'LANDSAT8' and \"eop:orbitNumber\" < 50"),
                mapped);
    }

    @Test
    public void testLS8SpecificWithSentinel() throws Exception {
        // property that exists only on LS8, provided against SENTINEL2... should exclude
        TemplatePropertyMapper mapper = getPropertyMapper();
        Filter mapped =
                mapper.mapProperties(
                        Arrays.asList("SENTINEL2"), ECQL.toFilter("\"landsat:orbit\" < 50"));
        assertEquals(Filter.EXCLUDE, mapped);
    }

    @Test
    @Ignore // we changed lookups so that queryables and filter back-mapping match 1-1
    // gsd was not considered a queryable, so it's not filterable upon... we might get this
    // back if gsd is considered a valid queryable too
    public void testStaticMatching() throws Exception {
        // property that exists only on LS8, and is static
        TemplatePropertyMapper mapper = getPropertyMapper();
        Filter mapped = mapper.mapProperties(null, ECQL.toFilter("gsd = 30"));
        assertEquals(ECQL.toFilter("parentIdentifier = 'LANDSAT8'"), mapped);
    }

    @Test
    public void testStaticNotMatching() throws Exception {
        // property that exists only on LS8, and is static
        TemplatePropertyMapper mapper = getPropertyMapper();
        Filter mapped = mapper.mapProperties(null, ECQL.toFilter("gsd = 40"));
        assertEquals(Filter.EXCLUDE, mapped);
    }

    @Test
    public void testDifferentExpressions() throws Exception {
        // property mapped in a different way in the two collections
        TemplatePropertyMapper mapper = getPropertyMapper();
        Filter mapped = mapper.mapProperties(null, ECQL.toFilter("eo:cloud_cover < 20"));
        Filter expected =
                ECQL.toFilter(
                        "(parentIdentifier = 'LANDSAT8' and (opt:cloudCover / 2) < 20) "
                                + "or (parentIdentifier <> 'LANDSAT8' and opt:cloudCover < 20)");
        assertEquals(expected, mapped);
    }
}
