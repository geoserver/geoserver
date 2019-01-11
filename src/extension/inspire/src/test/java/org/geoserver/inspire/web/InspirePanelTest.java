/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.inspire.web;

import static org.geoserver.inspire.InspireMetadata.CREATE_EXTENDED_CAPABILITIES;
import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_URL;
import static org.geoserver.inspire.InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE;
import static org.geoserver.inspire.InspireTestSupport.clearInspireMetadata;
import static org.geoserver.web.GeoServerWicketTestSupport.tester;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.ValidationErrorFeedback;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ServiceInfo;
import org.geoserver.inspire.UniqueResourceIdentifiers;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wms.WMSInfo;
import org.geotools.util.Converters;
import org.junit.Test;

public class InspirePanelTest extends GeoServerWicketTestSupport {

    private void startPage(final ServiceInfo serviceInfo) {

        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {

                            @Override
                            public Component buildComponent(String id) {
                                return new InspireAdminPanel(id, new Model(serviceInfo));
                            }
                        }));
    }

    @Test
    @SuppressWarnings("TryFailThrowable")
    public void testNoInspireSettingsWMS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        tester.assertComponent("form", Form.class);

        // check ExtendedCapabilities off
        tester.assertComponent("form:panel:createExtendedCapabilities", CheckBox.class);
        tester.assertModelValue("form:panel:createExtendedCapabilities", false);

        try {
            tester.assertComponent(
                    "form:panel:container:configs:language", LanguageDropDownChoice.class);
            fail("Shouldn't have found section for INSPIRE extension configuration");
        } catch (AssertionError e) {
        }
    }

    @Test
    public void testCreateExtCapsOffWMS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, false);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        tester.assertComponent("form", Form.class);

        // check ExtendedCapabilities off
        tester.assertComponent("form:panel:createExtendedCapabilities", CheckBox.class);
        tester.assertModelValue("form:panel:createExtendedCapabilities", false);

        tester.assertInvisible("form:panel:container:configs");
    }

    @Test
    public void testCreateExtCapsOffWFS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, false);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/one");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        tester.assertComponent("form", Form.class);

        // check ExtendedCapabilities off
        tester.assertComponent("form:panel:createExtendedCapabilities", CheckBox.class);
        tester.assertModelValue("form:panel:createExtendedCapabilities", false);

        tester.assertInvisible("form:panel:container:configs");
    }

    @Test
    public void testCreateExtCapsOffWCS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WCSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, false);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/one");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        tester.assertComponent("form", Form.class);

        // check ExtendedCapabilities off
        tester.assertComponent("form:panel:createExtendedCapabilities", CheckBox.class);
        tester.assertModelValue("form:panel:createExtendedCapabilities", false);

        tester.assertInvisible("form:panel:container:configs");
    }

    @Test
    @SuppressWarnings("TryFailThrowable")
    public void testWithFullSettingsWMS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        tester.assertComponent("form", Form.class);

        // check ExtendedCapabilities on
        tester.assertComponent("form:panel:createExtendedCapabilities", CheckBox.class);
        tester.assertModelValue("form:panel:createExtendedCapabilities", true);

        // check language
        tester.assertComponent(
                "form:panel:container:configs:language", LanguageDropDownChoice.class);
        tester.assertModelValue("form:panel:container:configs:language", "fre");

        print(tester.getLastRenderedPage(), true, true);
        // check metadata url
        tester.assertComponent(
                "form:panel:container:configs:border:border_body:metadataURL", TextField.class);
        tester.assertModelValue(
                "form:panel:container:configs:border:border_body:metadataURL",
                "http://foo.com?bar=baz");

        // check metadata url type
        tester.assertComponent(
                "form:panel:container:configs:metadataURLType", DropDownChoice.class);
        tester.assertModelValue(
                "form:panel:container:configs:metadataURLType", "application/vnd.iso.19139+xml");

        try {
            // the spatial identifiers editor
            tester.assertComponent(
                    "form:panel:container:configs:datasetIdentifiersContainer:spatialDatasetIdentifiers",
                    UniqueResourceIdentifiersEditor.class);
            fail("Shouldn't have found a Spatial Dataset Identifers section");
        } catch (AssertionError e) {
        }
    }

    @Test
    public void testWithFullSettingsWFS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(
                SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                "one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        tester.assertComponent("form", Form.class);

        // check ExtendedCapabilities on
        tester.assertComponent("form:panel:createExtendedCapabilities", CheckBox.class);
        tester.assertModelValue("form:panel:createExtendedCapabilities", true);

        // check language
        tester.assertComponent(
                "form:panel:container:configs:language", LanguageDropDownChoice.class);
        tester.assertModelValue("form:panel:container:configs:language", "fre");

        // check metadata url
        tester.assertComponent(
                "form:panel:container:configs:border:border_body:metadataURL", TextField.class);
        tester.assertModelValue(
                "form:panel:container:configs:border:border_body:metadataURL",
                "http://foo.com?bar=baz");

        // check metadata url type
        tester.assertComponent(
                "form:panel:container:configs:metadataURLType", DropDownChoice.class);
        tester.assertModelValue(
                "form:panel:container:configs:metadataURLType", "application/vnd.iso.19139+xml");

        // the spatial identifiers editor
        tester.assertComponent(
                "form:panel:container:configs:datasetIdentifiersContainer:spatialDatasetIdentifiers",
                UniqueResourceIdentifiersEditor.class);
        UniqueResourceIdentifiers expected =
                Converters.convert(
                        "one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two",
                        UniqueResourceIdentifiers.class);
        tester.assertModelValue(
                "form:panel:container:configs:datasetIdentifiersContainer:spatialDatasetIdentifiers",
                expected);
    }

    @Test
    public void testWithFullSettingsWCS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WCSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(
                SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                "one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        tester.assertComponent("form", Form.class);

        // check ExtendedCapabilities on
        tester.assertComponent("form:panel:createExtendedCapabilities", CheckBox.class);
        tester.assertModelValue("form:panel:createExtendedCapabilities", true);

        // check language
        tester.assertComponent(
                "form:panel:container:configs:language", LanguageDropDownChoice.class);
        tester.assertModelValue("form:panel:container:configs:language", "fre");

        // check metadata url
        tester.assertComponent(
                "form:panel:container:configs:border:border_body:metadataURL", TextField.class);
        tester.assertModelValue(
                "form:panel:container:configs:border:border_body:metadataURL",
                "http://foo.com?bar=baz");

        // check metadata url type
        tester.assertComponent(
                "form:panel:container:configs:metadataURLType", DropDownChoice.class);
        tester.assertModelValue(
                "form:panel:container:configs:metadataURLType", "application/vnd.iso.19139+xml");

        // the spatial identifiers editor
        tester.assertComponent(
                "form:panel:container:configs:datasetIdentifiersContainer:spatialDatasetIdentifiers",
                UniqueResourceIdentifiersEditor.class);
        UniqueResourceIdentifiers expected =
                Converters.convert(
                        "one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two",
                        UniqueResourceIdentifiers.class);
        tester.assertModelValue(
                "form:panel:container:configs:datasetIdentifiersContainer:spatialDatasetIdentifiers",
                expected);
    }

    @Test
    public void testNoLanguageWMS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        // check language defaults to "eng"
        tester.assertComponent(
                "form:panel:container:configs:language", LanguageDropDownChoice.class);
        tester.assertModelValue("form:panel:container:configs:language", "eng");
    }

    @Test
    public void testNoMediaTypeWMS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        // check no metadata url type selected
        tester.assertComponent(
                "form:panel:container:configs:metadataURLType", DropDownChoice.class);
        tester.assertModelValue("form:panel:container:configs:metadataURLType", null);
    }

    @Test
    public void testCreateExtCapMissingWithRequiredSettingsWMS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        tester.assertComponent("form", Form.class);

        // check ExtendedCapabilities on
        tester.assertComponent("form:panel:createExtendedCapabilities", CheckBox.class);
        tester.assertModelValue("form:panel:createExtendedCapabilities", true);

        // Just check there is some configuration won't repeat all checks as for when check box
        // explcitly set.
        tester.assertComponent(
                "form:panel:container:configs:language", LanguageDropDownChoice.class);
        tester.assertModelValue("form:panel:container:configs:language", "fre");
    }

    @Test
    public void testCreateExtCapMissingWithoutRequiredSettingsWMS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        tester.assertComponent("form", Form.class);

        // check ExtendedCapabilities off
        tester.assertComponent("form:panel:createExtendedCapabilities", CheckBox.class);
        tester.assertModelValue("form:panel:createExtendedCapabilities", false);

        tester.assertInvisible("form:panel:container:configs");
    }

    @Test
    public void testCreateExtCapMissingWithRequiredSettingsWFS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/one");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        tester.assertComponent("form", Form.class);

        // check ExtendedCapabilities on
        tester.assertComponent("form:panel:createExtendedCapabilities", CheckBox.class);
        tester.assertModelValue("form:panel:createExtendedCapabilities", true);

        // Just check there is some configuration won't repeat all checks as for when check box
        // explcitly set.
        tester.assertComponent(
                "form:panel:container:configs:language", LanguageDropDownChoice.class);
        tester.assertModelValue("form:panel:container:configs:language", "fre");
    }

    @Test
    public void testCreateExtCapMissingWithoutRequiredSettingsWFS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        tester.assertComponent("form", Form.class);

        // check ExtendedCapabilities off
        tester.assertComponent("form:panel:createExtendedCapabilities", CheckBox.class);
        tester.assertModelValue("form:panel:createExtendedCapabilities", false);

        tester.assertInvisible("form:panel:container:configs");
    }

    @Test
    public void testCreateExtCapMissingWithRequiredSettingsWCS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WCSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/one");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        tester.assertComponent("form", Form.class);

        // check ExtendedCapabilities on
        tester.assertComponent("form:panel:createExtendedCapabilities", CheckBox.class);
        tester.assertModelValue("form:panel:createExtendedCapabilities", true);

        // Just check there is some configuration won't repeat all checks as for when check box
        // explcitly set.
        tester.assertComponent(
                "form:panel:container:configs:language", LanguageDropDownChoice.class);
        tester.assertModelValue("form:panel:container:configs:language", "fre");
    }

    @Test
    public void testCreateExtCapMissingWithoutRequiredSettingsWCS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WCSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        tester.assertComponent("form", Form.class);

        // check ExtendedCapabilities off
        tester.assertComponent("form:panel:createExtendedCapabilities", CheckBox.class);
        tester.assertModelValue("form:panel:createExtendedCapabilities", false);

        tester.assertInvisible("form:panel:container:configs");
    }

    @Test
    public void testEditBasicWFS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(
                SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                "one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        FormTester ft = tester.newFormTester("form");
        ft.select("panel:container:configs:language", 0);
        ft.setValue(
                "panel:container:configs:border:border_body:metadataURL",
                "http://www.geoserver.org/test");
        ft.select("panel:container:configs:metadataURLType", 0);
        ft.submit();

        tester.assertModelValue("form:panel:container:configs:language", "bul");
        tester.assertModelValue(
                "form:panel:container:configs:border:border_body:metadataURL",
                "http://www.geoserver.org/test");
        tester.assertModelValue(
                "form:panel:container:configs:metadataURLType",
                "application/vnd.ogc.csw.GetRecordByIdResponse_xml");
    }

    @Test
    public void testEditBasicWCS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WCSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(
                SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                "one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        FormTester ft = tester.newFormTester("form");
        ft.select("panel:container:configs:language", 0);
        ft.setValue(
                "panel:container:configs:border:border_body:metadataURL",
                "http://www.geoserver.org/test");
        ft.select("panel:container:configs:metadataURLType", 0);
        ft.submit();

        tester.assertModelValue("form:panel:container:configs:language", "bul");
        tester.assertModelValue(
                "form:panel:container:configs:border:border_body:metadataURL",
                "http://www.geoserver.org/test");
        tester.assertModelValue(
                "form:panel:container:configs:metadataURLType",
                "application/vnd.ogc.csw.GetRecordByIdResponse_xml");
    }

    @Test
    public void testSubmitWithoutRequiredWMS() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);
        startPage(serviceInfo);

        FormTester ft = tester.newFormTester("form");
        ft.setValue("panel:createExtendedCapabilities", true);
        tester.executeAjaxEvent("form:panel:createExtendedCapabilities", "change");

        tester.submitForm("form");

        List<Serializable> messages = tester.getMessages(FeedbackMessage.ERROR);
        assertEquals(1, messages.size());
        String message = (String) ((ValidationErrorFeedback) messages.get(0)).getMessage();
        assertTrue(message.contains("Service Metadata URL"));
    }
}
