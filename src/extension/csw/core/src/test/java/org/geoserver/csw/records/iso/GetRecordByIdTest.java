/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records.iso;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import org.junit.Test;
import org.w3c.dom.Document;

/** @author Niels Charlier */
public class GetRecordByIdTest extends MDTestSupport {

    @Test
    public void test() throws Exception {
        String forestId = getCatalog().getLayerByName("Forests").getResource().getId();

        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecordById&typeNames=gmd:MD_Metadata&outputSchema=http://www.isotc211.org/2005/gmd&id="
                        + forestId;
        Document d = getAsDOM(request);
        print(d);
        // validateSchema(d.getElementsByTagName("//gmd:MD_MetaData"));

        // check we have the expected results
        // we have the right kind of document
        assertXpathEvaluatesTo("1", "count(/csw:GetRecordByIdResponse)", d);
        // check contents Forests record
        assertXpathEvaluatesTo(
                "abstract about Forests",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:AbstractMD_Identification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:AbstractMD_Identification/gmd:abstract/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "Forests",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:AbstractMD_Identification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:AbstractMD_Identification/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "http://purl.org/dc/dcmitype/Dataset",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:AbstractMD_Identification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:hierarchyLevel/gmd:MD_ScopeCode/@codeListValue",
                d);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:6.11:4326",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:AbstractMD_Identification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/@crs",
                d);
        assertXpathEvaluatesTo(
                "-90.0",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:AbstractMD_Identification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:westBoundLongitude",
                d);
        assertXpathEvaluatesTo(
                "-180.0",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:AbstractMD_Identification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:southBoundLatitude",
                d);
        assertXpathEvaluatesTo(
                "90.0",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:AbstractMD_Identification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:eastBoundLongitude",
                d);
        assertXpathEvaluatesTo(
                "180.0",
                "//gmd:MD_Metadata[gmd:identificationInfo/gmd:AbstractMD_Identification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString='Forests']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:northBoundLatitude",
                d);
    }
}
