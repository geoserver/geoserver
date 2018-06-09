/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.IOException;
import java.util.List;
import net.opengis.wcs20.GetCapabilitiesType;
import org.geoserver.ExtendedCapabilitiesProvider;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.wcs.WCSInfo;
import org.geotools.data.ows.GetCapabilitiesRequest;

/**
 * WCS extensions have a place for content extensions, and operation extensions, so we add two new
 * methods and disable the default one
 */
public abstract class WCSExtendedCapabilitiesProvider
        implements ExtendedCapabilitiesProvider<WCSInfo, GetCapabilitiesRequest> {

    @Override
    public void encode(
            org.geoserver.ExtendedCapabilitiesProvider.Translator tx,
            WCSInfo wcs,
            GetCapabilitiesRequest request)
            throws IOException {
        // nothing to do here
    }

    public abstract void encodeExtendedOperations(
            org.geoserver.ExtendedCapabilitiesProvider.Translator tx,
            WCSInfo wcs,
            GetCapabilitiesType request)
            throws IOException;

    public abstract void encodeExtendedContents(
            org.geoserver.ExtendedCapabilitiesProvider.Translator tx,
            WCSInfo wcs,
            List<CoverageInfo> coverages,
            GetCapabilitiesType request)
            throws IOException;
}
