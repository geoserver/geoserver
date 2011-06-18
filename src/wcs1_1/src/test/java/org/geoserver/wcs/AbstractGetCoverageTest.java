package org.geoserver.wcs;

import static org.geoserver.data.test.MockData.*;
import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.*;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletResponse;

import junit.framework.Test;
import net.opengis.wcs11.GetCoverageType;

import org.geoserver.config.GeoServer;
import org.geoserver.wcs.kvp.GetCoverageRequestReader;
import org.geoserver.wcs.test.WCSTestSupport;
import org.geoserver.wcs.xml.v1_1_1.WCSConfiguration;
import org.geoserver.wcs.xml.v1_1_1.WcsXmlReader;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.CRS;
import org.opengis.coverage.grid.GridCoverage;
import org.vfny.geoserver.wcs.WcsException;
import org.w3c.dom.Document;

public abstract class AbstractGetCoverageTest extends WCSTestSupport {

    static final double EPS = 10 - 6;

    GetCoverageRequestReader kvpreader;

    WebCoverageService111 service;

    WCSConfiguration configuration;

    WcsXmlReader xmlReader;

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        kvpreader = (GetCoverageRequestReader) applicationContext
                .getBean("wcs111GetCoverageRequestReader");
        service = (WebCoverageService111) applicationContext.getBean("wcs111ServiceTarget");
        configuration = new WCSConfiguration();
        xmlReader = new WcsXmlReader("GetCoverage", "1.1.1", configuration);
    }

    /**
     * Runs GetCoverage on the specified parameters and returns an array of coverages
     */
    protected GridCoverage[] executeGetCoverageKvp(Map<String, Object> raw) throws Exception {
        GetCoverageType getCoverage = (GetCoverageType) kvpreader.read(kvpreader.createRequest(),
                parseKvp(raw), raw);
        return service.getCoverage(getCoverage);
    }

    /**
     * Prepares the basic KVP map (service, version, request)
     * @return
     */
    protected Map<String, Object> baseMap() {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "WCS");
        raw.put("version", "1.1.1");
        raw.put("request", "GetCoverage");
        return raw;
    }


    /**
     * Runs GetCoverage on the specified parameters and returns an array of coverages
     */
    protected GridCoverage[] executeGetCoverageXml(String request) throws Exception {
        GetCoverageType getCoverage = (GetCoverageType) xmlReader.read(null, new StringReader(
                request), null);
        return service.getCoverage(getCoverage);
    }

    protected void setInputLimit(int kbytes) {
        GeoServer gs = getGeoServer();
        WCSInfo info = gs.getService(WCSInfo.class);
        info.setMaxInputMemory(kbytes);
        gs.save(info);
    } 
    

    protected void setOutputLimit(int kbytes) {
        GeoServer gs = getGeoServer();
        WCSInfo info = gs.getService(WCSInfo.class);
        info.setMaxOutputMemory(kbytes);
        gs.save(info);
    } 


}
