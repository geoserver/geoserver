/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfstemplating.response;

import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfstemplating.builders.geojson.GeoJsonRootBuilder;
import org.geoserver.wfstemplating.builders.impl.RootBuilder;
import org.geoserver.wfstemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.wfstemplating.configuration.TemplateConfiguration;
import org.geoserver.wfstemplating.configuration.TemplateIdentifier;
import org.geoserver.wfstemplating.writers.CommonJsonWriter;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;

/** Write a valid GeoJSON output from a template */
public class GeoJsonTemplateGetFeatureResponse extends WFSGetFeatureOutputFormat {

    private TemplateConfiguration configuration;

    private GeoJSONGetFeatureResponse delegate;

    public GeoJsonTemplateGetFeatureResponse(
            GeoServer gs, TemplateConfiguration configuration, GeoJSONGetFeatureResponse delegate) {
        super(gs, TemplateIdentifier.GEOJSON.getOutputFormat());
        this.configuration = configuration;
        this.delegate = delegate;
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws ServiceException {
        String mime = getMimeType(null, null);
        TemplateGetFeatureResponseHelper helper =
                new TemplateGetFeatureResponseHelper(gs.getCatalog(), TemplateIdentifier.GEOJSON);
        GetFeatureRequest getFeatureRequest =
                GetFeatureRequest.adapt(getFeature.getParameters()[0]);
        FeatureTypeInfo info =
                getFeatureRequest != null
                        ? helper.getFeatureTypeInfo(getFeatureRequest)
                        : helper.getFeatureType(getFeature.getParameters()[0].toString());
        CommonJsonWriter writer = null;
        try {
            GeoJsonRootBuilder rootBuilder =
                    (GeoJsonRootBuilder) configuration.getTemplate(info, mime);
            if (rootBuilder == null) {
                throw new RuntimeException(
                        "No template found for feature type "
                                + info.getName()
                                + "for output format"
                                + TemplateIdentifier.GEOJSON.getOutputFormat());
            }
            String flattenedList =
                    rootBuilder.getVendorOption(
                            RootBuilder.VendorOption.FLAT_OUTPUT.getVendorOptionName());
            writer =
                    (CommonJsonWriter)
                            helper.getOutputWriter(
                                    output,
                                    flattenedList != null ? Boolean.valueOf(flattenedList) : false);
            writer.startJson();
            for (FeatureCollection collection : featureCollection.getFeature()) {
                FeatureTypeInfo fti = helper.getFeatureTypeInfo(collection);
                if (!info.getName().equals(fti.getName())) info = fti;
                GeoJsonRootBuilder rb = (GeoJsonRootBuilder) configuration.getTemplate(fti, mime);
                if (rb == null) {
                    throw new RuntimeException(
                            "No template found for feature type "
                                    + info.getName()
                                    + "and output format"
                                    + TemplateIdentifier.GEOJSON.getOutputFormat());
                } else {
                    rootBuilder = rb;
                }
                FeatureIterator iterator = collection.features();
                try {
                    while (iterator.hasNext()) {
                        TemplateBuilderContext context =
                                new TemplateBuilderContext(iterator.next());
                        rootBuilder.evaluate(writer, context);
                    }
                } finally {
                    iterator.close();
                }
            }
            writer.endJson();
        } catch (Exception e) {
            throw new ServiceException(e);
        } finally {
            try {
                if (writer != null && !writer.isClosed()) writer.close();
                output.close();
            } catch (IOException ioex) {
                throw new ServiceException(ioex);
            }
        }
    }

    @Override
    public String getCapabilitiesElementName() {
        return delegate.getCapabilitiesElementName();
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return delegate.getMimeType(value, operation);
    }
}
