/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ows.wms;

import java.io.IOException;
import java.io.OutputStream;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geoserver.wms.featureinfo.GetFeatureInfoResponse;

public class TemplateGetFeatureInfoResponse extends GetFeatureInfoResponse {
    /**
     * Creates a new GetMapResponse object.
     *
     * @param wms
     * @param defaultOutputFormat
     */
    private GetFeatureInfoOutputFormat outputFormat;

    public TemplateGetFeatureInfoResponse(WMS wms, GetFeatureInfoOutputFormat outputFormat) {
        super(wms, null);
        if (wms.isAllowedGetFeatureInfoFormat(outputFormat) == false) {
            throw wms.unallowedGetFeatureInfoFormatException(outputFormat.getContentType());
        }
        this.outputFormat = outputFormat;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        GetFeatureInfoRequest request = (GetFeatureInfoRequest) operation.getParameters()[0];
        FeatureCollectionType results = (FeatureCollectionType) value;
        outputFormat.write(results, request, output);
    }
}
