/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import java.nio.charset.Charset;
import java.util.logging.Logger;

import net.opengis.wcs20.DescribeCoverageType;
import net.opengis.wcs20.GetCapabilitiesType;
import net.opengis.wcs20.GetCoverageType;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.response.WCS20DescribeCoverageTransformer;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.opengis.coverage.grid.GridCoverage;

/**
 * Default implementation of the Web Coverage Service 2.0
 * 
 * @author Emanuele Tajariol (etj) - GeoSolutions
 */
public class DefaultWebCoverageService20 implements WebCoverageService20 {

    protected Logger LOGGER = Logging.getLogger(DefaultWebCoverageService20.class);

    private Catalog catalog;

    private GeoServer geoServer;

    private CoverageResponseDelegateFinder responseFactory;
    
    /** Utility class to map envelope dimension*/
    private EnvelopeAxesLabelsMapper envelopeAxesMapper;

    public DefaultWebCoverageService20(GeoServer geoServer, CoverageResponseDelegateFinder responseFactory, EnvelopeAxesLabelsMapper envelopeDimensionsMapper) {
        this.geoServer = geoServer;
        this.catalog = geoServer.getCatalog();
        this.responseFactory = responseFactory;
        this.envelopeAxesMapper=envelopeDimensionsMapper;
    }
    
    @Override
    public WCSInfo getServiceInfo() {
        return geoServer.getService(WCSInfo.class);
    }


    @Override
    public TransformerBase getCapabilities(GetCapabilitiesType request) {
        checkService(request.getService());

        return new GetCapabilities(getServiceInfo(), catalog, responseFactory).run(request);
    }

    @Override
    public WCS20DescribeCoverageTransformer describeCoverage(DescribeCoverageType request) {
        checkService(request.getService());
        checkVersion(request.getVersion());
        
        if( request.getCoverageId() == null || request.getCoverageId().isEmpty() ) {
            throw new OWS20Exception("Required parameter coverageId missing", WCS20Exception.WCS20ExceptionCode.EmptyCoverageIdList, "coverageId");
        }

        WCSInfo wcs = getServiceInfo();

        WCS20DescribeCoverageTransformer describeTransformer = new WCS20DescribeCoverageTransformer(wcs, catalog, responseFactory,envelopeAxesMapper);
        describeTransformer.setEncoding(Charset.forName(wcs.getGeoServer().getSettings().getCharset()));
        return describeTransformer;
    }

    @Override
    public GridCoverage getCoverage(GetCoverageType request) {
        checkService(request.getService());
        checkVersion(request.getVersion());

        if( request.getCoverageId() == null || "".equals(request.getCoverageId()) ) {
            throw new OWS20Exception("Required parameter coverageId missing", WCS20Exception.WCS20ExceptionCode.EmptyCoverageIdList, "coverageId");
        }
        
        return new GetCoverage(getServiceInfo(), catalog,envelopeAxesMapper).run(request);
    }

    private void checkVersion(String version) {
        if(version == null) {
            throw new WCS20Exception("Missing version", OWS20Exception.OWSExceptionCode.MissingParameterValue, version);
        }

        if ( ! WCS20Const.V20x.equals(version) && ! WCS20Const.V20.equals(version)) {
            throw new WCS20Exception("Could not understand version:" + version);
        }
    }

    private void checkService(String serviceName) {
        if( serviceName == null ) {
            throw new WCS20Exception("Missing service name", OWS20Exception.OWSExceptionCode.MissingParameterValue, "service");
        }
        if( ! "WCS".equals(serviceName)) {
            throw new WCS20Exception("Error in service name", OWS20Exception.OWSExceptionCode.InvalidParameterValue, serviceName);
        }
    }

}
