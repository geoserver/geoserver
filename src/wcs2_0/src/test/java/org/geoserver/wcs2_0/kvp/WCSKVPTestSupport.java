/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wcs2_0.kvp;

import java.util.HashMap;
import java.util.Map;
import net.opengis.wcs20.ExtensionItemType;
import net.opengis.wcs20.GetCoverageType;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.kvp.GetCoverageRequestReader;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.geoserver.wcs2_0.WebCoverageService20;
import org.geotools.wcs.v1_1.WCSConfiguration;
import org.junit.Before;
import org.opengis.coverage.grid.GridCoverage;

/** @author Simone Giannecchini, GeoSolutions SAS */
public abstract class WCSKVPTestSupport extends WCSTestSupport {

    static final double EPS = 10 - 6;
    WCSConfiguration configuration;
    GetCoverageRequestReader kvpreader;
    WebCoverageService20 service;

    public WCSKVPTestSupport() {
        super();
    }

    @SuppressWarnings("unchecked")
    protected GetCoverageType parse(String url) throws Exception {
        Map<String, Object> rawKvp = new CaseInsensitiveMap(KvpUtils.parseQueryString(url));
        Map<String, Object> kvp = new CaseInsensitiveMap(parseKvp(rawKvp));
        WCS20GetCoverageRequestReader reader = new WCS20GetCoverageRequestReader();
        GetCoverageType gc = (GetCoverageType) reader.createRequest();
        return (GetCoverageType) reader.read(gc, kvp, rawKvp);
    }

    protected Map<String, Object> getExtensionsMap(GetCoverageType gc) {
        // collect extensions
        Map<String, Object> extensions = new HashMap<String, Object>();
        for (ExtensionItemType item : gc.getExtension().getContents()) {
            Object value =
                    item.getSimpleContent() != null
                            ? item.getSimpleContent()
                            : item.getObjectContent();
            extensions.put(item.getNamespace() + ":" + item.getName(), value);
        }
        return extensions;
    }

    /** Runs GetCoverage on the specified parameters and returns an array of coverages */
    protected GridCoverage executeGetCoverage(String url) throws Exception {
        GridCoverage coverage = service.getCoverage(parse(url));
        super.scheduleForCleaning(coverage);
        return coverage;
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

    @Before
    public void setup() {
        kvpreader =
                (GetCoverageRequestReader)
                        applicationContext.getBean("wcs111GetCoverageRequestReader");
        service = (WebCoverageService20) applicationContext.getBean("wcs20ServiceTarget");
        configuration = new WCSConfiguration();
    }
}
