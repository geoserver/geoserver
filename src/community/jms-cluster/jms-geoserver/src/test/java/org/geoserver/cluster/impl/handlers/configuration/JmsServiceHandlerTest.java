/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.configuration;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cluster.impl.events.configuration.JMSEventType;
import org.geoserver.cluster.impl.events.configuration.JMSServiceModifyEvent;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JmsServiceHandlerTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:TestContext.xml");
    }

    @Before
    public void setup() {
        // create a test workspace
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId("jms-test-workspace");
        workspace.setName("jms-test-workspace");
        getCatalog().add(workspace);
    }

    @After
    public void clean() {
        // remove test workspace
        getCatalog().remove(getCatalog().getWorkspace("jms-test-workspace"));
        // remove any created service
        Collection<? extends ServiceInfo> services = getGeoServer().getServices();
        for (ServiceInfo service : services) {
            ServiceInfo finalService = ModificationProxy.unwrap(service);
            if (finalService instanceof JmsTestServiceInfoImpl) {
                getGeoServer().remove(finalService);
            }
        }
    }

    @Test
    public void testGlobalServiceSimpleCrud() throws Exception {
        // service events handler
        JMSServiceHandler handler = createHandler();
        // create a new global service
        handler.synchronize(
                createNewServiceEvent(
                        "jms-test-service-1", "jms-test-service", "global-jms-test-service", null));
        checkServiceExists("jms-test-service", "global-jms-test-service", null);
        // update global service
        handler.synchronize(
                createModifyServiceEvent(
                        "jms-test-service", "global-jms-test-service-updated", null));
        checkServiceExists("jms-test-service", "global-jms-test-service-updated", null);
        // delete global service
        handler.synchronize(createRemoveServiceEvent("jms-test-service", null));
        assertThat(findService("jms-test-service", null), nullValue());
    }

    @Test
    public void testVirtualServiceSimpleCrud() throws Exception {
        // service events handler
        JMSServiceHandler handler = createHandler();
        // create a new virtual service
        handler.synchronize(
                createNewServiceEvent(
                        "jms-test-service-2",
                        "jms-test-service",
                        "virtual-jms-test-service",
                        "jms-test-workspace"));
        checkServiceExists("jms-test-service", "virtual-jms-test-service", "jms-test-workspace");
        // update virtual service
        handler.synchronize(
                createModifyServiceEvent(
                        "jms-test-service",
                        "virtual-jms-test-service-updated",
                        "jms-test-workspace"));
        checkServiceExists(
                "jms-test-service", "virtual-jms-test-service-updated", "jms-test-workspace");
        // delete virtual service
        handler.synchronize(createRemoveServiceEvent("jms-test-service", "jms-test-workspace"));
        assertThat(findService("jms-test-service", "jms-test-workspace"), nullValue());
    }

    @Test
    public void testGlobalAndVirtualServiceSimpleCrud() throws Exception {
        // service events handler
        JMSServiceHandler handler = createHandler();
        // create a new global and virtual service
        handler.synchronize(
                createNewServiceEvent(
                        "jms-test-service-1", "jms-test-service", "global-jms-test-service", null));
        checkServiceExists("jms-test-service", "global-jms-test-service", null);
        handler.synchronize(
                createNewServiceEvent(
                        "jms-test-service-2",
                        "jms-test-service",
                        "virtual-jms-test-service",
                        "jms-test-workspace"));
        checkServiceExists("jms-test-service", "virtual-jms-test-service", "jms-test-workspace");
        // update global service
        handler.synchronize(
                createModifyServiceEvent(
                        "jms-test-service", "global-jms-test-service-updated", null));
        checkServiceExists("jms-test-service", "global-jms-test-service-updated", null);
        checkServiceExists("jms-test-service", "virtual-jms-test-service", "jms-test-workspace");
        // update virtual service
        handler.synchronize(
                createModifyServiceEvent(
                        "jms-test-service",
                        "virtual-jms-test-service-updated",
                        "jms-test-workspace"));
        checkServiceExists(
                "jms-test-service", "virtual-jms-test-service-updated", "jms-test-workspace");
        checkServiceExists("jms-test-service", "global-jms-test-service-updated", null);
        // delete virtual service
        handler.synchronize(createRemoveServiceEvent("jms-test-service", "jms-test-workspace"));
        assertThat(findService("jms-test-service", "jms-test-workspace"), nullValue());
        assertThat(findService("jms-test-service", null), notNullValue());
        // delete global service
        handler.synchronize(createRemoveServiceEvent("jms-test-service", null));
        assertThat(findService("jms-test-service", null), nullValue());
    }

    @Test
    public void testUpdatingNonExistingVirtualService() throws Exception {
        // service events handler
        JMSServiceHandler handler = createHandler();
        // create a new global and virtual service
        handler.synchronize(
                createNewServiceEvent(
                        "jms-test-service-3", "jms-test-service", "global-jms-test-service", null));
        checkServiceExists("jms-test-service", "global-jms-test-service", null);
        handler.synchronize(
                createNewServiceEvent(
                        "jms-test-service-4",
                        "jms-test-service",
                        "virtual-jms-test-service",
                        "jms-test-workspace"));
        checkServiceExists("jms-test-service", "virtual-jms-test-service", "jms-test-workspace");
        // create update virtual service event
        handler.synchronize(
                createModifyServiceEvent(
                        "jms-test-service",
                        "virtual-jms-test-service-updated",
                        "jms-test-workspace"));
        // remove virtual service
        handler.synchronize(createRemoveServiceEvent("jms-test-service", "jms-test-workspace"));
        assertThat(findService("jms-test-service", "jms-test-workspace"), nullValue());
        // check the update result
        checkServiceExists("jms-test-service", "global-jms-test-service", null);
    }

    private void checkServiceExists(
            String serviceName, String serviceAbstract, String workspaceName) {
        ServiceInfo serviceInfo = findService(serviceName, workspaceName);
        assertThat(serviceInfo, notNullValue());
        assertThat(serviceInfo.getAbstract(), is(serviceAbstract));
    }

    private JMSServiceModifyEvent createNewServiceEvent(
            String serviceId, String serviceName, String serviceAbstract, String workspaceName) {
        // our virtual service information
        JmsTestServiceInfoImpl serviceInfo = new JmsTestServiceInfoImpl();
        serviceInfo.setName(serviceName);
        serviceInfo.setId(serviceId);
        if (workspaceName != null) {
            // this is a virtual service
            serviceInfo.setWorkspace(getCatalog().getWorkspace(workspaceName));
        }
        serviceInfo.setAbstract(serviceAbstract);
        // create jms service modify event
        return new JMSServiceModifyEvent(serviceInfo, JMSEventType.ADDED);
    }

    private JMSServiceModifyEvent createModifyServiceEvent(
            String serviceName, String newServiceAbstract, String workspaceName) {
        // service information
        ServiceInfo serviceInfo = findService(serviceName, workspaceName);
        String oldServiceAbstract = serviceInfo.getAbstract();
        serviceInfo.setAbstract(newServiceAbstract);
        // create jms service modify event
        return new JMSServiceModifyEvent(
                serviceInfo,
                Collections.singletonList("abstract"),
                Collections.singletonList(oldServiceAbstract),
                Collections.singletonList(newServiceAbstract),
                JMSEventType.MODIFIED);
    }

    private JMSServiceModifyEvent createRemoveServiceEvent(
            String serviceName, String workspaceName) {
        // our virtual service information
        ServiceInfo serviceInfo = findService(serviceName, workspaceName);
        // create jms service modify event
        return new JMSServiceModifyEvent(serviceInfo, JMSEventType.REMOVED);
    }

    private ServiceInfo findService(String serviceName, String workspaceName) {
        if (workspaceName == null) {
            // global service
            return ModificationProxy.unwrap(
                    getGeoServer().getServiceByName(serviceName, ServiceInfo.class));
        }
        // virtual service
        WorkspaceInfo workspaceInfo = getCatalog().getWorkspace(workspaceName);
        return ModificationProxy.unwrap(
                getGeoServer().getServiceByName(workspaceInfo, serviceName, ServiceInfo.class));
    }

    private JMSServiceHandler createHandler() {
        JMSServiceHandlerSPI handlerSpi = GeoServerExtensions.bean(JMSServiceHandlerSPI.class);
        return (JMSServiceHandler) handlerSpi.createHandler();
    }
}
