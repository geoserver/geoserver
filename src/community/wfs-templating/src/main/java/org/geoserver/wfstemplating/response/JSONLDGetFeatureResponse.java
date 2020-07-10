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
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfstemplating.builders.TemplateBuilder;
import org.geoserver.wfstemplating.builders.impl.RootBuilder;
import org.geoserver.wfstemplating.builders.jsonld.JsonLdRootBuilder;
import org.geoserver.wfstemplating.configuration.TemplateConfiguration;
import org.geoserver.wfstemplating.configuration.TemplateIdentifier;
import org.geoserver.wfstemplating.writers.JsonLdWriter;
import org.geoserver.wfstemplating.writers.TemplateOutputWriter;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;

/**
 * Encodes features in json-ld output format by means of a ${@link TemplateBuilder} tree obtained by
 * a JSON-LD template
 */
public class JSONLDGetFeatureResponse extends BaseTemplateGetFeatureResponse {

    private TemplateConfiguration configuration;

    public JSONLDGetFeatureResponse(GeoServer gs, TemplateConfiguration configuration) {
        super(gs, configuration, TemplateIdentifier.JSONLD);
        this.configuration = configuration;
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws ServiceException {
        //  Multiple FeatureType encoding for json-ld has not be implemented.
        //  Is missed a strategy for multiple context in template. Probably we should merge them
        // before
        //  writing the context to the output.
        //  This is thus working only for one featureType and so the RootBuilder is being got before
        // iteration.
        try (JsonLdWriter writer = (JsonLdWriter) helper.getOutputWriter(output)) {
            FeatureTypeInfo info =
                    helper.getFirstFeatureTypeInfo(
                            GetFeatureRequest.adapt(getFeature.getParameters()[0]));
            JsonLdRootBuilder root =
                    (JsonLdRootBuilder)
                            configuration.getTemplate(
                                    info, TemplateIdentifier.JSONLD.getOutputFormat());
            writer.setContextHeader(root.getContextHeader());
            writer.startTemplateOutput();
            iterateFeatureCollection(writer, featureCollection, root);
            writer.endTemplateOutput();
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    @Override
    protected void beforeFeatureIteration(
            TemplateOutputWriter writer, RootBuilder root, FeatureTypeInfo typeInfo) {}

    protected void iterateFeatureCollection(
            TemplateOutputWriter writer,
            FeatureCollectionResponse featureCollection,
            RootBuilder root)
            throws IOException {
        List<FeatureCollection> collectionList = featureCollection.getFeature();

        for (FeatureCollection collection : collectionList) {
            iterateFeatures(root, writer, collection);
        }
    }

    @Override
    protected void beforeEvaluation(
            TemplateOutputWriter writer, RootBuilder root, Feature feature) {}

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return TemplateIdentifier.JSONLD.getOutputFormat();
    }
}
