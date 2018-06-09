/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import static org.geoserver.cluster.JmsEventsListener.getMessagesForHandler;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.UUID;
import javax.jms.Message;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cluster.impl.events.configuration.JMSEventType;
import org.geoserver.cluster.impl.events.configuration.JMSServiceModifyEvent;
import org.geoserver.cluster.impl.handlers.configuration.JMSServiceHandlerSPI;
import org.geoserver.config.ServiceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.After;
import org.junit.Test;

/** Tests related with services events. */
public final class JmsServicesTest extends GeoServerSystemTestSupport {

    private static final String SERVICE_EVENT_HANDLER_KEY = "JMSServiceHandlerSPI";

    private WorkspaceInfoImpl workspace;
    private static JMSEventHandler<String, JMSServiceModifyEvent> serviceHandler;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        // adding our test spring context
        springContextLocations.add("classpath:TestContext.xml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // create the test workspace if it doesn't exsist
        workspace = new WorkspaceInfoImpl();
        workspace.setId("test-workspace");
        workspace.setName("test-workspace");
        getCatalog().add(workspace);
        // initiate the handlers related with services
        serviceHandler = GeoServerExtensions.bean(JMSServiceHandlerSPI.class).createHandler();
    }

    @After
    public void afterTest() {
        // clear all pending events
        JmsEventsListener.clear();
    }

    @Test
    public void testAddService() throws Exception {
        // create a WMS service for the test workspace
        WMSInfoImpl serviceInfo = new WMSInfoImpl();
        serviceInfo.setName("TEST-WMS-NAME");
        serviceInfo.setId("TEST-WMS-ID");
        serviceInfo.setWorkspace(workspace);
        serviceInfo.setAbstract("TEST-WMS-ABSTRACT");
        // add the new service to GeoServer
        getGeoServer().add(serviceInfo);
        // waiting for a service add event
        List<Message> messages =
                JmsEventsListener.getMessagesByHandlerKey(
                        5000, (selected) -> selected.size() >= 1, SERVICE_EVENT_HANDLER_KEY);
        // let's check if the new added service was correctly published
        assertThat(messages.size(), is(1));
        List<JMSServiceModifyEvent> serviceEvents =
                getMessagesForHandler(messages, SERVICE_EVENT_HANDLER_KEY, serviceHandler);
        assertThat(serviceEvents.size(), is(1));
        assertThat(serviceEvents.get(0).getEventType(), is(JMSEventType.ADDED));
        // check the service content
        ServiceInfo publishedService = serviceEvents.get(0).getSource();
        assertThat(publishedService.getName(), is("TEST-WMS-NAME"));
        assertThat(publishedService.getId(), is("TEST-WMS-ID"));
        assertThat(publishedService.getAbstract(), is("TEST-WMS-ABSTRACT"));
    }

    @Test
    public void testModifyService() throws Exception {
        // modify the abstract of the WMS service
        WMSInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        assertThat(serviceInfo, notNullValue());
        String newAbstract = UUID.randomUUID().toString();
        serviceInfo.setAbstract(newAbstract);
        getGeoServer().save(serviceInfo);
        // waiting for the service modify events
        List<Message> messages =
                JmsEventsListener.getMessagesByHandlerKey(
                        5000, (selected) -> selected.size() >= 2, SERVICE_EVENT_HANDLER_KEY);
        // checking if we got the correct events, modify event and a post modify event
        assertThat(messages.size(), is(2));
        List<JMSServiceModifyEvent> serviceEvents =
                getMessagesForHandler(messages, SERVICE_EVENT_HANDLER_KEY, serviceHandler);
        assertThat(serviceEvents.size(), is(2));
        // check the modify event
        JMSServiceModifyEvent modifyEvent =
                serviceEvents
                        .stream()
                        .filter(event -> event.getEventType() == JMSEventType.MODIFIED)
                        .findFirst()
                        .orElse(null);
        assertThat(modifyEvent, notNullValue());
        ServiceInfo modifiedService = serviceEvents.get(0).getSource();
        assertThat(modifiedService.getName(), is(serviceInfo.getName()));
        assertThat(modifiedService.getId(), is(serviceInfo.getId()));
        assertThat(modifiedService.getAbstract(), is(newAbstract));
        // check the post modify event
        JMSServiceModifyEvent postModifyEvent =
                serviceEvents
                        .stream()
                        .filter(event -> event.getEventType() == JMSEventType.ADDED)
                        .findFirst()
                        .orElse(null);
        assertThat(postModifyEvent, notNullValue());
        ServiceInfo postModifiedService = serviceEvents.get(0).getSource();
        assertThat(postModifiedService.getName(), is(serviceInfo.getName()));
        assertThat(postModifiedService.getId(), is(serviceInfo.getId()));
        assertThat(postModifiedService.getAbstract(), is(newAbstract));
    }
}
