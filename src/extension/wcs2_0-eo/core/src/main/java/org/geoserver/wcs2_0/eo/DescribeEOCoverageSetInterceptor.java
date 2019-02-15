/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo;

import static org.geoserver.wcs2_0.util.RequestUtils.checkService;
import static org.geoserver.wcs2_0.util.RequestUtils.checkVersion;

import java.util.ArrayList;
import java.util.List;
import net.opengis.wcs20.DescribeEOCoverageSetType;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.platform.OWS20Exception.OWSExceptionCode;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.eo.response.DescribeEOCoverageSetTransformer;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.response.MIMETypeMapper;
import org.geoserver.wcs2_0.response.WCS20DescribeCoverageTransformer;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geoserver.wcs2_0.util.StringUtils;

/**
 * AOP interceptor running DescribeEOCoverageSet for WCS 2.O EO
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DescribeEOCoverageSetInterceptor implements MethodInterceptor {

    EOCoverageResourceCodec resourceCodec;

    private GeoServer geoServer;

    private Catalog catalog;

    private EnvelopeAxesLabelsMapper envelopeAxesMapper;

    private MIMETypeMapper mimemapper;

    public DescribeEOCoverageSetInterceptor(
            GeoServer geoServer,
            EnvelopeAxesLabelsMapper envelopeDimensionsMapper,
            MIMETypeMapper mimemappe,
            EOCoverageResourceCodec resourceCodec) {
        this.geoServer = geoServer;
        this.catalog = geoServer.getCatalog();
        this.envelopeAxesMapper = envelopeDimensionsMapper;
        this.mimemapper = mimemappe;
        this.resourceCodec = resourceCodec;
    }

    public WCSInfo getServiceInfo() {
        return geoServer.getService(WCSInfo.class);
    }

    private boolean isEarthObservationEnabled() {
        WCSInfo wcs = getServiceInfo();
        Boolean enabled = wcs.getMetadata().get(WCSEOMetadata.ENABLED.key, Boolean.class);
        return Boolean.TRUE.equals(enabled);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (invocation.getMethod().getName().equals("describeEOCoverageSet")
                && isEarthObservationEnabled()) {
            try {
                DescribeEOCoverageSetType dcs =
                        (DescribeEOCoverageSetType) invocation.getArguments()[0];
                return describeEOCoverageSet(dcs);
            } catch (Exception e) {
                if (e instanceof ServiceException) {
                    throw e;
                } else {
                    throw new ServiceException(e);
                }
            }
        } else {
            return invocation.proceed();
        }
    }

    private Object describeEOCoverageSet(DescribeEOCoverageSetType dcs) {
        checkService(dcs.getService());
        checkVersion(dcs.getVersion());

        if (dcs.getEoId() == null || dcs.getEoId().isEmpty()) {
            throw new OWS20Exception(
                    "Required parameter eoID missing",
                    new OWSExceptionCode("emptyEoIdList", 404),
                    "eoid");
        }

        // check coverages are legit
        List<String> badCoverageIds = new ArrayList<String>();

        for (String datasetId : (List<String>) dcs.getEoId()) {
            CoverageInfo layer = resourceCodec.getDatasetCoverage(datasetId);
            if (layer == null) {
                badCoverageIds.add(datasetId);
            }
        }
        if (!badCoverageIds.isEmpty()) {
            String mergedIds = StringUtils.merge(badCoverageIds);
            throw new WCS20Exception(
                    "Could not find the requested coverage(s): " + mergedIds,
                    new OWSExceptionCode("noSuchEODataset", 404),
                    "eoid");
        }

        WCS20DescribeCoverageTransformer tx =
                new WCS20DescribeCoverageTransformer(catalog, envelopeAxesMapper, mimemapper);
        return new DescribeEOCoverageSetTransformer(
                getServiceInfo(), resourceCodec, envelopeAxesMapper, tx);
    }
}
