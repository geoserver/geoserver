/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp.v2_0;

import java.util.Map;

import net.opengis.wfs20.Wfs20Factory;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.opengis.filter.FilterFactory;


public class GetFeatureKvpRequestReader extends org.geoserver.wfs.kvp.GetFeatureKvpRequestReader {


    public GetFeatureKvpRequestReader(Class requestBean, GeoServer geoServer, FilterFactory filterFactory) {
        super(requestBean, Wfs20Factory.eINSTANCE, geoServer, filterFactory);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        // special cite compliance check to ensure the client specified typeNames rather than just
        // typeName
        if (!kvp.containsKey("typenames") && kvp.containsKey("typename")
            && getWFS().isCiteCompliant()) {
            throw new WFSException("WFS 2.0 requires typeNames, not typeName");
        }
        return super.read(request, kvp, rawKvp);
    }
}
