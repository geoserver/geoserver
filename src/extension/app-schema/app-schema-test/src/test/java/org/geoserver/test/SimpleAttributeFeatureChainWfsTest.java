/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.appschema.filter.FilterFactoryImplNamespaceAware;
import org.geotools.appschema.jdbc.NestedFilterToSQL;
import org.geotools.data.FeatureSource;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.data.complex.AppSchemaDataAccessRegistry;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.filter.ComplexFilterSplitter;
import org.geotools.data.jdbc.FilterToSQLException;
import org.geotools.data.util.NullProgressListener;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.w3c.dom.Document;

/**
 * Test feature chaining with simple content type, e.g. for gml:name.
 *
 * @author Rini Angreani, CSIRO Earth Science and Resource Engineering
 */
public class SimpleAttributeFeatureChainWfsTest extends AbstractAppSchemaTestSupport {

    @Override
    protected SimpleAttributeFeatureChainMockData createTestData() {
        return new SimpleAttributeFeatureChainMockData();
    }

    /** Test that feature chaining for gml:name works. */
    @Test
    public void testGetFeature() {
        String path = "wfs?request=GetFeature&version=1.1.0&typeName=gsml:MappedFeature";
        Document doc = getAsDOM(path);
        LOGGER.info("MappedFeature with name feature chained Response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("4", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(4, "//gsml:MappedFeature", doc);

        // mf1
        checkMf1(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "133.8855 -23.6701",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4326",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/@srsName",
                doc);

        // mf2: extra values from denormalised tables
        checkMf2(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "167.9388 -29.0434",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4326",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsName",
                doc);

        // mf3
        checkMf3(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "-1.2 53.5 -1.2 53.6 -1.1 53.6 -1.1 53.5 -1.2 53.5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4283",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsName",
                doc);

        // mf4
        checkMf4(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "-1.3 53.5 -1.3 53.6 -1.2 53.6 -1.2 52.5 -1.3 53.5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gsml:shape/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gsml:shape/gml:Polygon/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4283",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gsml:shape/gml:Polygon/@srsName",
                doc);
    }

    /** Test reprojecting with feature chained geometry. */
    @Test
    public void testReprojecting() {
        String path =
                "wfs?request=GetFeature&version=1.1.0&typeName=gsml:MappedFeature&srsName=EPSG:4326";
        Document doc = getAsDOM(path);
        LOGGER.info(
                "Reprojected MappedFeature with name feature chained Response:\n"
                        + prettyString(doc));
        assertXpathEvaluatesTo("4", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(4, "//gsml:MappedFeature", doc);

        // mf1
        checkMf1(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "133.8855 -23.6701",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4326",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/@srsName",
                doc);

        // mf2: extra values from denormalised tables
        checkMf2(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "167.9388 -29.0434",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4326",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsName",
                doc);

        // mf3
        checkMf3(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "-1.2 53.50000004 -1.2 53.60000004 -1.1 53.60000004 -1.1 53.50000004 -1.2 53.50000004",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4326",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsName",
                doc);

        // mf4
        checkMf4(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "-1.3 53.50000004 -1.3 53.60000004 -1.2 53.60000004 -1.2 52.50000004 -1.3 53.50000004",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gsml:shape/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gsml:shape/gml:Polygon/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4326",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gsml:shape/gml:Polygon/@srsName",
                doc);
    }

    /** Test that filtering feature chained values works. */
    @Test
    public void testAttributeFilter() {
        // filter by name
        String xml = //
                "<wfs:GetFeature " //
                        + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES //
                        + ">" //
                        + "    <wfs:Query typeName=\"gsml:MappedFeature\">" //
                        + "        <ogc:Filter>" //
                        + "            <ogc:PropertyIsEqualTo>" //
                        + "                <ogc:PropertyName>gml:name</ogc:PropertyName>" //
                        + "                <ogc:Literal>nametwo 4</ogc:Literal>" //
                        + "            </ogc:PropertyIsEqualTo>" //
                        + "        </ogc:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf3(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "-1.2 53.5 -1.2 53.6 -1.1 53.6 -1.1 53.5 -1.2 53.5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4283",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsName",
                doc);

        xml = //
                "<wfs:GetFeature " //
                        + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES //
                        + ">" //
                        + "    <wfs:Query typeName=\"gsml:MappedFeature\">" //
                        + "        <ogc:Filter>" //
                        + "            <ogc:PropertyIsEqualTo>" //
                        + "                <ogc:PropertyName>gml:name</ogc:PropertyName>" //
                        + "                <ogc:Literal>nametwo 3</ogc:Literal>" //
                        + "            </ogc:PropertyIsEqualTo>" //
                        + "        </ogc:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));

        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf2(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "167.9388 -29.0434",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4326",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsName",
                doc);

        xml = //
                "<wfs:GetFeature " //
                        + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES //
                        + ">" //
                        + "    <wfs:Query typeName=\"gsml:MappedFeature\">" //
                        + "        <ogc:Filter>" //
                        + "            <ogc:PropertyIsEqualTo>" //
                        + "                <ogc:PropertyName>gml:name</ogc:PropertyName>" //
                        + "                <ogc:Literal>nametwo 2</ogc:Literal>" //
                        + "            </ogc:PropertyIsEqualTo>" //
                        + "        </ogc:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));

        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf2(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "167.9388 -29.0434",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4326",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsName",
                doc);
    }

    /** Test filtering client properties. */
    @Test
    public void testClientPropertiesFilter() {
        // filter by codespace coming from parent table
        String xml = //
                "<wfs:GetFeature " //
                        + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES
                        + ">" //
                        + "    <wfs:Query typeName=\"gsml:MappedFeature\">"
                        + "        <ogc:Filter>"
                        + "            <ogc:PropertyIsEqualTo>"
                        + "                <ogc:PropertyName>gml:name/@codeSpace</ogc:PropertyName>"
                        + "                <ogc:Literal>some:uri:mf3</ogc:Literal>"
                        + "            </ogc:PropertyIsEqualTo>"
                        + "        </ogc:Filter>"
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf3(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "-1.2 53.5 -1.2 53.6 -1.1 53.6 -1.1 53.5 -1.2 53.5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4283",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsName",
                doc);

        // filter by codespace coming from chained feature
        xml = //
                "<wfs:GetFeature " //
                        + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES //
                        + ">"
                        + "    <wfs:Query typeName=\"gsml:MappedFeature\">"
                        + "        <ogc:Filter>"
                        + "            <ogc:PropertyIsEqualTo>"
                        + "                <ogc:PropertyName>gml:name/@codeSpace</ogc:PropertyName>"
                        + "                <ogc:Literal>some uri 4</ogc:Literal>"
                        + "            </ogc:PropertyIsEqualTo>"
                        + "        </ogc:Filter>"
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf3(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "-1.2 53.5 -1.2 53.6 -1.1 53.6 -1.1 53.5 -1.2 53.5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4283",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsName",
                doc);

        // filter by xlink:href coming from chained feature
        xml = //
                "<wfs:GetFeature " //
                        + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES //
                        + ">"
                        + "    <wfs:Query typeName=\"gsml:MappedFeature\">"
                        + "        <ogc:Filter>"
                        + "            <ogc:PropertyIsEqualTo>"
                        + "                <ogc:PropertyName>gml:name/@xlink:href</ogc:PropertyName>"
                        + "                <ogc:Literal>some:uri:4</ogc:Literal>"
                        + "            </ogc:PropertyIsEqualTo>"
                        + "        </ogc:Filter>"
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf3(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "-1.2 53.5 -1.2 53.6 -1.1 53.6 -1.1 53.5 -1.2 53.5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4283",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsName",
                doc);
    }

    @Test
    public void testNestedFilterEncoding() throws IOException, FilterToSQLException {
        FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName("gsml", "MappedFeature");
        FeatureSource fs = ftInfo.getFeatureSource(new NullProgressListener(), null);
        AppSchemaDataAccess da = (AppSchemaDataAccess) fs.getDataStore();
        FeatureTypeMapping rootMapping = da.getMappingByNameOrElement(ftInfo.getQualifiedName());

        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        JDBCDataStore store = (JDBCDataStore) rootMapping.getSource().getDataStore();
        NestedFilterToSQL nestedFilterToSQL = createNestedFilterEncoder(rootMapping);

        FilterFactoryImplNamespaceAware ff = new FilterFactoryImplNamespaceAware();
        ff.setNamepaceContext(rootMapping.getNamespaces());

        /*
         * test filter by name coming from child table
         */
        PropertyIsEqualTo propertyIsEqualTo =
                ff.equals(ff.property("gml:name[2]"), ff.literal("nameone 4"));

        // Filter involving single nested attribute --> can be encoded
        ComplexFilterSplitter splitter =
                new ComplexFilterSplitter(store.getFilterCapabilities(), rootMapping);
        splitter.visit(propertyIsEqualTo, null);
        Filter preFilter = splitter.getFilterPre();
        Filter postFilter = splitter.getFilterPost();

        assertEquals(propertyIsEqualTo, preFilter);
        assertEquals(Filter.INCLUDE, postFilter);

        // filter must be "unrolled" (i.e. reverse mapped) first
        Filter unrolled = AppSchemaDataAccess.unrollFilter(propertyIsEqualTo, rootMapping);

        // Filter is nested
        assertTrue(NestedFilterToSQL.isNestedFilter(unrolled));

        String encodedFilter = nestedFilterToSQL.encodeToString(unrolled);

        // this is the generated query in PostGIS, but the test limits to check the presence of the
        // EXISTS keyword, as the actual SQL is dependent on the underlying database
        // EXISTS (SELECT "chain_link_1"."PKEY"
        //      FROM "appschematest"."MAPPEDFEATURENAMEONE" "chain_link_1"
        //      WHERE "chain_link_1"."NAME" = 'nameone 4' AND
        //            "appschematest"."MAPPEDFEATUREWITHNESTEDNAME"."ID" = "chain_link_1"."MF_ID")
        assertTrue(encodedFilter.contains("EXISTS"));
        assertContainsFeatures(fs.getFeatures(propertyIsEqualTo), "mf3");

        /*
         * test filter on attribute of root feature type
         */
        PropertyIsEqualTo ordinaryFilter =
                ff.equals(ff.property("gml:name[1]"), ff.literal("GUNTHORPE FORMATION"));

        // Filter involving direct attribute of root feature type --> can be encoded
        ComplexFilterSplitter splitter2 =
                new ComplexFilterSplitter(store.getFilterCapabilities(), rootMapping);
        splitter2.visit(ordinaryFilter, null);
        preFilter = splitter2.getFilterPre();
        postFilter = splitter2.getFilterPost();

        assertEquals(ordinaryFilter, preFilter);
        assertEquals(Filter.INCLUDE, postFilter);

        // Filter must be "unrolled" (i.e. reverse mapped) prior to being encoded
        unrolled = AppSchemaDataAccess.unrollFilter(ordinaryFilter, rootMapping);

        // Filter is NOT nested
        assertFalse(NestedFilterToSQL.isNestedFilter(unrolled));
        // IS THIS A BUG?!?
        String ordinaryEncoded = nestedFilterToSQL.encodeToString(unrolled);

        assertFalse(ordinaryEncoded.contains("EXISTS"));
        assertContainsFeatures(fs.getFeatures(ordinaryFilter), "mf1");

        /*
         * test filter matching multiple mappings
         */
        PropertyIsEqualTo multipleFilter =
                ff.equals(ff.property("gml:name"), ff.literal("GUNTHORPE FORMATION"));

        // Filter involves multiple nested attributes --> CANNOT be encoded
        ComplexFilterSplitter splitter3 =
                new ComplexFilterSplitter(store.getFilterCapabilities(), rootMapping);
        splitter3.visit(multipleFilter, null);
        preFilter = splitter3.getFilterPre();
        postFilter = splitter3.getFilterPost();

        assertEquals(Filter.INCLUDE, preFilter);
        assertEquals(multipleFilter, postFilter);

        // Filter must be "unrolled" (i.e. reverse mapped) first
        unrolled = AppSchemaDataAccess.unrollFilter(multipleFilter, rootMapping);

        // Filter is nested
        assertTrue(NestedFilterToSQL.isNestedFilter(unrolled));

        assertContainsFeatures(fs.getFeatures(multipleFilter), "mf1");

        /*
         * test combined filter, i.e. a filter composed of a "regular" filter, and a nested filter
         */
        PropertyIsEqualTo regularFilter =
                ff.equals(ff.property("gml:name[1]"), ff.literal("GUNTHORPE FORMATION"));
        PropertyIsEqualTo nestedFilter =
                ff.equals(ff.property("gml:name[2]"), ff.literal("nameone 4"));
        Or combined = ff.or(regularFilter, nestedFilter);

        // Filter can be encoded!
        ComplexFilterSplitter splitterCombined =
                new ComplexFilterSplitter(store.getFilterCapabilities(), rootMapping);
        splitterCombined.visit(combined, null);
        preFilter = splitterCombined.getFilterPre();
        postFilter = splitterCombined.getFilterPost();

        assertEquals(combined, preFilter);
        assertEquals(Filter.INCLUDE, postFilter);

        // Filter must be "unrolled" (i.e. reverse mapped) first
        unrolled = AppSchemaDataAccess.unrollFilter(combined, rootMapping);

        // Filter is nested
        assertTrue(NestedFilterToSQL.isNestedFilter(unrolled));

        String encodedCombined = nestedFilterToSQL.encodeToString(unrolled);

        // this is the generated query in PostGIS, but the test limits to check the presence of the
        // EXISTS keyword, as the actual SQL is dependent on the underlying database
        // (LEX_D = 'GUNTHORPE FORMATION' OR EXISTS
        //      (SELECT "chain_link_1"."PKEY"
        //      FROM "appschematest"."MAPPEDFEATURENAMEONE" "chain_link_1"
        //      WHERE "chain_link_1"."NAME" = 'nameone 4' AND
        //            "appschematest"."MAPPEDFEATUREWITHNESTEDNAME"."ID" = "chain_link_1"."MF_ID"))
        assertTrue(encodedCombined.matches("^\\(.*GUNTHORPE FORMATION.*OR.*EXISTS.*\\)$"));
        assertContainsFeatures(fs.getFeatures(combined), "mf1", "mf3");
        // test UNION improvement off
        AppSchemaDataAccessRegistry.getAppSchemaProperties()
                .setProperty("app-schema.orUnionReplace", "false");
        try {
            assertContainsFeatures(fs.getFeatures(combined), "mf1", "mf3");
        } finally {
            AppSchemaDataAccessRegistry.getAppSchemaProperties()
                    .setProperty("app-schema.orUnionReplace", "true");
        }

        /*
         * test filter comparing multiple nested attributes
         */
        PropertyIsNotEqualTo notEquals =
                ff.notEqual(ff.property("gml:name[2]"), ff.property("gml:name[3]"));

        // Filter involves multiple nested attributes --> CANNOT be encoded
        ComplexFilterSplitter splitterNotEquals =
                new ComplexFilterSplitter(store.getFilterCapabilities(), rootMapping);
        splitterNotEquals.visit(notEquals, null);
        preFilter = splitterNotEquals.getFilterPre();
        postFilter = splitterNotEquals.getFilterPost();

        assertEquals(Filter.INCLUDE, preFilter);
        assertEquals(notEquals, postFilter);

        // Filter must be "unrolled" (i.e. reverse mapped) first
        unrolled = AppSchemaDataAccess.unrollFilter(notEquals, rootMapping);

        // Filter is nested
        assertTrue(NestedFilterToSQL.isNestedFilter(unrolled));

        assertContainsFeatures(fs.getFeatures(notEquals), "mf1", "mf2", "mf3", "mf4");
    }

    /** Checks a nested OR condition with UNION improvement on and off */
    @Test
    public void testUnionImprovement() throws IOException, FilterToSQLException {
        FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName("gsml", "MappedFeature");
        FeatureSource fs = ftInfo.getFeatureSource(new NullProgressListener(), null);
        AppSchemaDataAccess da = (AppSchemaDataAccess) fs.getDataStore();
        FeatureTypeMapping rootMapping = da.getMappingByNameOrElement(ftInfo.getQualifiedName());
        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        JDBCDataStore store = (JDBCDataStore) rootMapping.getSource().getDataStore();
        NestedFilterToSQL nestedFilterToSQL = createNestedFilterEncoder(rootMapping);

        FilterFactoryImplNamespaceAware ff = new FilterFactoryImplNamespaceAware();
        ff.setNamepaceContext(rootMapping.getNamespaces());

        PropertyIsEqualTo regularFilter =
                ff.equals(ff.property("gml:name[2]"), ff.literal("nameone 2"));
        PropertyIsEqualTo nestedFilter =
                ff.equals(ff.property("gml:name[2]"), ff.literal("nameone 4"));
        Or combined = ff.or(regularFilter, nestedFilter);

        assertContainsFeatures(fs.getFeatures(combined), "mf2", "mf3");
        // set improvement to off
        AppSchemaDataAccessRegistry.getAppSchemaProperties()
                .setProperty("app-schema.orUnionReplace", "false");
        try {
            FeatureTypeInfo ftInfo1 = getCatalog().getFeatureTypeByName("gsml", "MappedFeature");
            FeatureSource fs1 = ftInfo.getFeatureSource(new NullProgressListener(), null);
            assertContainsFeatures(fs1.getFeatures(combined), "mf2", "mf3");
        } finally {
            AppSchemaDataAccessRegistry.getAppSchemaProperties()
                    .setProperty("app-schema.orUnionReplace", "true");
        }
    }

    private void checkMf1(Document doc) {
        assertXpathCount(4, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name", doc);
        // gml:name with values coming from the main table
        assertXpathEvaluatesTo(
                "GUNTHORPE FORMATION",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[1]",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo(
                "nameone 1",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[2]",
                doc);
        // client property coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo(
                "some uri 1",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[2]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo(
                "nametwo 1",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[3]",
                doc);
        // client property coming from the parent table
        assertXpathEvaluatesTo(
                "some:uri:mf1",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[3]/@codeSpace",
                doc);
        // gml:name as xlink:href from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo(
                "some:uri:1",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[4]/@xlink:href",
                doc);
    }

    private void checkMf2(Document doc) {
        // mf2: extra values from denormalised tables
        assertXpathCount(7, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name", doc);
        // gml:name with values coming from the main table
        assertXpathEvaluatesTo(
                "MERCIA MUDSTONE GROUP",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[1]",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo(
                "nameone 2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[2]",
                doc);
        // client property coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo(
                "some uri 2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[2]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo(
                "nameone 3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[3]",
                doc);
        // client property coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo(
                "some uri 3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[3]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo(
                "nametwo 2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[4]",
                doc);
        // client property coming from the parent table
        assertXpathEvaluatesTo(
                "some:uri:mf2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[4]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo(
                "nametwo 3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[5]",
                doc);
        // client property coming from the parent table
        assertXpathEvaluatesTo(
                "some:uri:mf2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[5]/@codeSpace",
                doc);
        // gml:name as xlink:href from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo(
                "some:uri:2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[6]/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "some:uri:3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[7]/@xlink:href",
                doc);
    }

    private void checkMf3(Document doc) {
        assertXpathCount(4, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name", doc);
        // gml:name with values coming from the main table
        assertXpathEvaluatesTo(
                "CLIFTON FORMATION",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[1]",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo(
                "nameone 4",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[2]",
                doc);
        // client property coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo(
                "some uri 4",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[2]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo(
                "nametwo 4",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[3]",
                doc);
        // client property coming from the parent table
        assertXpathEvaluatesTo(
                "some:uri:mf3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[3]/@codeSpace",
                doc);
        // gml:name as xlink:href from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo(
                "some:uri:4",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[4]/@xlink:href",
                doc);
    }

    private void checkMf4(Document doc) {
        assertXpathCount(4, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name", doc);
        // gml:name with values coming from the main table
        assertXpathEvaluatesTo(
                "MURRADUC BASALT",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[1]",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo(
                "nameone 5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[2]",
                doc);
        // client property coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo(
                "some uri 5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[2]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo(
                "nametwo 5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[3]",
                doc);
        // client property coming from the parent table
        assertXpathEvaluatesTo(
                "some:uri:mf4",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[3]/@codeSpace",
                doc);
        // gml:name as xlink:href from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo(
                "some:uri:5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[4]/@xlink:href",
                doc);
    }
}
