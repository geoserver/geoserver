/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.xml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.File;
import java.util.Locale;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.geotools.util.GrowableInternationalString;
import org.junit.Test;
import org.w3c.dom.Document;

public class GetCapabilitiesTest extends WCSTestSupport {

    @Test
    public void testBasicPost() throws Exception {
        final File xml = new File("./src/test/resources/getcapabilities/getCap.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        Document dom = postAsDOM("wcs", request);
        //        print(dom);

        checkFullCapabilitiesDocument(dom);
    }

    @Test
    public void testCase() throws Exception {
        final File xml = new File("./src/test/resources/getcapabilities/getCapWrongCase.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        Document dom = postAsDOM("wcs", request);
        // print(dom);

        //        checkValidationErrors(dom, WCS20_SCHEMA);

        // todo: check all the layers are here, the profiles, and so on

        // check that we have the crs extension
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport//ows:Exception)", dom);
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:ExceptionReport//ows:Exception[@exceptionCode='InvalidParameterValue'])",
                dom);
        assertXpathEvaluatesTo(
                "1", "count(//ows:ExceptionReport//ows:Exception[@locator='WcS'])", dom);
    }

    @Test
    public void testInternationalContent() throws Exception {
        GeoServer gs = getGeoServer();
        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName("BlueMarble");
        GrowableInternationalString title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for ci bluemarble");
        title.add(Locale.ITALIAN, "titolo italiano");
        GrowableInternationalString _abstract = new GrowableInternationalString();
        _abstract.add(Locale.ENGLISH, "a i18n abstract for ci bluemarble");
        _abstract.add(Locale.ITALIAN, "abstract italiano");
        ci.setInternationalTitle(title);
        ci.setInternationalAbstract(_abstract);
        KeywordInfo keywordInfo = new Keyword("english keyword");
        keywordInfo.setLanguage(Locale.ENGLISH.getLanguage());

        KeywordInfo keywordInfo2 = new Keyword("parola chiave");
        keywordInfo2.setLanguage(Locale.ITALIAN.getLanguage());
        ci.getKeywords().add(keywordInfo);
        ci.getKeywords().add(keywordInfo2);
        catalog.save(ci);
        WCSInfo wcs = gs.getService(WCSInfo.class);
        title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for WCS service");
        title.add(Locale.ITALIAN, "titolo italiano servizio WCS");
        _abstract = new GrowableInternationalString();
        _abstract.add(Locale.ENGLISH, "a i18n abstract for WCS service");
        _abstract.add(Locale.ITALIAN, "abstract italiano servizio WCS");
        wcs.setInternationalTitle(title);
        wcs.setInternationalAbstract(_abstract);
        gs.save(wcs);
        Document doc =
                getAsDOM(
                        "ows?service=WCS&request=getCapabilities&version=2.0.1&acceptLanguages=it");
        String service = "//ows:ServiceIdentification";
        assertXpathEvaluatesTo("titolo italiano servizio WCS", service + "/ows:Title", doc);
        assertXpathEvaluatesTo("abstract italiano servizio WCS", service + "/ows:Abstract", doc);
        String fifteenLayer =
                "/wcs:Capabilities/wcs:Contents/wcs:CoverageSummary[wcs:CoverageId = 'wcs__BlueMarble']";
        assertXpathEvaluatesTo("titolo italiano", fifteenLayer + "/ows:Title", doc);
        assertXpathEvaluatesTo("abstract italiano", fifteenLayer + "/ows:Abstract", doc);
        assertXpathEvaluatesTo("parola chiave", fifteenLayer + "/ows:Keywords/ows:Keyword", doc);
        assertXpathEvaluatesTo(
                "DID NOT FIND i18n CONTENT FOR THIS ELEMENT",
                "/wcs:Capabilities/wcs:Contents/wcs:CoverageSummary[wcs:CoverageId = 'wcs__DEM']/ows:Title",
                doc);
    }
}
