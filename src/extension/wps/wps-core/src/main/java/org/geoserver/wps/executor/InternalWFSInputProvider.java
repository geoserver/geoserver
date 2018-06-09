/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import net.opengis.wfs.GetFeatureType;
import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.MethodType;
import org.geoserver.wfs.WebFeatureService;
import org.geoserver.wfs.kvp.GetFeatureKvpRequestReader;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.opengis.util.ProgressListener;
import org.springframework.context.ApplicationContext;

/**
 * Handles an internal reference to a local FeatureType by a WFS request
 *
 * @author Andrea Aime - GeoSolutions
 */
public class InternalWFSInputProvider extends AbstractInputProvider {

    private ApplicationContext context;

    public InternalWFSInputProvider(
            InputType input, ProcessParameterIO ppio, ApplicationContext context) {
        super(input, ppio);
        this.context = context;
    }

    @Override
    protected Object getValueInternal(ProgressListener listener) throws Exception {
        WebFeatureService wfs = (WebFeatureService) context.getBean("wfsServiceTarget");
        GetFeatureType gft = null;
        InputReferenceType ref = input.getReference();
        if (ref.getMethod() == MethodType.POST_LITERAL) {
            gft = (GetFeatureType) ref.getBody();
        } else {
            GetFeatureKvpRequestReader reader =
                    (GetFeatureKvpRequestReader) context.getBean("getFeatureKvpReader");
            gft = (GetFeatureType) kvpParse(ref.getHref(), reader);
        }

        FeatureCollectionResponse featureCollectionType = wfs.getFeature(gft);
        // this will also deal with axis order issues
        return ((ComplexPPIO) ppio).decode(featureCollectionType.getAdaptee());
    }

    @Override
    public int longStepCount() {
        return 0;
    }
}
