/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import net.opengis.wcs11.GetCoverageType;
import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.MethodType;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.wcs.WebCoverageService100;
import org.geoserver.wcs.WebCoverageService111;
import org.geoserver.wcs2_0.WebCoverageService20;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.opengis.util.ProgressListener;
import org.springframework.context.ApplicationContext;

/**
 * Handles an internal reference to a local Coverage by a WCS request
 *
 * @author Andrea Aime - GeoSolutions
 */
public class InternalWCSInputProvider extends AbstractInputProvider {

    private ApplicationContext context;

    public InternalWCSInputProvider(
            InputType input, ProcessParameterIO ppio, ApplicationContext context) {
        super(input, ppio);
        this.context = context;
    }

    @Override
    protected Object getValueInternal(ProgressListener listener) throws Exception {
        // first parse the request, it might be a WCS 1.0 or a WCS 1.1 one
        Object getCoverage = null;
        InputReferenceType ref = input.getReference();
        if (ref.getMethod() == MethodType.POST_LITERAL) {
            getCoverage = ref.getBody();
        } else {
            // what WCS version?
            String version = getVersion(ref.getHref());
            KvpRequestReader reader;
            if ("1.0.0".equals(version) || "1.0".equals(version)) {
                reader = (KvpRequestReader) context.getBean("wcs100GetCoverageRequestReader");
            } else if ("2.0.1".equals(version) || "2.0.0".equals(version)) {
                reader = (KvpRequestReader) context.getBean("wcs20getCoverageKvpParser");
            } else {
                reader = (KvpRequestReader) context.getBean("wcs111GetCoverageRequestReader");
            }

            getCoverage = kvpParse(ref.getHref(), reader);
        }

        // perform GetCoverage
        if (getCoverage instanceof GetCoverageType) {
            WebCoverageService111 wcs =
                    (WebCoverageService111) context.getBean("wcs111ServiceTarget");
            return wcs.getCoverage((net.opengis.wcs11.GetCoverageType) getCoverage)[0];
        } else if (getCoverage instanceof net.opengis.wcs10.GetCoverageType) {
            WebCoverageService100 wcs =
                    (WebCoverageService100) context.getBean("wcs100ServiceTarget");
            return wcs.getCoverage((net.opengis.wcs10.GetCoverageType) getCoverage)[0];
        } else if (getCoverage instanceof net.opengis.wcs20.GetCoverageType) {
            WebCoverageService20 wcs = (WebCoverageService20) context.getBean("wcs20ServiceTarget");
            return wcs.getCoverage((net.opengis.wcs20.GetCoverageType) getCoverage);
        } else {
            throw new WPSException("Unrecognized request type " + getCoverage);
        }
    }

    @Override
    public int longStepCount() {
        return 0;
    }
}
