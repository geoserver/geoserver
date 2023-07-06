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
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.data.InternationalContentHelper;
import org.geoserver.data.test.MockData;
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
        // print(dom);

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

        CoverageInfo blueMarble = catalog.getCoverageByName("BlueMarble");
        GrowableInternationalString title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for ci bluemarble");
        title.add(Locale.ITALIAN, "titolo italiano");
        GrowableInternationalString _abstract = new GrowableInternationalString();
        _abstract.add(Locale.ENGLISH, "a i18n abstract for ci bluemarble");
        _abstract.add(Locale.ITALIAN, "abstract italiano");
        blueMarble.setInternationalTitle(title);
        blueMarble.setInternationalAbstract(_abstract);
        KeywordInfo keywordInfo = new Keyword("english keyword");
        keywordInfo.setLanguage(Locale.ENGLISH.getLanguage());
        KeywordInfo keywordInfo2 = new Keyword("parola chiave");
        keywordInfo2.setLanguage(Locale.ITALIAN.getLanguage());
        blueMarble.getKeywords().add(keywordInfo);
        blueMarble.getKeywords().add(keywordInfo2);
        catalog.save(blueMarble);

        CoverageInfo dem = catalog.getCoverageByName(getLayerId(MockData.TASMANIA_DEM));
        GrowableInternationalString demTitle = new GrowableInternationalString();
        demTitle.add(null, "the default dem title");
        demTitle.add(Locale.ENGLISH, "an english title");
        dem.setInternationalTitle(demTitle);
        catalog.save(dem);

        CoverageInfo cad = catalog.getCoverageByName(getLayerId(MockData.ROTATED_CAD));
        GrowableInternationalString cadTitle = new GrowableInternationalString();
        // no default language
        cadTitle.add(Locale.ENGLISH, "an english title");
        cad.setInternationalTitle(cadTitle);
        catalog.save(cad);

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
        print(doc);
        String service = "//ows:ServiceIdentification";
        assertXpathEvaluatesTo("titolo italiano servizio WCS", service + "/ows:Title", doc);
        assertXpathEvaluatesTo("abstract italiano servizio WCS", service + "/ows:Abstract", doc);
        String bmLayer =
                "/wcs:Capabilities/wcs:Contents/wcs:CoverageSummary[wcs:CoverageId = 'wcs__BlueMarble']";
        assertXpathEvaluatesTo("titolo italiano", bmLayer + "/ows:Title", doc);
        assertXpathEvaluatesTo("abstract italiano", bmLayer + "/ows:Abstract", doc);
        assertXpathEvaluatesTo("parola chiave", bmLayer + "/ows:Keywords/ows:Keyword", doc);
        // title checks for default value used
        assertXpathEvaluatesTo(
                "the default dem title",
                "/wcs:Capabilities/wcs:Contents/wcs:CoverageSummary[wcs:CoverageId = 'wcs__DEM']/ows:Title",
                doc);
        // title checks for no configured i18n title
        assertXpathEvaluatesTo(
                InternationalContentHelper.ERROR_MESSAGE,
                "/wcs:Capabilities/wcs:Contents/wcs:CoverageSummary[wcs:CoverageId = 'wcs__World']/ows:Title",
                doc);
        // title checks for configured i18n title without italian language and without default
        assertXpathEvaluatesTo(
                InternationalContentHelper.ERROR_MESSAGE,
                "/wcs:Capabilities/wcs:Contents/wcs:CoverageSummary[wcs:CoverageId = 'wcs__RotatedCad']/ows:Title",
                doc);
    }

    @Test
    public void testDefaultLocale() throws Exception {
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
        ci.setTitle(null);
        ci.setAbstract(null);
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
        wcs.setTitle(null);
        wcs.setAbstract(null);
        wcs.setDefaultLocale(Locale.ENGLISH);
        gs.save(wcs);
        Document doc = getAsDOM("ows?service=WCS&request=getCapabilities&version=2.0.1");
        String service = "//ows:ServiceIdentification";
        assertXpathEvaluatesTo("a i18n title for WCS service", service + "/ows:Title", doc);
        assertXpathEvaluatesTo("a i18n abstract for WCS service", service + "/ows:Abstract", doc);
        String fifteenLayer =
                "/wcs:Capabilities/wcs:Contents/wcs:CoverageSummary[wcs:CoverageId = 'wcs__BlueMarble']";
        assertXpathEvaluatesTo("a i18n title for ci bluemarble", fifteenLayer + "/ows:Title", doc);
        assertXpathEvaluatesTo(
                "a i18n abstract for ci bluemarble", fifteenLayer + "/ows:Abstract", doc);
    }

    @Test
    public void testWithoutDefaultLocale() throws Exception {
        GeoServer gs = getGeoServer();
        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName("BlueMarble");
        GrowableInternationalString title = new GrowableInternationalString();
        title.add(Locale.getDefault(), "a i18n title for ci bluemarble");
        GrowableInternationalString _abstract = new GrowableInternationalString();
        _abstract.add(Locale.getDefault(), "a i18n abstract for ci bluemarble");
        ci.setInternationalTitle(title);
        ci.setInternationalAbstract(_abstract);
        ci.setTitle(null);
        ci.setAbstract(null);
        catalog.save(ci);
        WCSInfo wcs = gs.getService(WCSInfo.class);
        title = new GrowableInternationalString();
        title.add(Locale.getDefault(), "a i18n title for WCS service");
        _abstract = new GrowableInternationalString();
        _abstract.add(Locale.getDefault(), "a i18n abstract for WCS service");
        wcs.setInternationalTitle(title);
        wcs.setInternationalAbstract(_abstract);
        wcs.setTitle(null);
        wcs.setAbstract(null);
        gs.save(wcs);
        Document doc = getAsDOM("ows?service=WCS&request=getCapabilities&version=2.0.1");
        String service = "//ows:ServiceIdentification";
        assertXpathEvaluatesTo("a i18n title for WCS service", service + "/ows:Title", doc);
        assertXpathEvaluatesTo("a i18n abstract for WCS service", service + "/ows:Abstract", doc);
        String fifteenLayer =
                "/wcs:Capabilities/wcs:Contents/wcs:CoverageSummary[wcs:CoverageId = 'wcs__BlueMarble']";
        assertXpathEvaluatesTo("a i18n title for ci bluemarble", fifteenLayer + "/ows:Title", doc);
        assertXpathEvaluatesTo(
                "a i18n abstract for ci bluemarble", fifteenLayer + "/ows:Abstract", doc);
    }

    @Test
    public void testInternationalContentContact() throws Exception {
        ContactInfo old = getGeoServer().getSettings().getContact();
        try {
            GrowableInternationalString person = new GrowableInternationalString();
            person.add(Locale.ITALIAN, "I'm an italian person");
            person.add(Locale.ENGLISH, "I'm an english person");
            ContactInfo contactInfo = new ContactInfoImpl();
            contactInfo.setInternationalContactPerson(person);

            GrowableInternationalString org = new GrowableInternationalString();
            org.add(Locale.ITALIAN, "I'm an italian organization");
            org.add(Locale.ENGLISH, "I'm an english organization");
            contactInfo.setInternationalContactOrganization(org);

            GrowableInternationalString email = new GrowableInternationalString();
            email.add(Locale.ITALIAN, "italian@person.it");
            email.add(Locale.ENGLISH, "english@person.com");
            contactInfo.setInternationalContactEmail(email);

            GrowableInternationalString position = new GrowableInternationalString();
            position.add(Locale.ITALIAN, "Cartografo");
            position.add(Locale.ENGLISH, "Cartographer");
            contactInfo.setInternationalContactPosition(position);

            GrowableInternationalString tel = new GrowableInternationalString();
            tel.add(Locale.ITALIAN, "0558077333");
            tel.add(Locale.ENGLISH, "02304566607");
            contactInfo.setInternationalContactVoice(tel);

            GrowableInternationalString fax = new GrowableInternationalString();
            fax.add(Locale.ITALIAN, "0557777333");
            fax.add(Locale.ENGLISH, "0023030948");
            contactInfo.setInternationalContactFacsimile(fax);

            GrowableInternationalString address = new GrowableInternationalString();
            address.add(Locale.ITALIAN, "indirizzo");
            address.add(Locale.ENGLISH, "address");
            contactInfo.setInternationalAddress(address);

            GrowableInternationalString addressType = new GrowableInternationalString();
            addressType.add(Locale.ITALIAN, "lavoro");
            addressType.add(Locale.ENGLISH, "work");
            contactInfo.setInternationalAddressType(addressType);

            GrowableInternationalString country = new GrowableInternationalString();
            country.add(Locale.ITALIAN, "Italia");
            country.add(Locale.ENGLISH, "England");
            contactInfo.setInternationalAddressCountry(country);

            GrowableInternationalString city = new GrowableInternationalString();
            city.add(Locale.ITALIAN, "Roma");
            city.add(Locale.ENGLISH, "London");
            contactInfo.setInternationalAddressCity(city);

            GrowableInternationalString postalCode = new GrowableInternationalString();
            postalCode.add(Locale.ITALIAN, "50021");
            postalCode.add(Locale.ENGLISH, "34234");
            contactInfo.setInternationalAddressPostalCode(postalCode);

            GeoServerInfo global = getGeoServer().getGlobal();
            global.getSettings().setContact(contactInfo);
            getGeoServer().save(global);

            Document doc =
                    getAsDOM(
                            "ows?service=WCS&request=getCapabilities&version=2.0.1&acceptLanguages=it");
            String service = "//ows:ServiceProvider";
            assertXpathEvaluatesTo(
                    "I'm an italian organization", service + "/ows:ProviderName", doc);

            String contact = service + "/ows:ServiceContact";
            assertXpathEvaluatesTo("I'm an italian person", contact + "/ows:IndividualName", doc);

            assertXpathEvaluatesTo("Cartografo", contact + "/ows:PositionName", doc);

            String otherInfo = contact + "/ows:ContactInfo";
            assertXpathEvaluatesTo("0558077333", otherInfo + "/ows:Phone/ows:Voice", doc);
            assertXpathEvaluatesTo("0557777333", otherInfo + "/ows:Phone/ows:Facsimile", doc);
            String addressPath = otherInfo + "/ows:Address";
            assertXpathEvaluatesTo("indirizzo", addressPath + "/ows:DeliveryPoint", doc);
            assertXpathEvaluatesTo("Roma", addressPath + "/ows:City", doc);
            assertXpathEvaluatesTo("50021", addressPath + "/ows:PostalCode", doc);
            assertXpathEvaluatesTo("Italia", addressPath + "/ows:Country", doc);
            assertXpathEvaluatesTo(
                    "italian@person.it", addressPath + "/ows:ElectronicMailAddress", doc);
        } finally {
            GeoServerInfo global = getGeoServer().getGlobal();
            global.getSettings().setContact(old);
            getGeoServer().save(global);
        }
    }
}
