/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.appschema.filter.FilterFactoryImplNamespaceAware;
import org.geotools.appschema.jdbc.NestedFilterToSQL;
import org.geotools.data.FeatureSource;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.filter.ComplexFilterSplitter;
import org.geotools.data.jdbc.FilterToSQLException;
import org.geotools.data.util.NullProgressListener;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsEqualTo;
import org.w3c.dom.Document;

/**
 * Test whether nested Id's can be used in a filter.
 *
 * @author Niels Charlier, Curtin University Of Technology *
 */
public class NestedIdSupportTest extends AbstractAppSchemaTestSupport {

    @Override
    protected NestedIdSupportTestData createTestData() {
        return new NestedIdSupportTestData();
    }

    /** Test Nested Id with Feature Chaining */
    @Test
    public void testNestedIdFeatureChaining() {
        String xml =
                "<wfs:GetFeature " //
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                        + "xmlns:gsml=\""
                        + AbstractAppSchemaMockData.GSML_URI
                        + "\" " //
                        + ">" //
                        + "<wfs:Query typeName=\"gsml:MappedFeature\">"
                        + "<ogc:Filter>"
                        + "     <ogc:PropertyIsEqualTo>"
                        + "        <ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/gsml:ControlledConcept/@gml:id</ogc:PropertyName>"
                        + "        <ogc:Literal>cc.1</ogc:Literal>"
                        + "     </ogc:PropertyIsEqualTo>"
                        + " </ogc:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);

        LOGGER.info("MappedFeature: WFS GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo(
                "mf4", "wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature/@gml:id", doc);
    }

    /** Test Nested Id with InlineMapping */
    @Test
    public void testNestedIdInlineMapping() {
        String xml =
                "<wfs:GetFeature " //
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                        + "xmlns:gsml=\""
                        + AbstractAppSchemaMockData.GSML_URI
                        + "\" " //
                        + ">" //
                        + "<wfs:Query typeName=\"gsml:Borehole\">"
                        + "<ogc:Filter>"
                        + "     <ogc:PropertyIsEqualTo>"
                        + "        <ogc:PropertyName>gsml:indexData/gsml:BoreholeDetails/@gml:id</ogc:PropertyName>"
                        + "        <ogc:Literal>bh.details.11.sp</ogc:Literal>"
                        + "     </ogc:PropertyIsEqualTo>"
                        + " </ogc:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);

        LOGGER.info("Borehole: WFS GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:Borehole", doc);
        assertXpathEvaluatesTo(
                "11", "wfs:FeatureCollection/gml:featureMember/gsml:Borehole/@gml:id", doc);
    }

    @Test
    public void testNestedFiltersEncoding() throws IOException, FilterToSQLException {
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
         * test filter on nested ID
         */
        PropertyIsEqualTo nestedIdFilter =
                ff.equals(
                        ff.property(
                                "gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/gsml:ControlledConcept/@gml:id"),
                        ff.literal("cc.1"));

        // Filter involves a single nested attribute --> can be encoded
        ComplexFilterSplitter splitter =
                new ComplexFilterSplitter(store.getFilterCapabilities(), rootMapping);
        splitter.visit(nestedIdFilter, null);
        Filter preFilter = splitter.getFilterPre();
        Filter postFilter = splitter.getFilterPost();

        assertEquals(nestedIdFilter, preFilter);
        assertEquals(Filter.INCLUDE, postFilter);

        // filter must be "unrolled" (i.e. reverse mapped) first
        Filter unrolled = AppSchemaDataAccess.unrollFilter(nestedIdFilter, rootMapping);

        // Filter is nested
        assertTrue(NestedFilterToSQL.isNestedFilter(unrolled));

        String encodedFilter = nestedFilterToSQL.encodeToString(unrolled);

        // this is the generated query in PostGIS, but the test limits to check the presence of the
        // a few keywords, as the actual SQL is dependent on the underlying database
        // EXISTS (SELECT "chain_link_3"."PKEY"
        //      FROM "appschematest"."CONTROLLEDCONCEPT" "chain_link_3"
        //           INNER JOIN "appschematest"."COMPOSITIONPART" "chain_link_2" ON
        // "chain_link_2"."ROW_ID" = "chain_link_3"."COMPOSITION_ID"
        //           INNER JOIN "appschematest"."GEOLOGICUNIT" "chain_link_1" ON
        // "chain_link_1"."COMPONENTPART_ID" = "chain_link_2"."ROW_ID"
        //      WHERE "chain_link_3"."GML_ID" = 'cc.1' AND
        // "appschematest"."MAPPEDFEATUREPROPERTYFILE"."GEOLOGIC_UNIT_ID" = "chain_link_1"."GML_ID")
        assertTrue(
                encodedFilter.matches("^EXISTS.*SELECT.*FROM.*INNER JOIN.*INNER JOIN.*WHERE.*$"));
        assertContainsFeatures(fs.getFeatures(nestedIdFilter), "mf4");
    }
}
