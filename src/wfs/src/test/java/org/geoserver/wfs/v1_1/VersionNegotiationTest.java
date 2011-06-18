package org.geoserver.wfs.v1_1;

import junit.framework.Test;
import net.opengis.ows10.Ows10Factory;
import net.opengis.wfs.GetCapabilitiesType;
import net.opengis.wfs.WfsFactory;

import org.geoserver.wfs.CapabilitiesTransformer;
import org.geoserver.wfs.GetCapabilities;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.xml.transform.TransformerBase;

public class VersionNegotiationTest extends WFSTestSupport {

    static GetCapabilities getCaps;

    static WfsFactory factory;

    static Ows10Factory owsFactory;
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new VersionNegotiationTest());
    }

    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();

        getCaps = new GetCapabilities(getWFS(), getCatalog());

        factory = WfsFactory.eINSTANCE;
        owsFactory = Ows10Factory.eINSTANCE;
    }

    public void test0() throws Exception {
        // test when provided and accepted match up
        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("1.0.0");
        request.getAcceptVersions().getVersion().add("1.1.0");

        TransformerBase tx = getCaps.run(request);
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_1);
    }

    public void test1() throws Exception {
        // test accepted only 1.0
        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("1.0.0");

        TransformerBase tx = getCaps.run(request);
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_0);
    }

    public void test2() throws Exception {
        // test accepted only 1.1
        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("1.1.0");

        TransformerBase tx = getCaps.run(request);
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_1);
    }

    public void test5() throws Exception {
        // test accepted = 0.0.0

        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("0.0.0");

        TransformerBase tx = getCaps.run(request);
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_0);
    }

    public void test6() throws Exception {
        // test accepted = 1.1.1

        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("1.1.1");

        TransformerBase tx = getCaps.run(request);
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_1);
    }

    public void test7() throws Exception {
        // test accepted = 1.0.5
        GetCapabilitiesType request = factory.createGetCapabilitiesType();
        request.setService("WFS");
        request.setAcceptVersions(owsFactory.createAcceptVersionsType());
        request.getAcceptVersions().getVersion().add("1.0.5");

        TransformerBase tx = getCaps.run(request);
        assertTrue(tx instanceof CapabilitiesTransformer.WFS1_0);
    }

}
