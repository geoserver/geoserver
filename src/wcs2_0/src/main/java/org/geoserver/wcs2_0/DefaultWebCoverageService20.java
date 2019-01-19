/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import static org.geoserver.wcs2_0.util.RequestUtils.*;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import net.opengis.wcs20.DescribeCoverageType;
import net.opengis.wcs20.DescribeEOCoverageSetType;
import net.opengis.wcs20.GetCapabilitiesType;
import net.opengis.wcs20.GetCoverageType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.response.MIMETypeMapper;
import org.geoserver.wcs2_0.response.WCS20DescribeCoverageTransformer;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geoserver.wcs2_0.util.StringUtils;
import org.geoserver.wcs2_0.util.WCS20DescribeCoverageExtension;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.opengis.coverage.grid.GridCoverage;

/**
 * Default implementation of the Web Coverage Service 2.0
 *
 * @author Emanuele Tajariol (etj) - GeoSolutions
 * @author Simone Giannecchini, GeoSolutions
 */
public class DefaultWebCoverageService20 implements WebCoverageService20 {

    protected Logger LOGGER = Logging.getLogger(DefaultWebCoverageService20.class);

    private MIMETypeMapper mimeMapper;

    private Catalog catalog;

    private GeoServer geoServer;

    private CoverageResponseDelegateFinder responseFactory;

    /** Utility class to map envelope dimension */
    private EnvelopeAxesLabelsMapper envelopeAxesMapper;

    /** Available extension points for the DescribeCoverage operation */
    private List<WCS20DescribeCoverageExtension> wcsDescribeCoverageExtensions;

    /**
     * Boolean indicating that at least an extension point for the DescribeCoverage operation is
     * available
     */
    private boolean availableDescribeCovExtensions;

    public DefaultWebCoverageService20(
            GeoServer geoServer,
            CoverageResponseDelegateFinder responseFactory,
            EnvelopeAxesLabelsMapper envelopeDimensionsMapper,
            MIMETypeMapper mimemappe) {
        this.geoServer = geoServer;
        this.catalog = geoServer.getCatalog();
        this.responseFactory = responseFactory;
        this.envelopeAxesMapper = envelopeDimensionsMapper;
        this.mimeMapper = mimemappe;
        this.wcsDescribeCoverageExtensions =
                GeoServerExtensions.extensions(WCS20DescribeCoverageExtension.class);
        this.availableDescribeCovExtensions =
                wcsDescribeCoverageExtensions != null && !wcsDescribeCoverageExtensions.isEmpty();
    }

    @Override
    public WCSInfo getServiceInfo() {
        return geoServer.getService(WCSInfo.class);
    }

    @Override
    public TransformerBase getCapabilities(GetCapabilitiesType request) {
        checkService(request.getService());

        return new GetCapabilities(getServiceInfo(), responseFactory).run(request);
    }

    @Override
    public WCS20DescribeCoverageTransformer describeCoverage(DescribeCoverageType request) {
        checkService(request.getService());
        checkVersion(request.getVersion());

        if (request.getCoverageId() == null || request.getCoverageId().isEmpty()) {
            throw new OWS20Exception(
                    "Required parameter coverageId missing",
                    WCS20Exception.WCS20ExceptionCode.EmptyCoverageIdList,
                    "coverageId");
        }

        // check coverages are legit
        List<String> badCoverageIds = new ArrayList<String>();

        for (String encodedCoverageId : (List<String>) request.getCoverageId()) {
            String newCoverageID = encodedCoverageId;
            // Extension point for encoding the coverageId
            if (availableDescribeCovExtensions) {
                for (WCS20DescribeCoverageExtension ext : wcsDescribeCoverageExtensions) {
                    newCoverageID = ext.handleCoverageId(newCoverageID);
                }
            }
            LayerInfo layer = NCNameResourceCodec.getCoverage(catalog, newCoverageID);
            if (layer == null) {
                badCoverageIds.add(encodedCoverageId);
            }
        }
        if (!badCoverageIds.isEmpty()) {
            String mergedIds = StringUtils.merge(badCoverageIds);
            throw new WCS20Exception(
                    "Could not find the requested coverage(s): " + mergedIds,
                    WCS20Exception.WCS20ExceptionCode.NoSuchCoverage,
                    "coverageId");
        }

        WCSInfo wcs = getServiceInfo();

        WCS20DescribeCoverageTransformer describeTransformer =
                new WCS20DescribeCoverageTransformer(catalog, envelopeAxesMapper, mimeMapper);
        describeTransformer.setEncoding(
                Charset.forName(wcs.getGeoServer().getSettings().getCharset()));
        return describeTransformer;
    }

    @Override
    public GridCoverage getCoverage(GetCoverageType request) {
        checkService(request.getService());
        checkVersion(request.getVersion());

        if (request.getCoverageId() == null || "".equals(request.getCoverageId())) {
            throw new OWS20Exception(
                    "Required parameter coverageId missing",
                    WCS20Exception.WCS20ExceptionCode.EmptyCoverageIdList,
                    "coverageId");
        }

        return new GetCoverage(getServiceInfo(), catalog, envelopeAxesMapper, mimeMapper)
                .run(request);
    }

    @Override
    public TransformerBase describeEOCoverageSet(DescribeEOCoverageSetType request) {
        throw new ServiceException(
                "WCS-EO extension is not installed, thus the operation is not available");
    }
}
