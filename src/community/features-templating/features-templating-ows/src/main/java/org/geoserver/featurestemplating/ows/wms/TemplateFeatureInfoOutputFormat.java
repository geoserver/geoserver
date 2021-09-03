/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ows.wms;

import java.io.IOException;
import java.io.OutputStream;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.ows.OWSResponseFactory;
import org.geoserver.featurestemplating.ows.wfs.BaseTemplateGetFeatureResponse;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;

public abstract class TemplateFeatureInfoOutputFormat extends GetFeatureInfoOutputFormat {

    private TemplateIdentifier identifier;

    public TemplateFeatureInfoOutputFormat(TemplateIdentifier identifier, String origFormat) {
        super(origFormat);
        this.identifier = identifier;
    }

    @Override
    public void write(
            FeatureCollectionType results, GetFeatureInfoRequest request, OutputStream out)
            throws ServiceException, IOException {
        BaseTemplateGetFeatureResponse response =
                OWSResponseFactory.getInstance().getFeatureResponse(identifier);
        response.write(results, out, Dispatcher.REQUEST.get().getOperation());
    }
}
