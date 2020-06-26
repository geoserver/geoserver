/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfstemplating.response;

import java.io.*;
import java.util.List;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfstemplating.builders.TemplateBuilder;
import org.geoserver.wfstemplating.builders.impl.RootBuilder;
import org.geoserver.wfstemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.wfstemplating.builders.jsonld.JsonLdRootBuilder;
import org.geoserver.wfstemplating.configuration.TemplateConfiguration;
import org.geoserver.wfstemplating.configuration.TemplateIdentifier;
import org.geoserver.wfstemplating.writers.JsonLdWriter;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;

/**
 * Encodes features in json-ld output format by means of a ${@link TemplateBuilder} tree obtained by
 * a JSON-LD template
 */
public class JSONLDGetFeatureResponse extends WFSGetFeatureOutputFormat {

    /** The MIME type for a JSON-LD response* */
    public static final String MIME = "application/ld+json";

    private TemplateConfiguration configuration;

    public JSONLDGetFeatureResponse(GeoServer gs, TemplateConfiguration configuration) {
        super(gs, MIME);
        this.configuration = configuration;
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws ServiceException {

        TemplateGetFeatureResponseHelper helper =
                new TemplateGetFeatureResponseHelper(gs.getCatalog(), TemplateIdentifier.JSONLD);
        FeatureTypeInfo info =
                helper.getFeatureTypeInfo(GetFeatureRequest.adapt(getFeature.getParameters()[0]));
        JsonLdWriter writer = null;
        try {
            JsonLdRootBuilder rootBuilder =
                    (JsonLdRootBuilder) configuration.getTemplate(info, MIME);
            if (rootBuilder == null) {
                throw new RuntimeException(
                        "No template found for feature type "
                                + info.getName()
                                + "and output format"
                                + TemplateIdentifier.JSONLD.getOutputFormat());
            }
            String flattenedList =
                    rootBuilder.getVendorOption(
                            RootBuilder.VendorOption.FLAT_OUTPUT.getVendorOptionName());
            writer =
                    (JsonLdWriter)
                            helper.getOutputWriter(
                                    output,
                                    flattenedList != null ? Boolean.valueOf(flattenedList) : false);
            writer.setContextHeader(rootBuilder.getContextHeader());
            writer.startJson();
            List<FeatureCollection> collectionList = featureCollection.getFeature();

            for (FeatureCollection collection : collectionList) {
                FeatureTypeInfo fti = helper.getFeatureTypeInfo(collection);
                if (!info.getName().equals(fti.getName())) info = fti;
                JsonLdRootBuilder rb = (JsonLdRootBuilder) configuration.getTemplate(fti, MIME);
                if (rb == null) {
                    throw new RuntimeException(
                            "No template found for feature type "
                                    + info.getName()
                                    + "and output format"
                                    + TemplateIdentifier.JSONLD.getOutputFormat());
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
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MIME;
    }
}
