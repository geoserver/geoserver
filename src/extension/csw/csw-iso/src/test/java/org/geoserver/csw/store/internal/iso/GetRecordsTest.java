/* (c) 2017 -2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal.iso;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.w3c.dom.Document;

/** @author Niels Charlier */
public class GetRecordsTest extends MDTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // insert extra metadata
        ResourceInfo forestInfo = getCatalog().getLayerByName("Forests").getResource();
        forestInfo.getMetadata().put("date", "09/10/2012");
        forestInfo.getMetadata().put("contact", "blabla");
        forestInfo.getMetadata().put("contact-href", "http://blabla");
        forestInfo.getMetadata().put("ref-system", Lists.newArrayList("ref-system-one", "ref-system-two"));
        forestInfo.setLatLonBoundingBox(new ReferencedEnvelope(-200, -180, -100, -90, CRS.decode("EPSG:4326")));
        forestInfo.getKeywords().add(new Keyword("CustomKeyWord-1"));
        forestInfo.getKeywords().add(new Keyword("CustomKeyWord-2"));
        forestInfo.setDescription("Land with lots of trees on.");
        forestInfo.getAlias().add("Bush");
        forestInfo.getAlias().add("Woods");
        getCatalog().save(forestInfo);
    }

    @Test
    public void testAllRecordsPaged() throws Exception {
        String request = "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata"
                + "&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd";

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
        String request = "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata"
                + "&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd"
                + "&maxRecords=100";
        Document d = getAsDOM(request);
        // print(d);
        // validateSchema(d.getElementsByTagName("//gmd:MD_MetaData"));

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
                "abstract about Forests",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:abstract/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "Forests",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "09/10/2012",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:dateStamp/gco:Date",
                d);
        assertXpathEvaluatesTo(
                "http://purl.org/dc/dcmitype/Dataset",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:hierarchyLevel/gmd:MD_ScopeCode/@codeListValue",
                d);
        assertXpathEvaluatesTo(
                "-200.0",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:westBoundLongitude/gco:Decimal",
                d);
        assertXpathEvaluatesTo(
                "-100.0",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:southBoundLatitude/gco:Decimal",
                d);
        assertXpathEvaluatesTo(
                "-180.0",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:eastBoundLongitude/gco:Decimal",
                d);
        assertXpathEvaluatesTo(
                "-90.0",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:northBoundLatitude/gco:Decimal",
                d);

        // check the multi-valued field alternate title
        assertXpathEvaluatesTo(
                "Land with lots of trees on.",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:alternateTitle[1]/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "Bush",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:alternateTitle[2]/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "Woods",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:alternateTitle[3]/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "##Forests",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:alternateTitle[4]/gco:CharacterString",
                d);

        // check the service information
        assertXpathEvaluatesTo(
                "OGC:WFS",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions/gmd:onLine/gmd:CI_OnlineResource[1]/gmd:protocol/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "Forests",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions/gmd:onLine/gmd:CI_OnlineResource[1]/gmd:name/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/wfs",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions/gmd:onLine/gmd:CI_OnlineResource[1]/gmd:linkage/gmd:URL",
                d);
        assertXpathEvaluatesTo(
                "OGC:WMS",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions/gmd:onLine/gmd:CI_OnlineResource[2]/gmd:protocol/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "Forests",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions/gmd:onLine/gmd:CI_OnlineResource[2]/gmd:name/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/wms",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions/gmd:onLine/gmd:CI_OnlineResource[2]/gmd:linkage/gmd:URL",
                d);
        assertXpathEvaluatesTo(
                "OGC:WCS",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions/gmd:onLine/gmd:CI_OnlineResource[3]/gmd:protocol/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "Forests",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions/gmd:onLine/gmd:CI_OnlineResource[3]/gmd:name/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/wcs",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions/gmd:onLine/gmd:CI_OnlineResource[3]/gmd:linkage/gmd:URL",
                d);

        // test SRV
        assertXpathEvaluatesTo(
                "srv_works",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/srv:SV_ServiceIdentification/gmd:citation",
                d);
        assertXpathEvaluatesTo(
                "srv_works",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/srv:SV_ServiceIdentification/srv:serviceType/gco:LocalName/@codeSpace",
                d);
        assertXpathEvaluatesTo(
                "srv_works",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/srv:SV_ServiceIdentification/srv:serviceTypeVersion/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "srv_works",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/srv:SV_ServiceIdentification/srv:couplingType/srv:SV_CouplingType/@codeListValue",
                d);

        // check contact anchor href
        assertXpathEvaluatesTo(
                "blabla",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:contact/gmd:CI_ResponsibleParty/gmd:organisationName/gmx:Anchor",
                d);
        assertXpathEvaluatesTo(
                "http://blabla",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:contact/gmd:CI_ResponsibleParty/gmd:organisationName/gmx:Anchor/@xlink:href",
                d);

        assertXpathEvaluatesTo(
                "therole",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:pointOfContact/gmd:CI_ResponsibleParty[1]/gmd:role/gmd:CI_RoleCode",
                d);
        assertXpathEvaluatesTo(
                "therole",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:pointOfContact/gmd:CI_ResponsibleParty[2]/gmd:role/gmd:CI_RoleCode",
                d);

        // check indexed mappings
        assertXpathEvaluatesTo(
                "second/first key",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords[2]/gmd:MD_Keywords/gmd:keyword[1]/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "second/second key",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords[2]/gmd:MD_Keywords/gmd:keyword[2]/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "nonsense",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords[2]/gmd:MD_Keywords/gmd:type/gmd:MD_KeywordTypeCode",
                d);
        assertXpathEvaluatesTo(
                "third/first key",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords[3]/gmd:MD_Keywords/gmd:keyword[1]/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "third/second key",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords[3]/gmd:MD_Keywords/gmd:keyword[2]/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "third/third key",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords[3]/gmd:MD_Keywords/gmd:keyword[3]/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "third/fourth key",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords[3]/gmd:MD_Keywords/gmd:keyword[4]/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "nonsense",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords[3]/gmd:MD_Keywords/gmd:type/gmd:MD_KeywordTypeCode",
                d);
        assertXpathEvaluatesTo(
                "fourth key",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords[4]/gmd:MD_Keywords/gmd:keyword[1]/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "nonsense",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords[4]/gmd:MD_Keywords/gmd:type/gmd:MD_KeywordTypeCode",
                d);
        assertXpathEvaluatesTo(
                "fifth key",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords[5]/gmd:MD_Keywords/gmd:keyword[1]/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "nonsense",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords[5]/gmd:MD_Keywords/gmd:type/gmd:MD_KeywordTypeCode",
                d);
    }

    @Test
    public void testAllRecordsBrief() throws Exception {
        String request = "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata"
                + "&resultType=results&elementSetName=brief&outputSchema=http://www.isotc211.org/2005/gmd"
                + "&maxRecords=100";
        Document d = getAsDOM(request);
        // print(d);
        // validateSchema(d.getElementsByTagName("//gmd:MD_MetaData"));

        // check we have the expected results
        assertXpathEvaluatesTo("brief", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("29", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("29", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@nextRecord", d);
        assertXpathEvaluatesTo("29", "count(//csw:SearchResults/*)", d);

        // check contents Forests record
        assertXpathNotExists(
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:abstract/gco:CharacterString",
                d);
        assertXpathNotExists(
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword/gco:CharacterString",
                d);
        assertXpathNotExists(
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:hierarchyLevel/gmd:MD_ScopeCode/@codeListValue",
                d);
        assertXpathEvaluatesTo(
                "-200.0",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:westBoundLongitude/gco:Decimal",
                d);
        assertXpathEvaluatesTo(
                "-100.0",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:southBoundLatitude/gco:Decimal",
                d);
        assertXpathEvaluatesTo(
                "-180.0",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:eastBoundLongitude/gco:Decimal",
                d);
        assertXpathEvaluatesTo(
                "-90.0",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:northBoundLatitude/gco:Decimal",
                d);
        assertXpathNotExists(
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:dateStamp/gco:Date/@xsi:nil",
                d);
    }

    @Test
    public void testTitleFilter1() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=brief&outputSchema=http://www.isotc211.org/2005/gmd&constraint=Title = 'Forests'";
        Document d = getAsDOM(request);
        // print(d);
        // validateSchema(d.getElementsByTagName("//gmd:MD_MetaData"));

        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("1", "count(//csw:SearchResults/*)", d);
        assertXpathEvaluatesTo(
                "Forests",
                "//gmd:MD_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString",
                d);
    }

    @Test
    public void testTitleFilter2() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=brief&outputSchema=http://www.isotc211.org/2005/gmd&constraint=Title like 'S%25'";
        Document d = getAsDOM(request);
        // print(d);
        // validateSchema(d.getElementsByTagName("//gmd:MD_MetaData"));

        assertXpathEvaluatesTo("2", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("2", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("2", "count(//csw:SearchResults/*)", d);
        assertXpathExists(
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Streams']",
                d);
        assertXpathExists(
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Seven']",
                d);
    }

    @Test
    public void testTitleFilterMultiQueryable() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=brief&outputSchema=http://www.isotc211.org/2005/gmd&constraint=Contact = 'blabla'";
        Document d = getAsDOM(request);
        // print(d);
        // validateSchema(d.getElementsByTagName("//gmd:MD_MetaData"));

        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsMatched", d);

        request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=brief&outputSchema=http://www.isotc211.org/2005/gmd&constraint=Contact = 'The Organisation'";
        d = getAsDOM(request);
        // print(d);
        // validateSchema(d.getElementsByTagName("//gmd:MD_MetaData"));

        assertXpathEvaluatesTo("28", "//csw:SearchResults/@numberOfRecordsMatched", d);
    }

    @Test
    public void testFullTextSearch() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=brief&outputSchema=http://www.isotc211.org/2005/gmd&constraint=AnyText like '%25about B%25'";
        Document d = getAsDOM(request);
        // print(d);
        // validateSchema(d.getElementsByTagName("//gmd:MD_MetaData"));

        // basic checks
        assertXpathEvaluatesTo("3", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("3", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@nextRecord", d);
        assertXpathEvaluatesTo("3", "count(//csw:SearchResults/*)", d);

        assertXpathExists(
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='BasicPolygons']",
                d);
        assertXpathExists(
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Bridges']",
                d);
        assertXpathExists(
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Buildings']",
                d);
    }

    @Test
    public void testFilterBBox() throws Exception {

        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&outputSchema=http://www.isotc211.org/2005/gmd&typeNames=gmd:MD_Metadata&resultType=results"
                        + "&constraint=BBOX(BoundingBox, -250, -250, -190, -100)&maxRecords=100";
        Document d = getAsDOM(request);
        // print(d);
        // validateSchema(d.getElementsByTagName("//gmd:MD_MetaData"));

        // basic checks
        // assertXpathEvaluatesTo("15", "//csw:SearchResults/@numberOfRecordsMatched", d);
        // assertXpathEvaluatesTo("10", "//csw:SearchResults/@numberOfRecordsReturned", d);
        // assertXpathEvaluatesTo("11", "//csw:SearchResults/@nextRecord", d);
        // assertXpathEvaluatesTo("10", "count(//csw:SearchResults/*)", d);

        // verify we got the expected records

        assertXpathExists(
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']",
                d);
    }

    @Test
    public void testFilterIndexed() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd&constraint=ReferenceSystem='EPSG:4326'";
        Document d = getAsDOM(request);
        // print(d);
        // validateSchema(d.getElementsByTagName("//gmd:MD_MetaData"));
        assertXpathEvaluatesTo("15", "//csw:SearchResults/@numberOfRecordsMatched", d);

        request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd"
                        + "&constraint=\"gmd:referenceSystemInfo/gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:code/gco:CharacterString\"='ref-system-one'";
        d = getAsDOM(request);
        // print(d);
        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsMatched", d);

        request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd"
                        + "&constraint=\"gmd:referenceSystemInfo/gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:code/gco:CharacterString\"='EPSG:4326'";
        d = getAsDOM(request);
        // print(d);
        assertXpathEvaluatesTo("15", "//csw:SearchResults/@numberOfRecordsMatched", d);

        request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd&constraint=ReferenceSystem='ref-system-one'";
        d = getAsDOM(request);
        // print(d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@numberOfRecordsMatched", d);

        request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd"
                        + "&constraint=\"gmd:referenceSystemInfo[1]/gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:code/gco:CharacterString\"='EPSG:4326'";
        d = getAsDOM(request);
        // print(d);
        assertXpathEvaluatesTo("15", "//csw:SearchResults/@numberOfRecordsMatched", d);

        request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd"
                        + "&constraint=\"gmd:referenceSystemInfo[2]/gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:code/gco:CharacterString\"='EPSG:4326'";
        d = getAsDOM(request);
        print(d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@numberOfRecordsMatched", d);

        request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd"
                        + "&constraint=\"gmd:referenceSystemInfo[2]/gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:code/gco:CharacterString\"='ref-system-two'";
        d = getAsDOM(request);
        // print(d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@numberOfRecordsMatched", d);
    }

    @Test
    public void testTitleFilterCSWRecord() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&elementSetName=brief&outputSchema=http://www.isotc211.org/2005/gmd&constraint=dc:title like 'S%25'";
        Document d = getAsDOM(request);
        // print(d);
        // validateSchema(d.getElementsByTagName("//gmd:MD_MetaData"));

        assertXpathEvaluatesTo("2", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("2", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("2", "count(//csw:SearchResults/*)", d);
        assertXpathExists(
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Streams']",
                d);
        assertXpathExists(
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Seven']",
                d);
    }

    /** Tests CSW service disabled on layer-resource */
    @Test
    public void testLayerDisabledServiceRecords() throws Exception {
        disableCWSOnLinesLayer();
        String request = "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata"
                + "&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd";
        Document doc = getAsDOM(request);

        assertXpathEvaluatesTo(
                "0",
                "count(/csw:GetRecordsResponse/csw:SearchResults/gmd:MD_Metadata/"
                        + "gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions/"
                        + "gmd:onLine/gmd:CI_OnlineResource/gmd:name/gco:CharacterString[.='Lines'])",
                doc);
        enableCWSOnLinesLayer();
    }

    /** Tests CSW service enabled on layer-resource */
    @Test
    public void testLayerEnabledServiceRecords() throws Exception {
        enableCWSOnLinesLayer();
        String request = "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata"
                + "&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd";
        Document doc = getAsDOM(request);

        assertXpathEvaluatesTo(
                "3",
                "count(/csw:GetRecordsResponse/csw:SearchResults/gmd:MD_Metadata/"
                        + "gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions/"
                        + "gmd:onLine/gmd:CI_OnlineResource/gmd:name/gco:CharacterString[.='Lines'])",
                doc);
    }

    @Test
    public void testTitleFilterMetaDataRecordWithDCOutput() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&namespace=xmlns(gmd=http://www.isotc211.org/2005/gmd)&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=brief&constraint=Title='Forests'&outputSchema=http://www.opengis.net/cat/csw/2.0.2";
        Document d = getAsDOM(request);
        // print(d);

        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("1", "count(//csw:SearchResults/*)", d);
        assertXpathEvaluatesTo("Forests", "//csw:BriefRecord/dc:title", d);
    }

    private void enableCWSOnLinesLayer() {
        LayerInfo linfo = getCatalog().getLayerByName("Lines");
        ResourceInfo ri = linfo.getResource();
        ri.setServiceConfiguration(false);
        ri.setDisabledServices(new ArrayList<>());
        getCatalog().save(ri);
        getCatalog().save(linfo);
    }

    private void disableCWSOnLinesLayer() {
        LayerInfo linfo = getCatalog().getLayerByName("Lines");
        ResourceInfo ri = linfo.getResource();
        ri.setServiceConfiguration(true);
        ri.setDisabledServices(new ArrayList<>(Arrays.asList("CSW")));
        getCatalog().save(ri);
        getCatalog().save(linfo);
    }
}
