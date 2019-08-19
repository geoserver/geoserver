/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.geotools.csw.CSWConfiguration;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.w3c.dom.Document;

/** @author Niels Charlier */
public class GetRecordsTest extends CSWInternalTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // insert extra metadata
        ResourceInfo forestInfo = getCatalog().getLayerByName("Forests").getResource();
        forestInfo.getMetadata().put("date", "09/10/2012");
        forestInfo.setLatLonBoundingBox(
                new ReferencedEnvelope(-200, -180, -100, -90, CRS.decode("EPSG:4326")));
        getCatalog().save(forestInfo);
    }

    @Test
    public void testAllRecordsPaged() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record"
                        + "&resultType=results&elementSetName=full";

        Document d = getAsDOM(request);
        // print(d);
        // validateSchema(d.getElementsByTagName("//gmd:MD_MetaData"));

        // we have the right kind of document
        assertXpathEvaluatesTo("1", "count(/csw:GetRecordsResponse)", d);

        // check we have the expected results
        assertXpathEvaluatesTo("full", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("29", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("10", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("11", "//csw:SearchResults/@nextRecord", d);
        assertXpathEvaluatesTo("10", "count(//csw:SearchResults/*)", d);
    }

    @Test
    public void testAllRecords() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record"
                        + "&resultType=results&elementSetName=full&maxRecords=100";
        Document d = getAsDOM(request);
        // print(d);
        checkValidationErrors(d, new CSWConfiguration());

        // we have the right kind of document
        assertXpathEvaluatesTo("1", "count(/csw:GetRecordsResponse)", d);

        // check we have the expected results
        assertXpathEvaluatesTo("full", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("29", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("29", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@nextRecord", d);
        assertXpathEvaluatesTo("29", "count(//csw:SearchResults/*)", d);

        // check contents Forests record
        assertXpathEvaluatesTo(
                "abstract about Forests", "//csw:Record[dc:title='Forests']/dct:abstract", d);
        assertXpathEvaluatesTo(
                "description about Forests", "//csw:Record[dc:title='Forests']/dc:description", d);
        assertXpathEvaluatesTo("Forests", "//csw:Record[dc:title='Forests']/dc:subject", d);
        assertXpathEvaluatesTo(
                "http://purl.org/dc/dcmitype/Dataset",
                "//csw:Record[dc:title='Forests']/dc:type",
                d);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:6.11:4326",
                "//csw:Record[dc:title='Forests']/ows:BoundingBox/@crs",
                d);
        assertXpathEvaluatesTo(
                "-100.0 -200.0",
                "//csw:Record[dc:title='Forests']/ows:BoundingBox/ows:LowerCorner",
                d);
        assertXpathEvaluatesTo(
                "-90.0 -180.0",
                "//csw:Record[dc:title='Forests']/ows:BoundingBox/ows:UpperCorner",
                d);
        // custom metadata
        assertXpathEvaluatesTo("09/10/2012", "//csw:Record[dc:title='Forests']/dc:date", d);
        // scheme attribute
        assertXpathEvaluatesTo(
                "http://www.digest.org/2.1",
                "//csw:Record[dc:title='Forests']/dc:subject/@scheme",
                d);
    }

    @Test
    public void testAllRecordsWithOffset() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&StartPosition=11&elementSetName=full";
        Document d = getAsDOM(request);
        // print(d);
        checkValidationErrors(d, new CSWConfiguration());

        // check we have the right kind of document
        assertXpathEvaluatesTo("1", "count(/csw:GetRecordsResponse)", d);

        // check we have the expected results
        assertXpathEvaluatesTo("full", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("29", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("10", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("21", "//csw:SearchResults/@nextRecord", d);
        assertXpathEvaluatesTo("10", "count(//csw:SearchResults/*)", d);
    }

    @Test
    public void testAllRecordsWithMax() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&StartPosition=11&maxRecords=5&elementSetName=full";
        Document d = getAsDOM(request);
        // print(d);
        checkValidationErrors(d, new CSWConfiguration());

        // check we have the right kind of document
        assertXpathEvaluatesTo("1", "count(/csw:GetRecordsResponse)", d);

        // check we have the expected results
        assertXpathEvaluatesTo("full", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("29", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("5", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("16", "//csw:SearchResults/@nextRecord", d);
        assertXpathEvaluatesTo("5", "count(//csw:SearchResults/*)", d);
    }

    @Test
    public void testTitleFilter1() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&elementSetName=brief&constraint=dc:title = 'Forests'";
        Document d = getAsDOM(request);
        // print(d);

        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("1", "count(//csw:SearchResults/*)", d);
        assertXpathEvaluatesTo("Forests", "//csw:BriefRecord/dc:title", d);
    }

    @Test
    public void testTitleFilter2() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&elementSetName=brief&constraint=dc:title like 'S%25'";
        Document d = getAsDOM(request);
        // print(d);

        assertXpathEvaluatesTo("2", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("2", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("2", "count(//csw:SearchResults/*)", d);
        assertXpathExists("//csw:BriefRecord[dc:title='Streams']", d);
        assertXpathExists("//csw:BriefRecord[dc:title='Seven']", d);
    }

    @Test
    public void testFullTextSearch() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&elementSetName=brief&constraint=AnyText like '%25about B%25'";
        Document d = getAsDOM(request);
        checkValidationErrors(d, new CSWConfiguration());
        print(d);

        // basic checks
        assertXpathEvaluatesTo("3", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("3", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@nextRecord", d);
        assertXpathEvaluatesTo("3", "count(//csw:SearchResults/*)", d);

        assertXpathExists("//csw:BriefRecord[dc:title='BasicPolygons']", d);
        assertXpathExists("//csw:BriefRecord[dc:title='Bridges']", d);
        assertXpathExists("//csw:BriefRecord[dc:title='Buildings']", d);
    }

    @Test
    public void testFilterBBox() throws Exception {

        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results"
                        + "&constraint=BBOX(ows:BoundingBox, -250, -250, -190, -100)&maxRecords=100";
        Document d = getAsDOM(request);
        checkValidationErrors(d, new CSWConfiguration());
        // print(d);

        // basic checks
        // assertXpathEvaluatesTo("15", "//csw:SearchResults/@numberOfRecordsMatched", d);
        // assertXpathEvaluatesTo("10", "//csw:SearchResults/@numberOfRecordsReturned", d);
        // assertXpathEvaluatesTo("11", "//csw:SearchResults/@nextRecord", d);
        // assertXpathEvaluatesTo("10", "count(//csw:SearchResults/*)", d);

        // verify we got the expected records

        assertXpathExists("//csw:SummaryRecord[dc:title='Forests']", d);
    }

    /**
     * From CITE compliance, throw an error if a non spatial property is used in a spatial filter
     */
    @Test
    public void testSpatialFilterNonGeomProperty() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results"
                        + "&elementName=dc:identifier,ows:BoundingBox&constraint=BBOX(dct:spatial, -250, -250, -190, -100)";
        Document d = getAsDOM(request);
        // print(d);
        checkOws10Exception(d);
    }

    /** From CITE compliance, throw an error the output format is not supported */
    @Test
    public void testUnsupportedOutputFormat() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&outputFormat=application/xhtml+xml";
        Document d = getAsDOM(request);
        print(d);
        checkOws10Exception(d, ServiceException.INVALID_PARAMETER_VALUE, "outputFormat");
    }

    @Test
    public void testUnadvertised() throws Exception {
        // unadvertise layer
        ResourceInfo forests = getCatalog().getResourceByName("Forests", ResourceInfo.class);
        forests.setAdvertised(false);
        getCatalog().save(forests);

        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record"
                        + "&resultType=results&elementSetName=full&maxRecords=100";
        Document d = getAsDOM(request);
        // print(d);
        checkValidationErrors(d, new CSWConfiguration());

        // we have the right kind of document
        assertXpathEvaluatesTo("1", "count(/csw:GetRecordsResponse)", d);

        // check we have the expected results
        assertXpathEvaluatesTo("28", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("28", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@nextRecord", d);
        assertXpathEvaluatesTo("28", "count(//csw:SearchResults/*)", d);

        // check contents Forests record
        assertXpathNotExists("//csw:Record[dc:title='Forests']", d);

        // restore catalog
        forests.setAdvertised(true);
        getCatalog().save(forests);
    }
}
