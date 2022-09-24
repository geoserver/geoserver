/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2007 OpenPlans
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;

import java.util.Locale;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.LayerGroupStyleImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.util.GrowableInternationalString;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Unit test suite for {@link GetCapabilitiesResponse}
 *
 * @author Simone Giannecchini - GeoSolutions
 * @version $Id$
 */
public class GetCapabilitiesReponseTest extends WMSTestSupport {

    /** Tests ContentDisposition */
    @Test
    public void testSimple() throws Exception {
        String request = "wms?version=1.1.1&request=GetCapabilities&service=WMS";
        MockHttpServletResponse result = getAsServletResponse(request);
        Assert.assertTrue(result.containsHeader("content-disposition"));
        Assert.assertEquals(
                "inline; filename=getcapabilities_1.1.1.xml",
                result.getHeader("content-disposition"));

        request = "wms?version=1.3.0&request=GetCapabilities&service=WMS";
        result = getAsServletResponse(request);
        Assert.assertTrue(result.containsHeader("content-disposition"));
        Assert.assertEquals(
                "inline; filename=getcapabilities_1.3.0.xml",
                result.getHeader("content-disposition"));
    }

    @Test
    public void testInternationalContent() throws Exception {
        Catalog catalog = getCatalog();
        GeoServer geoServer = getGeoServer();

        LayerGroupInfo groupInfo = catalog.getLayerGroupByName(NATURE_GROUP);
        GrowableInternationalString title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for group nature");
        title.add(Locale.ITALIAN, "titolo italiano");
        GrowableInternationalString _abstract = new GrowableInternationalString();
        _abstract.add(Locale.ENGLISH, "a i18n abstract for group nature");
        _abstract.add(Locale.ITALIAN, "abstract italiano");

        groupInfo.setInternationalTitle(title);
        groupInfo.setInternationalAbstract(_abstract);
        KeywordInfo keywordInfo = new Keyword("english keyword");
        keywordInfo.setLanguage(Locale.ENGLISH.getLanguage());

        KeywordInfo keywordInfo2 = new Keyword("parola chiave");
        keywordInfo2.setLanguage(Locale.ITALIAN.getLanguage());
        groupInfo.getKeywords().add(keywordInfo);
        groupInfo.getKeywords().add(keywordInfo2);
        groupInfo.setMode(LayerGroupInfo.Mode.NAMED);
        catalog.save(groupInfo);

        LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
        title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for layer fifteen");
        _abstract = new GrowableInternationalString();
        _abstract.add(Locale.ENGLISH, "a i18n abstract for layer fifteen");
        fifteen.setInternationalTitle(title);
        fifteen.setInternationalAbstract(_abstract);
        catalog.save(fifteen);

        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for layer lakes");
        lakes.setTitle(null);
        lakes.setInternationalTitle(title);
        lakes.setInternationalAbstract(_abstract);
        catalog.save(lakes);

        WMSInfo info = geoServer.getService(WMSInfo.class);
        title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for WMS service");
        _abstract = new GrowableInternationalString();
        _abstract.add(Locale.ENGLISH, "a i18n abstract for WMS service");
        info.setInternationalTitle(title);
        info.setInternationalAbstract(_abstract);
        geoServer.save(info);

        String request =
                "wms?version=1.1.1&request=GetCapabilities&service=WMS&"
                        + "AcceptLanguages="
                        + Locale.ENGLISH.getLanguage();
        Document result = getAsDOM(request);

        String service = "/WMT_MS_Capabilities/Service";
        assertXpathEvaluatesTo("a i18n title for WMS service", service + "/Title", result);
        assertXpathEvaluatesTo("a i18n abstract for WMS service", service + "/Abstract", result);

        String fifteenLayer = "/WMT_MS_Capabilities/Capability/Layer/Layer[Name = 'cdf:Fifteen']";
        assertXpathEvaluatesTo("a i18n title for layer fifteen", fifteenLayer + "/Title", result);
        assertXpathEvaluatesTo(
                "a i18n abstract for layer fifteen", fifteenLayer + "/Abstract", result);

        String natureGroup = "/WMT_MS_Capabilities/Capability/Layer/Layer/Layer[Name = 'nature']";
        assertXpathEvaluatesTo("a i18n title for group nature", natureGroup + "/Title", result);
        assertXpathEvaluatesTo(
                "a i18n abstract for group nature", natureGroup + "/Abstract", result);

        assertXpathEvaluatesTo("english keyword", natureGroup + "/KeywordList/Keyword", result);

        // check that lakes is not duplicated (GEOS-10205)
        assertXpathEvaluatesTo("1", "count(//Layer[Name='cite:Lakes'])", result);
    }

    @Test
    public void testInternationalContentAnyLanguage() throws Exception {
        // tests that if a * value is provided international content in
        // the available language is provided
        Catalog catalog = getCatalog();
        GeoServer geoServer = getGeoServer();

        LayerGroupInfo groupInfo = catalog.getLayerGroupByName("nature");
        GrowableInternationalString title = new GrowableInternationalString();
        title.add(Locale.ITALIAN, "titolo italiano");
        GrowableInternationalString _abstract = new GrowableInternationalString();
        _abstract.add(Locale.ITALIAN, "abstract italiano");

        groupInfo.setInternationalTitle(title);
        groupInfo.setInternationalAbstract(_abstract);

        KeywordInfo keywordInfo = new Keyword("parola chiave");
        keywordInfo.setLanguage(Locale.ITALIAN.getLanguage());
        groupInfo.getKeywords().add(keywordInfo);
        catalog.save(groupInfo);

        LayerInfo li = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
        title = new GrowableInternationalString();
        title.add(Locale.ITALIAN, "titolo per layer fifteen");
        _abstract = new GrowableInternationalString();
        _abstract.add(Locale.ITALIAN, "abstract per layer fifteen");
        li.setInternationalTitle(title);
        li.setInternationalAbstract(_abstract);

        catalog.save(li);

        WMSInfo info = geoServer.getService(WMSInfo.class);
        title = new GrowableInternationalString();
        title.add(Locale.ITALIAN, "Servizio WMS");
        _abstract = new GrowableInternationalString();
        _abstract.add(Locale.ITALIAN, "Abstract del servizio WMS");
        info.setInternationalTitle(title);
        info.setInternationalAbstract(_abstract);
        geoServer.save(info);

        // we put both fr and *. Content for fr is not available but since a * is present the
        // available one in it
        // will be returned in capabilities
        String request =
                "wms?version=1.1.1&request=GetCapabilities&service=WMS&"
                        + "AcceptLanguages="
                        + Locale.FRENCH.getLanguage()
                        + " *";
        Document result = getAsDOM(request);

        String service = "/WMT_MS_Capabilities/Service";
        assertXpathEvaluatesTo("Servizio WMS", service + "/Title", result);
        assertXpathEvaluatesTo("Abstract del servizio WMS", service + "/Abstract", result);

        String fifteenLayer = "/WMT_MS_Capabilities/Capability/Layer/Layer[Name = 'cdf:Fifteen']";
        assertXpathEvaluatesTo("titolo per layer fifteen", fifteenLayer + "/Title", result);
        assertXpathEvaluatesTo("abstract per layer fifteen", fifteenLayer + "/Abstract", result);

        String natureGroup = "/WMT_MS_Capabilities/Capability/Layer/Layer/Layer[Name = 'nature']";
        assertXpathEvaluatesTo("titolo italiano", natureGroup + "/Title", result);
        assertXpathEvaluatesTo("abstract italiano", natureGroup + "/Abstract", result);

        assertXpathEvaluatesTo("parola chiave", natureGroup + "/KeywordList/Keyword", result);
    }

    @Test
    public void testAcceptLanguagesParameter() throws Exception {
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(MockData.FIFTEEN));
        GrowableInternationalString title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for fti fifteen");
        title.add(Locale.ITALIAN, "titolo italiano");
        fti.setInternationalTitle(title);
        catalog.save(fti);

        // clear wms online resource defaults
        GeoServer geoServer = getGeoServer();
        WMSInfo wmsInfo = getWMS().getServiceInfo();
        wmsInfo.setOnlineResource("");
        geoServer.save(wmsInfo);

        // clear global online resources defaults
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setOnlineResource("");
        global.getSettings().getContact().setOnlineResource("");
        geoServer.save(global);

        Document dom =
                getAsDOM(
                        "wms?version=1.1.1&request=GetCapabilities&service=WMS&AcceptLanguages=it");

        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/?Language=it",
                "WMT_MS_Capabilities/Service/OnlineResource/@xlink:href",
                dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/wms?SERVICE=WMS&Language=it&",
                "WMT_MS_Capabilities/Capability/Request/GetCapabilities/DCPType/HTTP/Get/OnlineResource/@xlink:href",
                dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/wms?request=GetLegendGraphic&version=1.1.1&format=image%2Fpng&width=20&height=20&layer=nature&Language=it",
                "//Layer[Name='nature']/Style/LegendURL/OnlineResource/@xlink:href", dom);
    }

    @Test
    public void testNullLocale() throws Exception {
        Catalog catalog = getCatalog();
        FeatureTypeInfo old = catalog.getFeatureTypeByName(getLayerId(MockData.FIFTEEN));
        try {
            FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(MockData.FIFTEEN));
            GrowableInternationalString title = new GrowableInternationalString();
            title.add(Locale.ENGLISH, "a i18n title for fti fifteen");
            title.add(null, "null locale title");
            fti.setInternationalTitle(title);
            GrowableInternationalString abstrct = new GrowableInternationalString();
            abstrct.add(Locale.ENGLISH, "english abstract");
            abstrct.add(null, "null locale abstract");
            fti.setInternationalAbstract(abstrct);
            fti.setTitle(null);
            fti.setAbstract(null);
            catalog.save(fti);

            Document dom = getAsDOM("wms?version=1.1.1&request=GetCapabilities&service=WMS");

            String fifteenLayer =
                    "/WMT_MS_Capabilities/Capability/Layer/Layer[Name = 'cdf:Fifteen']";
            assertXpathEvaluatesTo("null locale title", fifteenLayer + "/Title", dom);
            assertXpathEvaluatesTo("null locale abstract", fifteenLayer + "/Abstract", dom);
        } finally {
            catalog.save(old);
        }
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
                            "wms?version=1.1.1&request=GetCapabilities&service=WMS&AcceptLanguages=en");
            String contactInf = "//ContactInformation";
            String primary = contactInf + "/ContactPersonPrimary";

            assertXpathEvaluatesTo(
                    "I'm an english organization", primary + "/ContactOrganization", doc);
            assertXpathEvaluatesTo("I'm an english person", primary + "/ContactPerson", doc);
            assertXpathEvaluatesTo("Cartographer", contactInf + "/ContactPosition", doc);
            assertXpathEvaluatesTo("02304566607", contactInf + "/ContactVoiceTelephone", doc);
            assertXpathEvaluatesTo("0023030948", contactInf + "/ContactFacsimileTelephone", doc);
            assertXpathEvaluatesTo(
                    "english@person.com", contactInf + "/ContactElectronicMailAddress", doc);

            String addrInfo = contactInf + "/ContactAddress";

            assertXpathEvaluatesTo("work", addrInfo + "/AddressType", doc);
            assertXpathEvaluatesTo("address", addrInfo + "/Address", doc);
            assertXpathEvaluatesTo("London", addrInfo + "/City", doc);
            assertXpathEvaluatesTo("England", addrInfo + "/Country", doc);
            assertXpathEvaluatesTo("34234", addrInfo + "/PostCode", doc);
        } finally {
            GeoServerInfo global = getGeoServer().getGlobal();
            global.getSettings().setContact(old);
            getGeoServer().save(global);
        }
    }

    @Test
    public void testLayerGroupStyle() throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo groupInfo = null;
        try {
            String layerGroupName = "test_group_style";
            createLakesPlacesLayerGroup(catalog, layerGroupName, LayerGroupInfo.Mode.SINGLE, null);
            groupInfo = catalog.getLayerGroupByName(layerGroupName);
            LayerGroupStyle groupStyle = new LayerGroupStyleImpl();
            StyleInfo groupStyleName = new StyleInfoImpl(catalog);
            groupStyleName.setName("group-style-name");
            groupStyle.setName(groupStyleName);
            groupStyle.getLayers().add(catalog.getLayerByName("cite:Forests"));
            groupStyle.getStyles().add(null);
            groupInfo.getLayerGroupStyles().add(groupStyle);
            catalog.save(groupInfo);
            String request = "wms?version=1.1.1&request=GetCapabilities&service=WMS";
            Document result = getAsDOM(request);

            String groupStyleEl =
                    "//Layer[Name = '" + layerGroupName + "']/Style[Name = 'group-style-name']";
            assertXpathExists(groupStyleEl, result);
        } finally {
            if (groupInfo != null) catalog.remove(groupInfo);
        }
    }

    @Test
    public void testLayerGroupStyleOpaque() throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo groupInfo = null;
        try {
            String layerGroupName = "test_group_style";
            createLakesPlacesLayerGroup(
                    catalog, layerGroupName, LayerGroupInfo.Mode.OPAQUE_CONTAINER, null);
            groupInfo = catalog.getLayerGroupByName(layerGroupName);
            LayerGroupStyle groupStyle = new LayerGroupStyleImpl();
            StyleInfo groupStyleName = new StyleInfoImpl(catalog);
            groupStyleName.setName("group-style-name");
            groupStyle.setName(groupStyleName);
            groupStyle.getLayers().add(catalog.getLayerByName("cite:Forests"));
            groupStyle.getStyles().add(null);
            groupInfo.getLayerGroupStyles().add(groupStyle);
            catalog.save(groupInfo);
            String request = "wms?version=1.1.1&request=GetCapabilities&service=WMS";
            Document result = getAsDOM(request);

            String groupStyleEl =
                    "//Layer[Name = '" + layerGroupName + "']/Style[Name = 'group-style-name']";
            assertXpathExists(groupStyleEl, result);
        } finally {
            if (groupInfo != null) catalog.remove(groupInfo);
        }
    }

    @Test
    public void testLayerGroupStyleSkippedWhenTree() throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo groupInfo = null;
        try {
            String layerGroupName = "test_group_style";
            createLakesPlacesLayerGroup(catalog, layerGroupName, LayerGroupInfo.Mode.NAMED, null);
            groupInfo = catalog.getLayerGroupByName(layerGroupName);
            LayerGroupStyle groupStyle = new LayerGroupStyleImpl();
            StyleInfo groupStyleName = new StyleInfoImpl(catalog);
            groupStyleName.setName("group-style-name");
            groupStyle.setName(groupStyleName);
            groupStyle.getLayers().add(catalog.getLayerByName("cite:Forests"));
            groupStyle.getStyles().add(null);
            groupInfo.getLayerGroupStyles().add(groupStyle);
            catalog.save(groupInfo);
            String request = "wms?version=1.1.1&request=GetCapabilities&service=WMS";
            Document result = getAsDOM(request);

            String groupStyleEl =
                    "//Layer[Name = '" + layerGroupName + "']/Style[Name = 'group-style-name']";
            assertXpathNotExists(groupStyleEl, result);
        } finally {
            if (groupInfo != null) catalog.remove(groupInfo);
        }
    }
}
