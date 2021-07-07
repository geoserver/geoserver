/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.FeatureSource;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;

public class TemplatePropertyMapperTest extends STACTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        copyTemplate("/mandatoryLinks.json");
        copyTemplate("/items-LANDSAT8.json");
    }

    public TemplatePropertyMapper getPropertyMapper() throws Exception {
        STACTemplates templates = GeoServerExtensions.bean(STACTemplates.class);
        FeatureSource<FeatureType, Feature> source = getOpenSearchAccess().getProductSource();
        return new TemplatePropertyMapper(source, templates);
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
