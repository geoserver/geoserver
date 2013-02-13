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
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wcs2_0.WCSTestSupport;

/**
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public abstract class WCSKVPTestSupport extends WCSTestSupport {

    /**
     * 
     */
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
            Object value = item.getSimpleContent() != null ? item.getSimpleContent() : item.getObjectContent();
            extensions.put(item.getNamespace() + ":" + item.getName(), value);
        }
        return extensions;
    }

}