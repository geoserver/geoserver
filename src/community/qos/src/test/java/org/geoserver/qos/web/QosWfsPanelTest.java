/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.qos.wfs.QosWFSRestTest;
import org.geoserver.qos.wfs.WfsQosConfigurationLoader;
import org.geoserver.qos.xml.QosMainConfiguration;
import org.geoserver.rest.RestBaseController;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wms.WMSInfo;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class QosWfsPanelTest extends GeoServerWicketTestSupport {

    protected void startPage(final ServiceInfo serviceInfo) {
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {
                            @Override
                            public Component buildComponent(String id) {
                                return new QosWfsAdminPanel(id, new Model(serviceInfo));
                            }
                        }));
    }

    @Test
    public void testQosDisabled() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        WfsQosConfigurationLoader loader =
                (WfsQosConfigurationLoader)
                        GeoServerExtensions.bean(WfsQosConfigurationLoader.SPRING_BEAN_NAME);
        disableQosConfig();
        QosMainConfiguration conf = loader.getConfiguration(serviceInfo);
        assertEquals(Boolean.FALSE, conf.getActivated());
        startPage(serviceInfo);
        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:panel:createExtendedCapabilities", CheckBox.class);
        try {
            tester.assertComponent("form:panel:container:configs", WebMarkupContainer.class);
            fail("Shouldn't have found section for QoS extension configuration");
        } catch (AssertionError e) {
        }
    }

    @Test
    public void testQosEnabled() {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        WfsQosConfigurationLoader loader =
                (WfsQosConfigurationLoader)
                        GeoServerExtensions.bean(WfsQosConfigurationLoader.SPRING_BEAN_NAME);
        QosMainConfiguration conf = loader.getConfiguration(serviceInfo);
        assertEquals(Boolean.FALSE, conf.getActivated());
        conf.setActivated(true);
        loader.setConfiguration((WFSInfo) serviceInfo, conf);
        startPage(serviceInfo);
        tester.assertComponent("form:panel:container:configs", WebMarkupContainer.class);
    }

    @Test
    public void testOperatingInfoConfig() throws Exception {
        setupQosConfig();
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        WfsQosConfigurationLoader loader =
                (WfsQosConfigurationLoader)
                        GeoServerExtensions.bean(WfsQosConfigurationLoader.SPRING_BEAN_NAME);
        QosMainConfiguration conf = loader.getConfiguration(serviceInfo);
        assertEquals(Boolean.TRUE, conf.getActivated());
        // Operating info data check
        startPage(serviceInfo);
        tester.assertComponent("form", Form.class);
        // check ExtendedCapabilities on opinfo:
        // titleSelect
        tester.assertComponent(
                "form:panel:container:configs:opInfoListView:0:opinfo:opInfoForm:titleSelect",
                DropDownChoice.class);
        tester.assertModelValue(
                "form:panel:container:configs:opInfoListView:0" + ":opinfo:opInfoForm:titleSelect",
                "http://def.opengeospatial.org/codelist/qos/status/1.0/operationalStatus.rdf#PreOperational");
        // titleInput
        tester.assertComponent(
                "form:panel:container:configs:opInfoListView:0:opinfo:opInfoForm:titleInput",
                TextField.class);
        tester.assertModelValue(
                "form:panel:container:configs:opInfoListView:0" + ":opinfo:opInfoForm:titleInput",
                "testbed14");
        // byDaysOfWeek
        tester.assertComponent(
                "form:panel:container:configs:opInfoListView:0:opinfo:opInfoForm"
                        + ":timeListContainer:timeList:0:timePanel:opInfoTimeForm:daysOfWeekCheckGroup",
                CheckGroup.class);
        tester.assertModelValue(
                "form:panel:container:configs:opInfoListView:0:opinfo:opInfoForm"
                        + ":timeListContainer:timeList:0:timePanel:opInfoTimeForm:daysOfWeekCheckGroup",
                conf.getWmsQosMetadata()
                        .getOperatingInfo()
                        .get(0)
                        .getByDaysOfWeek()
                        .get(0)
                        .getDays());
        // endTime
        tester.assertComponent(
                "form:panel:container:configs:opInfoListView:0:opinfo:opInfoForm"
                        + ":timeListContainer:timeList:0:timePanel:opInfoTimeForm:endTimeField",
                TextField.class);
        tester.assertModelValue(
                "form:panel:container:configs:opInfoListView:0:opinfo:opInfoForm"
                        + ":timeListContainer:timeList:0:timePanel:opInfoTimeForm:endTimeField",
                "21:00:00+03:00");
    }

    protected void setupQosConfig() throws Exception {
        String xml = getFileData("test-data/wfs-data.xml");
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + QosWFSRestTest.QOS_WFS_PATH,
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());
    }

    protected void disableQosConfig() throws Exception {
        String xml = getFileData("test-data/disabled-config.xml");
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + QosWFSRestTest.QOS_WFS_PATH,
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());
    }

    protected String getFileData(String filename) {
        String result = null;
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            result =
                    IOUtils.toString(
                            classLoader.getResourceAsStream(filename), Charset.defaultCharset());
        } catch (IOException e) {
        }
        return result;
    }
}
