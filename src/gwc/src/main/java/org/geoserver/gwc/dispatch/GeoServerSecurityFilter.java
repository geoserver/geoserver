/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.dispatch;

import java.util.Objects;
import org.geoserver.gwc.GWC;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.ServiceException;
import org.geotools.referencing.CRS;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.filter.security.SecurityFilter;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.SRS;
import org.geowebcache.layer.TileLayer;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;

/**
 * Filter which applies GeoServer security to GWC requests
 *
 * @author Kevin Smith, Boundless
 */
public class GeoServerSecurityFilter implements SecurityFilter {

    @Override
    public void checkSecurity(TileLayer layer, BoundingBox extent, SRS srs)
            throws SecurityException, GeoWebCacheException {
        if (GWC.get().getConfig().isSecurityEnabled()) {
            try {
                ReferencedEnvelope env;
                if (Objects.nonNull(extent)) {
                    env =
                            new ReferencedEnvelope(
                                    extent.getMinX(),
                                    extent.getMaxX(),
                                    extent.getMinY(),
                                    extent.getMaxY(),
                                    CRS.decode(srs.toString()));
                } else {
                    env = null;
                }
                GWC.get().verifyAccessLayer(layer.getName(), env);
            } catch (ServiceException | MismatchedDimensionException | FactoryException e) {
                throw new GeoWebCacheException(e);
            }
        }
    }
}
