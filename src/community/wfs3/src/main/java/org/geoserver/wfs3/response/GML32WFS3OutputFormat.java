/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import java.util.HashSet;
import java.util.Set;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.xml.GML32OutputFormat;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geoserver.wfs3.WebFeatureService30;
import org.geotools.util.Version;

/**
 * A format exposing the GML32 100km long mime type for WFS3.0... of course, it will be used only
 * for WFS 3.0 :-)
 */
public class GML32WFS3OutputFormat extends GML32OutputFormat {

    public static final String FORMAT =
            "application/gml+xml;version=3.2;profile=http://www.opengis.net/def/profile/ogc/2.0/gml-sf0";
    static final Set<String> FORMATS =
            new HashSet<String>() {
                {
                    add(FORMAT);
                }
            };

    protected GML32WFS3OutputFormat(GeoServer geoServer, WFSConfiguration configuration) {
        super(geoServer, FORMATS, configuration);
    }

    @Override
    protected boolean canHandleInternal(Operation operation) {
        return WebFeatureService30.V3.compareTo(operation.getService().getVersion()) <= 0;
    }

    @Override
    public boolean canHandle(Version version) {
        return WebFeatureService30.V3.compareTo(version) <= 0;
    }
}
