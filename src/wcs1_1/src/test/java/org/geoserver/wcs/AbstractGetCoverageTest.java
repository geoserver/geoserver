/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.opengis.wcs11.GetCoverageType;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.wcs.kvp.GetCoverageRequestReader;
import org.geoserver.wcs.test.WCSTestSupport;
import org.geoserver.wcs.xml.v1_1_1.WcsXmlReader;
import org.geotools.wcs.v1_1.WCSConfiguration;
import org.junit.After;
import org.junit.Before;
import org.opengis.coverage.grid.GridCoverage;

public abstract class AbstractGetCoverageTest extends WCSTestSupport {

    static final double EPS = 10 - 6;

    GetCoverageRequestReader kvpreader;

    WebCoverageService111 service;

    WCSConfiguration configuration;

    WcsXmlReader xmlReader;

    List<GridCoverage> coverages = new ArrayList<GridCoverage>();

    @Before
    public void setup() {
        kvpreader =
                (GetCoverageRequestReader)
                        applicationContext.getBean("wcs111GetCoverageRequestReader");
        service = (WebCoverageService111) applicationContext.getBean("wcs111ServiceTarget");
        configuration = new WCSConfiguration();
        xmlReader =
                new WcsXmlReader(
                        "GetCoverage",
                        "1.1.1",
                        configuration,
                        EntityResolverProvider.RESOLVE_DISABLED_PROVIDER);
    }

    @After
    public void cleanCoverages() {
        for (GridCoverage coverage : coverages) {
            CoverageCleanerCallback.disposeCoverage(coverage);
        }
    }

    /** Runs GetCoverage on the specified parameters and returns an array of coverages */
    protected GridCoverage[] executeGetCoverageKvp(Map<String, Object> raw) throws Exception {
        GetCoverageType getCoverage =
                (GetCoverageType) kvpreader.read(kvpreader.createRequest(), parseKvp(raw), raw);
        GridCoverage[] result = service.getCoverage(getCoverage);
        coverages.addAll(Arrays.asList(result));
        return result;
    }

    /** Prepares the basic KVP map (service, version, request) */
    protected Map<String, Object> baseMap() {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "WCS");
        raw.put("version", "1.1.1");
        raw.put("request", "GetCoverage");
        return raw;
    }

    /** Runs GetCoverage on the specified parameters and returns an array of coverages */
    protected GridCoverage[] executeGetCoverageXml(String request) throws Exception {
        GetCoverageType getCoverage =
                (GetCoverageType) xmlReader.read(null, new StringReader(request), null);
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

    protected Map parseKvp(Map /* <String,String> */ raw) throws Exception {

        // parse like the dispatcher but make sure we don't change the original map
        HashMap input = new HashMap(raw);
        List<Throwable> errors = KvpUtils.parse(input);
        if (errors != null && errors.size() > 0) throw (Exception) errors.get(0);

        return caseInsensitiveKvp(input);
    }

    protected Map caseInsensitiveKvp(HashMap input) {
        // make it case insensitive like the servlet+dispatcher maps
        Map result = new HashMap();
        for (Iterator it = input.keySet().iterator(); it.hasNext(); ) {
            String key = (String) it.next();
            result.put(key.toUpperCase(), input.get(key));
        }
        return new CaseInsensitiveMap(result);
    }
}
