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
 * WFS GetFeature to test polymorphism in Geoserver app-schema.
 *
 * @author Rini Angreani, CSIRO Earth Science and Resource Engineering
 */
public class PolymorphismWfsTest extends AbstractAppSchemaTestSupport {

    @Override
    protected PolymorphismMockData createTestData() {
        return new PolymorphismMockData();
    }

    @Test
    public void testPolymorphism() {
        Document doc =
                getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=ex:PolymorphicFeature");
        LOGGER.info(
                "WFS GetFeature&typename=ex:PolymorphicFeature response:\n" + prettyString(doc));
        assertXpathCount(6, "//ex:PolymorphicFeature", doc);
        // check contents per attribute (each attribute has a different use case)
        checkPolymorphicFeatureChaining(doc);
        checkPolymorphismOnly(doc);
        checkFeatureChainingOnly(doc);
        checkXlinkHrefValues(doc);
        checkAnyType(doc);
    }

    /**
     * Test filtering polymorphism with feature chaining set up works. Also tests filtering when
     * mappingName is used as linkElement.
     */
    @Test
    public void testFirstValueFilters() {
        //        <AttributeMapping>
        //        <!-- Test feature chaining and polymorphism -->
        //                <targetAttribute>ex:firstValue</targetAttribute>
        //                <sourceExpression>
        //                        <OCQL>VALUE_ID</OCQL>
        //                        <linkElement>
        //                            Recode(CLASS_TEXT, 'numeric', 'NumericType', 'literal',
        // toXlinkHref(strConcat('urn:value::', VALUE_ID)))
        //                        </linkElement>
        //                        <linkField>FEATURE_LINK</linkField>
        //                </sourceExpression>
        //                <isMultiple>true</isMultiple>
        //        </AttributeMapping>
        String xml = //
                "<wfs:GetFeature " //
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                        + "xmlns:ex=\"http://example.com\" " //
                        + "xmlns:gsml=\""
                        + AbstractAppSchemaMockData.GSML_URI
                        + "\" " //
                        + ">" //
                        + "    <wfs:Query typeName=\"ex:PolymorphicFeature\">" //
                        + "        <ogc:Filter>" //
                        + "            <ogc:PropertyIsEqualTo>" //
                        + "                <ogc:PropertyName>ex:firstValue/gsml:CGI_NumericValue/gsml:principalValue</ogc:PropertyName>" //
                        + "                <ogc:Literal>1.0</ogc:Literal>" //
                        + "            </ogc:PropertyIsEqualTo>" //
                        + "        </ogc:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//ex:PolymorphicFeature", doc);
        // f1
        assertXpathEvaluatesTo("f1", "//ex:PolymorphicFeature/@gml:id", doc);
        assertXpathEvaluatesTo(
                "1.0",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:firstValue/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:firstValue/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);
    }

    /**
     * Test filtering polymorphism with no feature chaining works. Also tests filtering when
     * mappingName is used as linkElement.
     */
    @Test
    public void testSecondValueFilters() {
        //        <AttributeMapping>
        //        <!-- Test polymorphism with no feature chaining i.e. no linkField -->
        //            <targetAttribute>ex:secondValue</targetAttribute>
        //            <sourceExpression>
        //                    <linkElement>
        //                        if_then_else(isNull(CLASS_TEXT), Expression.Nil,
        //                            if_then_else(equalTo(CLASS_TEXT, 'numeric'), 'NumericType',
        // 'TermValue2'))
        //                    </linkElement>
        //            </sourceExpression>
        //        </AttributeMapping>
        String xml = //
                "<wfs:GetFeature " //
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                        + "xmlns:ex=\"http://example.com\" " //
                        + "xmlns:gsml=\""
                        + AbstractAppSchemaMockData.GSML_URI
                        + "\" " //
                        + ">" //
                        + "    <wfs:Query typeName=\"ex:PolymorphicFeature\">" //
                        + "        <ogc:Filter>" //
                        + "            <ogc:PropertyIsEqualTo>" //
                        + "                <ogc:PropertyName>ex:secondValue/gsml:CGI_NumericValue/gsml:principalValue/@uom</ogc:PropertyName>" //
                        + "                <ogc:Literal>m</ogc:Literal>" //
                        + "            </ogc:PropertyIsEqualTo>" //
                        + "        </ogc:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(2, "//ex:PolymorphicFeature", doc);
        // f1
        assertXpathEvaluatesTo("f1", "(//ex:PolymorphicFeature)[1]/@gml:id", doc);
        assertXpathEvaluatesTo(
                "1.0",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:secondValue/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:secondValue/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);
        // f3
        assertXpathEvaluatesTo("f3", "(//ex:PolymorphicFeature)[2]/@gml:id", doc);
        assertXpathEvaluatesTo(
                "0.0",
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:secondValue/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:secondValue/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);
    }

    /** Tests filtering mapping of any type works. */
    @Test
    public void testAnyTypeFilters() {
        //        <AttributeMapping>
        //        <!-- Test polymorphism with anyType  -->
        //            <targetAttribute>ex:anyValue</targetAttribute>
        //            <sourceExpression>
        //                    <linkElement>
        //                       Recode(CLASS_TEXT, Expression.Nil,
        // toXlinkHref('urn:ogc:def:nil:OGC::missing'),
        //                           'numeric', toXlinkHref(strConcat('urn:numeric-value::',
        // NUMERIC_VALUE)),
        //                           'literal', 'TermValue2')
        //                    </linkElement>
        //            </sourceExpression>
        //         </AttributeMapping>
        String xml = //
                "<wfs:GetFeature " //
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                        + "xmlns:ex=\"http://example.com\" " //
                        + "xmlns:gsml=\""
                        + AbstractAppSchemaMockData.GSML_URI
                        + "\" " //
                        + ">" //
                        + "    <wfs:Query typeName=\"ex:PolymorphicFeature\">" //
                        + "        <ogc:Filter>" //
                        + "            <ogc:PropertyIsEqualTo>" //
                        + "                <ogc:PropertyName>ex:anyValue/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>" //
                        + "                <ogc:Literal>0</ogc:Literal>" //
                        + "            </ogc:PropertyIsEqualTo>" //
                        + "        </ogc:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(2, "//ex:PolymorphicFeature", doc);
        // f2
        assertXpathEvaluatesTo("f2", "(//ex:PolymorphicFeature)[1]/@gml:id", doc);
        assertXpathEvaluatesTo(
                "0",
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:anyValue/gsml:CGI_TermValue/gsml:value",
                doc);
        // f5
        assertXpathEvaluatesTo("f5", "(//ex:PolymorphicFeature)[2]/@gml:id", doc);
        assertXpathEvaluatesTo(
                "0",
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:anyValue/gsml:CGI_TermValue/gsml:value",
                doc);
    }

    /** Tests filtering feature chaining where it's linked by mappingName. */
    @Test
    public void testMappingNameFilters() {
        //        <AttributeMapping>
        //        <!-- Test polymorphism using normal feature chaining with no conditions -->
        //            <targetAttribute>ex:thirdValue</targetAttribute>
        //            <sourceExpression>
        //                    <OCQL>VALUE_ID</OCQL>
        //                    <linkElement>gsml:CGI_NumericValue</linkElement>
        //                    <linkField>FEATURE_LINK</linkField>
        //            </sourceExpression>
        //    </AttributeMapping>
        //    <AttributeMapping>
        //        <!-- See above -->
        //            <targetAttribute>ex:thirdValue</targetAttribute>
        //            <sourceExpression>
        //                    <OCQL>VALUE_ID</OCQL>
        //                    <linkElement>gsml:CGI_TermValue</linkElement>
        //                    <linkField>FEATURE_LINK</linkField>
        //            </sourceExpression>
        //    </AttributeMapping>
        String xml = //
                "<wfs:GetFeature " //
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                        + "xmlns:ex=\"http://example.com\" " //
                        + "xmlns:gsml=\""
                        + AbstractAppSchemaMockData.GSML_URI
                        + "\" " //
                        + ">" //
                        + "    <wfs:Query typeName=\"ex:PolymorphicFeature\">" //
                        + "        <ogc:Filter>" //
                        + "            <ogc:PropertyIsEqualTo>" //
                        + "                <ogc:PropertyName>ex:thirdValue/gsml:CGI_NumericValue/gsml:principalValue</ogc:PropertyName>" //
                        + "                <ogc:Literal>1.0</ogc:Literal>" //
                        + "            </ogc:PropertyIsEqualTo>" //
                        + "        </ogc:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(2, "//ex:PolymorphicFeature", doc);
        // f1
        assertXpathEvaluatesTo("f1", "(//ex:PolymorphicFeature)[1]/@gml:id", doc);
        assertXpathEvaluatesTo(
                "1.0",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:thirdValue/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:thirdValue/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);

        // f4
        assertXpathEvaluatesTo("f4", "(//ex:PolymorphicFeature)[2]/@gml:id", doc);
        assertXpathEvaluatesTo(
                "1.0",
                "//ex:PolymorphicFeature[@gml:id='f4']/ex:thirdValue/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//ex:PolymorphicFeature[@gml:id='f4']/ex:thirdValue/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);
    }

    /** This is to test that polymorphism with feature chaining works. */
    private void checkPolymorphicFeatureChaining(Document doc) {
        // f1: make sure only 1 gsml:CGI_NumericValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f1']/ex:firstValue", doc);
        assertXpathCount(
                1,
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:firstValue/gsml:CGI_NumericValue",
                doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f1']/ex:firstValue/gsml:CGI_TermValue", doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f1']/ex:firstValue/gsml:CGI_TermValue", doc);
        assertXpathEvaluatesTo(
                "1.0",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:firstValue/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:firstValue/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);

        // f2: make sure only 1 xlink:href to gsml:CGI_TermValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f2']/ex:firstValue", doc);
        assertXpathEvaluatesTo(
                "urn:value::x",
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:firstValue/@xlink:href",
                doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:firstValue/gsml:CGI_NumericValue",
                doc);

        // f3: make sure firstValue is null
        assertXpathCount(0, "//ex:PolymorphicFeature[@gml:id='f3']/ex:firstValue", doc);

        // f4: make sure firstValue is null
        assertXpathCount(0, "//ex:PolymorphicFeature[@gml:id='f4']/ex:firstValue", doc);

        // f5: make sure only 1 xlink:href to gsml:CGI_TermValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f5']/ex:firstValue", doc);
        assertXpathEvaluatesTo(
                "urn:value::y",
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:firstValue/@xlink:href",
                doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:firstValue/gsml:CGI_NumericValue",
                doc);

        // f6: make sure firstValue is null
        assertXpathCount(0, "//ex:PolymorphicFeature[@gml:id='f6']/ex:firstValue", doc);
    }

    /** This is to test that polymorphism with no feature chaining works. */
    private void checkPolymorphismOnly(Document doc) {

        // f1: make sure only 1 gsml:CGI_NumericValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f1']/ex:secondValue", doc);
        assertXpathCount(
                1,
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:secondValue/gsml:CGI_NumericValue",
                doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f1']/ex:secondValue/gsml:CGI_TermValue", doc);
        assertXpathEvaluatesTo(
                "1.0",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:secondValue/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:secondValue/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);

        // f2: make sure only 1 gsml:CGI_TermValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f2']/ex:secondValue", doc);
        assertXpathCount(
                1, "//ex:PolymorphicFeature[@gml:id='f2']/ex:secondValue/gsml:CGI_TermValue", doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:secondValue/gsml:CGI_NumericValue",
                doc);
        assertXpathEvaluatesTo(
                "0",
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:secondValue/gsml:CGI_TermValue/gsml:value",
                doc);
        // test GEOT-3304
        assertXpathEvaluatesTo(
                "approximate",
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:secondValue/gsml:CGI_TermValue/@gsml:qualifier",
                doc);

        // f3: make sure only 1 gsml:CGI_NumericValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f3']/ex:secondValue", doc);
        assertXpathCount(
                1,
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:secondValue/gsml:CGI_NumericValue",
                doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f3']/ex:secondValue/gsml:CGI_TermValue", doc);
        assertXpathEvaluatesTo(
                "0.0",
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:secondValue/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:secondValue/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);

        // f4: make sure nothing is encoded
        assertXpathCount(0, "//ex:PolymorphicFeature[@gml:id='f4']/ex:secondValue", doc);

        // f5: make sure only 1 gsml:CGI_TermValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f5']/ex:secondValue", doc);
        assertXpathCount(
                1, "//ex:PolymorphicFeature[@gml:id='f5']/ex:secondValue/gsml:CGI_TermValue", doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:secondValue/gsml:CGI_NumericValue",
                doc);
        assertXpathEvaluatesTo(
                "0",
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:secondValue/gsml:CGI_TermValue/gsml:value",
                doc);
        // test GEOT-3304
        assertXpathEvaluatesTo(
                "approximate",
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:secondValue/gsml:CGI_TermValue/@gsml:qualifier",
                doc);

        // f6: make sure only 1 gsml:CGI_TermValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f6']/ex:secondValue", doc);
        assertXpathCount(
                1, "//ex:PolymorphicFeature[@gml:id='f6']/ex:secondValue/gsml:CGI_TermValue", doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f6']/ex:secondValue/gsml:CGI_NumericValue",
                doc);
        assertXpathEvaluatesTo(
                "1000",
                "//ex:PolymorphicFeature[@gml:id='f6']/ex:secondValue/gsml:CGI_TermValue/gsml:value",
                doc);
        // test GEOT-3304
        assertXpathEvaluatesTo(
                "approximate",
                "//ex:PolymorphicFeature[@gml:id='f6']/ex:secondValue/gsml:CGI_TermValue/@gsml:qualifier",
                doc);
    }

    /** This is to test that polymorphism can be achieved with feature chaining alone. */
    private void checkFeatureChainingOnly(Document doc) {

        // f1: make sure only 1 gsml:CGI_NumericValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f1']/ex:thirdValue", doc);
        assertXpathCount(
                1,
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:thirdValue/gsml:CGI_NumericValue",
                doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f1']/ex:thirdValue/gsml:CGI_TermValue", doc);
        assertXpathEvaluatesTo(
                "1.0",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:thirdValue/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:thirdValue/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);

        // f2: make sure only 1 gsml:CGI_TermValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f2']/ex:thirdValue", doc);
        assertXpathCount(
                1, "//ex:PolymorphicFeature[@gml:id='f2']/ex:thirdValue/gsml:CGI_TermValue", doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:thirdValue/gsml:CGI_NumericValue",
                doc);
        assertXpathEvaluatesTo(
                "x",
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:thirdValue/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "some:uri",
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:thirdValue/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        // f3: make sure only 1 gsml:CGI_TermValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f3']/ex:thirdValue", doc);
        assertXpathCount(
                1, "//ex:PolymorphicFeature[@gml:id='f3']/ex:thirdValue/gsml:CGI_TermValue", doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:thirdValue/gsml:CGI_NumericValue",
                doc);
        assertXpathEvaluatesTo(
                "y",
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:thirdValue/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "some:uri",
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:thirdValue/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        // f4: make sure only 1 gsml:CGI_NumericValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f4']/ex:thirdValue", doc);
        assertXpathCount(
                1,
                "//ex:PolymorphicFeature[@gml:id='f4']/ex:thirdValue/gsml:CGI_NumericValue",
                doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f4']/ex:thirdValue/gsml:CGI_TermValue", doc);
        assertXpathEvaluatesTo(
                "1.0",
                "//ex:PolymorphicFeature[@gml:id='f4']/ex:thirdValue/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//ex:PolymorphicFeature[@gml:id='f4']/ex:thirdValue/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);

        // f5: make sure only 1 gsml:CGI_TermValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f5']/ex:thirdValue", doc);
        assertXpathCount(
                1, "//ex:PolymorphicFeature[@gml:id='f5']/ex:thirdValue/gsml:CGI_TermValue", doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:thirdValue/gsml:CGI_NumericValue",
                doc);
        assertXpathEvaluatesTo(
                "y",
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:thirdValue/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "some:uri",
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:thirdValue/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        // f6: make sure only 1 gsml:CGI_NumericValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f6']/ex:thirdValue", doc);
        assertXpathCount(
                1,
                "//ex:PolymorphicFeature[@gml:id='f6']/ex:thirdValue/gsml:CGI_NumericValue",
                doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f6']/ex:thirdValue/gsml:CGI_TermValue", doc);
        assertXpathEvaluatesTo(
                "1000.0",
                "//ex:PolymorphicFeature[@gml:id='f6']/ex:thirdValue/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//ex:PolymorphicFeature[@gml:id='f6']/ex:thirdValue/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);
    }

    /** This is to test referential polymorphism, mixed with sub-type polymorphism. */
    private void checkXlinkHrefValues(Document doc) {

        // f1: make sure only null reference is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f1']/ex:fourthValue", doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:fourthValue/ex:firstParentFeature",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:fourthValue/@xlink:href",
                doc);

        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f1']/ex:fifthValue", doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:fifthValue/ex:firstParentFeature",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:fifthValue/@xlink:href",
                doc);

        // f2: make sure only 1 null reference is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f2']/ex:fourthValue", doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:fourthValue/ex:firstParentFeature",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:fourthValue/@xlink:href",
                doc);

        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f2']/ex:fifthValue", doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:fifthValue/ex:firstParentFeature",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:fifthValue/@xlink:href",
                doc);

        // f3: make sure only 1 null reference is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f3']/ex:fourthValue", doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:fourthValue/ex:firstParentFeature",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:fourthValue/@xlink:href",
                doc);

        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f3']/ex:fifthValue", doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:fifthValue/ex:firstParentFeature",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:fifthValue/@xlink:href",
                doc);

        // f4: make sure only 1 null reference is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f4']/ex:fourthValue", doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f4']/ex:fourthValue/ex:firstParentFeature",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//ex:PolymorphicFeature[@gml:id='f4']/ex:fourthValue/@xlink:href",
                doc);

        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f4']/ex:fifthValue", doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f4']/ex:fifthValue/ex:firstParentFeature",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//ex:PolymorphicFeature[@gml:id='f4']/ex:fifthValue/@xlink:href",
                doc);

        // f5: make sure only 1 null reference is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f5']/ex:fourthValue", doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:fourthValue/ex:firstParentFeature",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:fourthValue/@xlink:href",
                doc);

        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f5']/ex:fifthValue", doc);
        assertXpathCount(
                0,
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:fifthValue/ex:firstParentFeature",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:fifthValue/@xlink:href",
                doc);

        // f6: make sure only 1 gsml:CGI_NumericValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f6']/ex:fourthValue", doc);
        assertXpathCount(
                1,
                "//ex:PolymorphicFeature[@gml:id='f6']/ex:fourthValue/gsml:CGI_NumericValue",
                doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f6']/ex:fourthValue/gsml:CGI_TermValue", doc);
        assertXpathEvaluatesTo(
                "1000.0",
                "//ex:PolymorphicFeature[@gml:id='f6']/ex:fourthValue/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//ex:PolymorphicFeature[@gml:id='f6']/ex:fourthValue/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);

        // GEOT:4417
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f6']/ex:fifthValue", doc);
        assertXpathCount(
                1,
                "//ex:PolymorphicFeature[@gml:id='f6']/ex:fifthValue/gsml:CGI_NumericValue",
                doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f6']/ex:fifthValue/gsml:CGI_TermValue", doc);
        assertXpathEvaluatesTo(
                "1000.0",
                "//ex:PolymorphicFeature[@gml:id='f6']/ex:fifthValue/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//ex:PolymorphicFeature[@gml:id='f6']/ex:fifthValue/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);
    }

    /** This is to test that conditional polymorphism works with xs:anyType. */
    private void checkAnyType(Document doc) {

        // f1: make sure only 1 ex:AnyValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f1']/ex:anyValue", doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f1']/ex:anyValue/gsml:CGI_TermValue", doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f1']/ex:anyValue/gsml:CGI_NumericValue", doc);
        assertXpathEvaluatesTo(
                "urn:numeric-value::1",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:anyValue/@xlink:href",
                doc);

        // f2: make sure only 1 ex:anyValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f2']/ex:anyValue", doc);
        assertXpathCount(
                1, "//ex:PolymorphicFeature[@gml:id='f2']/ex:anyValue/gsml:CGI_TermValue", doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f2']/ex:anyValue/gsml:CGI_NumericValue", doc);
        assertXpathEvaluatesTo(
                "0",
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:anyValue/gsml:CGI_TermValue/gsml:value",
                doc);
        // test GEOT-3304
        assertXpathEvaluatesTo(
                "approximate",
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:anyValue/gsml:CGI_TermValue/@gsml:qualifier",
                doc);

        // f3: make sure only 1 ex:anyValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f3']/ex:anyValue", doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f3']/ex:anyValue/gsml:CGI_TermValue", doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f3']/ex:anyValue/gsml:CGI_NumericValue", doc);
        assertXpathEvaluatesTo(
                "urn:numeric-value::0",
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:anyValue/@xlink:href",
                doc);

        // f4: make sure there's only null reference encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f4']/ex:anyValue", doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f4']/ex:anyValue/gsml:CGI_TermValue", doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f4']/ex:anyValue/gsml:CGI_NumericValue", doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//ex:PolymorphicFeature[@gml:id='f4']/ex:anyValue/@xlink:href",
                doc);

        // f5: make sure only 1 ex:anyValue is encoded
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f5']/ex:anyValue", doc);
        assertXpathCount(
                1, "//ex:PolymorphicFeature[@gml:id='f5']/ex:anyValue/gsml:CGI_TermValue", doc);
        assertXpathCount(
                0, "//ex:PolymorphicFeature[@gml:id='f5']/ex:anyValue/gsml:CGI_NumericValue", doc);
        assertXpathEvaluatesTo(
                "0",
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:anyValue/gsml:CGI_TermValue/gsml:value",
                doc);
        // test GEOT-3304
        assertXpathEvaluatesTo(
                "approximate",
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:anyValue/gsml:CGI_TermValue/@gsml:qualifier",
                doc);

        // f6: make sure nothing is encoded
        assertXpathCount(0, "//ex:PolymorphicFeature[@gml:id='f6']/ex:anyValue", doc);
    }

    @Test
    public void testNestedFilterEncoding() throws FilterToSQLException, IOException {
        FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName("ex", "PolymorphicFeature");
        FeatureSource fs = ftInfo.getFeatureSource(new NullProgressListener(), null);
        AppSchemaDataAccess da = (AppSchemaDataAccess) fs.getDataStore();
        FeatureTypeMapping rootMapping = da.getMappingByNameOrElement(ftInfo.getQualifiedName());

        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        JDBCDataStore store = (JDBCDataStore) rootMapping.getSource().getDataStore();

        FilterFactoryImplNamespaceAware ff = new FilterFactoryImplNamespaceAware();
        ff.setNamepaceContext(rootMapping.getNamespaces());

        /*
         * test equals filter on first nested attribute
         */
        PropertyIsEqualTo equalsFirstValue =
                ff.equals(
                        ff.property("ex:firstValue/gsml:CGI_NumericValue/gsml:principalValue"),
                        ff.literal(1.0));

        // Filter involving conditional polymorphism --> CANNOT be encoded
        ComplexFilterSplitter splitterFirstValueFilter =
                new ComplexFilterSplitter(store.getFilterCapabilities(), rootMapping);
        splitterFirstValueFilter.visit(equalsFirstValue, null);
        Filter preFilter = splitterFirstValueFilter.getFilterPre();
        Filter postFilter = splitterFirstValueFilter.getFilterPost();

        assertEquals(Filter.INCLUDE, preFilter);
        assertEquals(equalsFirstValue, postFilter);

        // filter must be "unrolled" (i.e. reverse mapped) first
        Filter unrolled = AppSchemaDataAccess.unrollFilter(equalsFirstValue, rootMapping);

        // Filter is nested
        assertTrue(NestedFilterToSQL.isNestedFilter(unrolled));

        assertContainsFeatures(fs.getFeatures(equalsFirstValue), "f1");

        /*
         * test equals filter on second nested attribute
         */
        PropertyIsEqualTo equalsSecondValue =
                ff.equals(
                        ff.property(
                                "ex:secondValue/gsml:CGI_NumericValue/gsml:principalValue/@uom"),
                        ff.literal("m"));

        // Filter involving conditional polymorphism --> CANNOT be encoded
        ComplexFilterSplitter splitterSecondValue =
                new ComplexFilterSplitter(store.getFilterCapabilities(), rootMapping);
        splitterSecondValue.visit(equalsSecondValue, null);
        preFilter = splitterSecondValue.getFilterPre();
        postFilter = splitterSecondValue.getFilterPost();

        assertEquals(Filter.INCLUDE, preFilter);
        assertEquals(equalsSecondValue, postFilter);

        // filter must be "unrolled" (i.e. reverse mapped) first
        unrolled = AppSchemaDataAccess.unrollFilter(equalsSecondValue, rootMapping);

        // Filter is nested
        assertTrue(NestedFilterToSQL.isNestedFilter(unrolled));

        assertContainsFeatures(fs.getFeatures(equalsSecondValue), "f1", "f3");

        /*
         * test equals filter on third nested attribute
         */
        PropertyIsEqualTo equalsThirdValue =
                ff.equals(
                        ff.property("ex:thirdValue/gsml:CGI_NumericValue/gsml:principalValue"),
                        ff.literal(1.0));

        // Filter involving non-conditional polymorphism --> can be encoded, because target
        // attribute mapping can be unequivocally determined a-priori
        ComplexFilterSplitter splitterThirdValueFilter =
                new ComplexFilterSplitter(store.getFilterCapabilities(), rootMapping);
        splitterThirdValueFilter.visit(equalsThirdValue, null);
        preFilter = splitterThirdValueFilter.getFilterPre();
        postFilter = splitterThirdValueFilter.getFilterPost();

        assertEquals(equalsThirdValue, preFilter);
        assertEquals(Filter.INCLUDE, postFilter);

        // filter must be "unrolled" (i.e. reverse mapped) first
        unrolled = AppSchemaDataAccess.unrollFilter(equalsThirdValue, rootMapping);

        // Filter is nested
        assertTrue(NestedFilterToSQL.isNestedFilter(unrolled));

        NestedFilterToSQL nestedFilterEncoder = createNestedFilterEncoder(rootMapping);
        String encodedFilter = nestedFilterEncoder.encodeToString(unrolled);

        // this is the generated query in PostGIS, but the test limits to check the presence of the
        // EXISTS keyword, as the actual SQL is dependent on the underlying database
        // EXISTS (SELECT "chain_link_1"."PKEY"
        //      FROM "appschematest"."POLYMORPHICFEATURE" "chain_link_1"
        //      WHERE "chain_link_1"."NUMERIC_VALUE" = 1.0 AND
        //            "appschematest"."POLYMORPHICFEATURE"."VALUE_ID" = "chain_link_1"."ID"
        assertTrue(encodedFilter.contains("EXISTS"));

        assertContainsFeatures(fs.getFeatures(equalsThirdValue), "f1", "f4");
    }
}
