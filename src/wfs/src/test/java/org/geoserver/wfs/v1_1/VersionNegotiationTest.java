/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v1_1;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import net.opengis.ows10.Ows10Factory;
import net.opengis.wfs.GetCapabilitiesType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.wfs.CapabilitiesTransformer;
import org.geoserver.wfs.GetCapabilities;
import org.geoserver.wfs.WFSExtendedCapabilitiesProvider;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.request.GetCapabilitiesRequest;
import org.geotools.xml.transform.TransformerBase;
import org.junit.Before;
import org.junit.Test;

public class VersionNegotiationTest extends WFSTestSupport {

    static GetCapabilities getCaps;

    static WfsFactory factory;

    static Ows10Factory owsFactory;

    @Before
    public void initialise() {
        getCaps =
                new GetCapabilities(
                        getWFS(),
                        getCatalog(),
                        Collections.<WFSExtendedCapabilitiesProvider>emptyList());

        factory = WfsFactory.eINSTANCE;
        owsFactory = Ows10Factory.eINSTANCE;
    }

    @Test
    public void test0() throws Exception {
        // test when provided and accepted match up
        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("1.0.0");
        request.getAcceptVersions().getVersion().add("1.1.0");

        TransformerBase tx = getCaps.run(GetCapabilitiesRequest.adapt(request));
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_1);
    }

    @Test
    public void test1() throws Exception {
        // test accepted only 1.0
        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("1.0.0");

        TransformerBase tx = getCaps.run(GetCapabilitiesRequest.adapt(request));
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_0);
    }

    @Test
    public void test2() throws Exception {
        // test accepted only 1.1
        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("1.1.0");

        TransformerBase tx = getCaps.run(GetCapabilitiesRequest.adapt(request));
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_1);
    }

    @Test
    public void test5() throws Exception {
        // test accepted = 0.0.0

        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("0.0.0");

        TransformerBase tx = getCaps.run(GetCapabilitiesRequest.adapt(request));
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_0);
    }

    @Test
    public void test6() throws Exception {
        // test accepted = 1.1.1

        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("1.1.1");

        TransformerBase tx = getCaps.run(GetCapabilitiesRequest.adapt(request));
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_1);
    }

    @Test
    public void test7() throws Exception {
        // test accepted = 1.0.5
        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("1.0.5");

        TransformerBase tx = getCaps.run(GetCapabilitiesRequest.adapt(request));
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_0);
    }
}
