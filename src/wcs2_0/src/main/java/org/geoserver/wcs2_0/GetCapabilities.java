/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wcs2_0;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import net.opengis.wcs20.GetCapabilitiesType;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.response.WCS20GetCapabilitiesTransformer;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;

/** @author Emanuele Tajariol (etj) - GeoSolutions */
public class GetCapabilities {

    protected Logger LOGGER = Logging.getLogger(DefaultWebCoverageService20.class);

    private WCSInfo wcs;
    private CoverageResponseDelegateFinder responseFactory;

    public static final List<String> PROVIDED_VERSIONS = Collections.unmodifiableList(
            Arrays.asList(WCS20Const.V201, WCS20Const.V20, WCS20Const.V111, WCS20Const.V110));

    public GetCapabilities(WCSInfo wcs, CoverageResponseDelegateFinder responseFactory) {
        this.wcs = wcs;
        this.responseFactory = responseFactory;
    }

    TransformerBase run(GetCapabilitiesType request) throws WCS20Exception {
        WCS20GetCapabilitiesTransformer capsTransformer =
                new WCS20GetCapabilitiesTransformer(wcs.getGeoServer(), responseFactory);
        capsTransformer.setEncoding(
                Charset.forName((wcs.getGeoServer().getSettings().getCharset())));
        return capsTransformer;
    }
}
