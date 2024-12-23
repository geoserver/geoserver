/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.MethodType;
import org.geoserver.wfs.WebFeatureService;
import org.geoserver.wfs.WebFeatureService20;
import org.geoserver.wfs.kvp.GetFeatureKvpRequestReader;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geotools.api.util.ProgressListener;
import org.springframework.context.ApplicationContext;

/**
 * Handles an internal reference to a local FeatureType by a WFS request
 *
 * @author Andrea Aime - GeoSolutions
 */
public class InternalWFSInputProvider extends AbstractInputProvider {

    private ApplicationContext context;

    public InternalWFSInputProvider(InputType input, ProcessParameterIO ppio, ApplicationContext context) {
        super(input, ppio);
        this.context = context;
    }

    @Override
    protected Object getValueInternal(ProgressListener listener) throws Exception {
        Object gft = null;
        InputReferenceType ref = input.getReference();

        if (ref.getMethod() == MethodType.POST_LITERAL) {
            gft = ref.getBody();
        } else {
            String version = getVersion(ref.getHref());
            GetFeatureKvpRequestReader reader;
            if ("2.0.0".equals(version)) {
                reader = (GetFeatureKvpRequestReader) context.getBean("getFeature20KvpReader");
            } else {
                reader = (GetFeatureKvpRequestReader) context.getBean("getFeatureKvpReader");
            }
            gft = kvpParse(ref.getHref(), reader);
        }

        if (gft instanceof net.opengis.wfs.GetFeatureType) {
            WebFeatureService wfs = (WebFeatureService) context.getBean("wfsServiceTarget");

            FeatureCollectionResponse featureCollectionType = wfs.getFeature((net.opengis.wfs.GetFeatureType) gft);
            // this will also deal with axis order issues
            return ((ComplexPPIO) ppio).decode(featureCollectionType.getAdaptee());
        } else if (gft instanceof net.opengis.wfs20.GetFeatureType) {
            WebFeatureService20 wfs = (WebFeatureService20) context.getBean("wfsService20Target");

            FeatureCollectionResponse featureCollectionType = wfs.getFeature((net.opengis.wfs20.GetFeatureType) gft);
            // this will also deal with axis order issues
            return ((ComplexPPIO) ppio).decode(featureCollectionType.getAdaptee());
        } else {

            if (gft == null) {
                throw new UnsupportedOperationException("We didn't get a valid GetFeatureType.");
            }
            throw new UnsupportedOperationException(
                    "We can't handle the inner WFS request. Provided GetFeatureType is of class "
                            + gft.getClass().getCanonicalName()
                            + "\nContent: ["
                            + gft
                            + "]\n");
        }
    }

    @Override
    public int longStepCount() {
        return 0;
    }
}
