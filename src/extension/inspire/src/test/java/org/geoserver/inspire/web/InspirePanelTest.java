/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.inspire.web;

import junit.framework.AssertionFailedError;
import static org.junit.Assert.*;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.inspire.InspireMetadata;
import org.geoserver.inspire.UniqueResourceIdentifiers;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wms.WMSInfo;
import org.geotools.util.Converters;
import org.junit.Test;

public class InspirePanelTest extends GeoServerWicketTestSupport {

    private void setupWMSPopulatedPage() {

        final WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wms.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        wms.getMetadata().put(InspireMetadata.SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        getGeoServer().save(wms);

        tester.startPage(new FormTestPage(new ComponentBuilder() {

            @Override
            public Component buildComponent(String id) {
                return new InspireAdminPanel(id, new Model(wms));
            }
        }));

    }

    private void setupWFSPopulatedPage() {

        final WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wfs.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        wfs.getMetadata().put(InspireMetadata.SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        wfs.getMetadata().put(InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                "one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two");
        getGeoServer().save(wfs);

        tester.startPage(new FormTestPage(new ComponentBuilder() {

            @Override
            public Component buildComponent(String id) {
                return new InspireAdminPanel(id, new Model(wfs));
            }
        }));

    }

    @Test
    public void testWMSContents() {
        setupWMSPopulatedPage();

        tester.assertComponent("form", Form.class);

        // check language
        tester.assertComponent("form:panel:language", LanguageDropDownChoice.class);
        tester.assertModelValue("form:panel:language", "fre");

        // check metadata url
        tester.assertComponent("form:panel:metadataURL", TextField.class);
        tester.assertModelValue("form:panel:metadataURL", "http://foo.com?bar=baz");

        // check metadata url type
        tester.assertComponent("form:panel:metadataURLType", DropDownChoice.class);
        tester.assertModelValue("form:panel:metadataURLType", "application/vnd.iso.19139+xml");

        try {
            // the spatial identifiers editor
            tester.assertComponent("form:panel:datasetIdentifiersContainer:spatialDatasetIdentifiers", UniqueResourceIdentifiersEditor.class);
            fail("Shouldn't have found a Spatial Dataset Identifers section");
        } catch (AssertionFailedError e) {
        }
    }

    @Test
    public void testWFSContents() {
        setupWFSPopulatedPage();

        tester.assertComponent("form", Form.class);

        // check language
        tester.assertComponent("form:panel:language", LanguageDropDownChoice.class);
        tester.assertModelValue("form:panel:language", "fre");

        // check metadata url
        tester.assertComponent("form:panel:metadataURL", TextField.class);
        tester.assertModelValue("form:panel:metadataURL", "http://foo.com?bar=baz");

        // check metadata url type
        tester.assertComponent("form:panel:metadataURLType", DropDownChoice.class);
        tester.assertModelValue("form:panel:metadataURLType", "application/vnd.iso.19139+xml");

        // the spatial identifiers editor
        tester.assertComponent("form:panel:datasetIdentifiersContainer:spatialDatasetIdentifiers", UniqueResourceIdentifiersEditor.class);
        UniqueResourceIdentifiers expected = Converters.convert("one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two", UniqueResourceIdentifiers.class);
        tester.assertModelValue("form:panel:datasetIdentifiersContainer:spatialDatasetIdentifiers", expected);
    }
    
    @Test
    public void testEditBasic() {
        setupWFSPopulatedPage();

        FormTester ft = tester.newFormTester("form");
        ft.select("panel:language", 0);
        ft.setValue("panel:metadataURL", "http://www.geoserver.org/test");
        ft.select("panel:metadataURLType", 0);
        ft.submit();

        tester.assertModelValue("form:panel:language", "bul");
        tester.assertModelValue("form:panel:metadataURL", "http://www.geoserver.org/test");
        tester.assertModelValue("form:panel:metadataURLType", "application/vnd.ogc.csw.GetRecordByIdResponse_xml");
    }

}
