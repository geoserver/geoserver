/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal.iso;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.File;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class MultipleMappingTest extends MDTestSupport {

    private File secondMappingFileIgnore,
            secondMappingFile,
            secondMappingFileIgnore2,
            secondMappingFile2;

    @Before
    public void load() {
        // copy all mappings into the data directory
        secondMappingFileIgnore =
                new File(
                        testData.getDataDirectoryRoot(),
                        "csw/MD_Metadata-second.properties.ignore");
        secondMappingFile =
                new File(testData.getDataDirectoryRoot(), "csw/MD_Metadata-second.properties");
        // copy all mappings into the data directory
        secondMappingFileIgnore2 =
                new File(testData.getDataDirectoryRoot(), "csw/Record-second.properties.ignore");
        secondMappingFile2 =
                new File(testData.getDataDirectoryRoot(), "csw/Record-second.properties");

        secondMappingFileIgnore.renameTo(secondMappingFile);
        secondMappingFileIgnore2.renameTo(secondMappingFile2);
    }

    @After
    public void restore() {
        secondMappingFile.renameTo(secondMappingFileIgnore);
        secondMappingFile2.renameTo(secondMappingFileIgnore2);
    }

    @Test
    public void testGetRecordById() throws Exception {
        String forestId = getCatalog().getLayerByName("Forests").getResource().getId();

        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecordById&typeNames=gmd:MD_Metadata&outputSchema=http://www.isotc211.org/2005/gmd&id="
                        + forestId
                        + ".second";
        Document d = getAsDOM(request);
        assertXpathEvaluatesTo(
                "Forests",
                "//gmd:MD_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "Jeffery Smith",
                "//gmd:MD_Metadata/gmd:contact/gmd:CI_ResponsibleParty/gmd:individualName/gco:CharacterString",
                d);
    }

    @Test
    public void testTitleFilter() throws Exception {
        String forestId = getCatalog().getLayerByName("Forests").getResource().getId();

        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=brief&outputSchema=http://www.isotc211.org/2005/gmd&constraint=Title = 'Forests'";
        Document d = getAsDOM(request);
        print(d);
        // validateSchema(d.getElementsByTagName("//gmd:MD_MetaData"));

        assertXpathEvaluatesTo("2", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("2", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("2", "count(//csw:SearchResults/*)", d);
        assertXpathEvaluatesTo(
                "Forests",
                "//gmd:MD_Metadata[gmd:fileIdentifier/gco:CharacterString='"
                        + forestId
                        + "']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "Forests",
                "//gmd:MD_Metadata[gmd:fileIdentifier/gco:CharacterString='"
                        + forestId
                        + ".second']/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "John Smith",
                "//gmd:MD_Metadata[gmd:fileIdentifier/gco:CharacterString='"
                        + forestId
                        + "']/gmd:contact/gmd:CI_ResponsibleParty/gmd:individualName/gco:CharacterString",
                d);
        assertXpathEvaluatesTo(
                "Jeffery Smith",
                "//gmd:MD_Metadata[gmd:fileIdentifier/gco:CharacterString='"
                        + forestId
                        + ".second']/gmd:contact/gmd:CI_ResponsibleParty/gmd:individualName/gco:CharacterString",
                d);
    }

    @Test
    public void testTitleFilterCSWRecord() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&elementSetName=brief&outputSchema=http://www.isotc211.org/2005/gmd&constraint=dc:title = 'Forests.second'";
        Document d = getAsDOM(request);

        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("1", "count(//csw:SearchResults/*)", d);
        assertXpathEvaluatesTo(
                "Forests",
                "//gmd:MD_Metadata[1]/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString",
                d);
    }
}
