/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wcs20;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.util.logging.Logging;
import org.geotools.xml.EMFUtils;
import org.opengis.coverage.Coverage;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class GetCoverageHandler extends RequestObjectHandler  {

    static Logger LOGGER = Logging.getLogger("org.geoserver.monitor");



    public GetCoverageHandler(MonitorConfig config, Catalog catalog) {
        super("net.opengis.wcs20.GetCoverageType", config);
    }
    
    @Override
    public List<String> getLayers(Object request) {
        String id = (String)EMFUtils.get((EObject)request, "coverageId");
        return id != null ? Arrays.asList(id) : null;
    }
    
    @Override
    protected BoundingBox getBBox(Object request) {
        return null;
    }


    public Object operationExecuted(Request request, Operation operation, Object result, RequestData data) {
        if (result instanceof GranuleStack) {
            List<GridCoverage2D> granules = ((GranuleStack) result).getGranules();
            Iterator<GridCoverage2D> iterator = granules.iterator();
            Rectangle2D union = iterator.next().getEnvelope2D();
            CoordinateReferenceSystem crs = ((Envelope2D) union).getCoordinateReferenceSystem();
            while (iterator.hasNext()) {
                union = iterator.next().getEnvelope2D().createUnion(union);
            }
            data.setBbox(new Envelope2D(crs,union));

        } else if (result instanceof Coverage) {
            Envelope envelope = ((Coverage) result).getEnvelope();
            if (envelope instanceof BoundingBox) {
                data.setBbox((BoundingBox) envelope);
            }
        }
        return null;
    }

}
