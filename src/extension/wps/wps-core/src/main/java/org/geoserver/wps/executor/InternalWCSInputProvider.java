/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.lang.reflect.Method;
import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.MethodType;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.wcs2_0.WebCoverageService20;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geotools.api.util.ProgressListener;
import org.springframework.context.ApplicationContext;

/**
 * Handles an internal reference to a local Coverage by a WCS request
 *
 * @author Andrea Aime - GeoSolutions
 */
public class InternalWCSInputProvider extends AbstractInputProvider {

    private ApplicationContext context;

    public InternalWCSInputProvider(InputType input, ProcessParameterIO ppio, ApplicationContext context) {
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
        if (getCoverage instanceof net.opengis.wcs11.GetCoverageType type2) {
            Object wcs = context.getBean("wcs111ServiceTarget");
            Method getCoverageMethod = wcs.getClass().getMethod("getCoverage", net.opengis.wcs11.GetCoverageType.class);
            Object[] result = (Object[]) getCoverageMethod.invoke(wcs, type2);
            return result[0];
        } else if (getCoverage instanceof net.opengis.wcs10.GetCoverageType type1) {
            Object wcs = context.getBean("wcs100ServiceTarget");
            Method getCoverageMethod = wcs.getClass().getMethod("getCoverage", net.opengis.wcs10.GetCoverageType.class);
            Object[] result = (Object[]) getCoverageMethod.invoke(wcs, type1);
            return result[0];
        } else if (getCoverage instanceof net.opengis.wcs20.GetCoverageType type) {
            WebCoverageService20 wcs = (WebCoverageService20) context.getBean("wcs20ServiceTarget");
            return wcs.getCoverage(type);
        } else {
            throw new WPSException("Unrecognized request type " + getCoverage);
        }
    }

    @Override
    public int longStepCount() {
        return 0;
    }
}
