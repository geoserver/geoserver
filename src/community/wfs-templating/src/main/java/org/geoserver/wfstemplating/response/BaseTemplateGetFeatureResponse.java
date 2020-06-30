/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfstemplating.response;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfstemplating.builders.impl.RootBuilder;
import org.geoserver.wfstemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.wfstemplating.configuration.TemplateConfiguration;
import org.geoserver.wfstemplating.configuration.TemplateIdentifier;
import org.geoserver.wfstemplating.writers.TemplateOutputWriter;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;

/**
 * An abstract template response to be extended from the output specific implementation, defining
 * the steps of the output production
 */
public abstract class BaseTemplateGetFeatureResponse extends WFSGetFeatureOutputFormat {

    private TemplateConfiguration configuration;
    protected TemplateGetFeatureResponseHelper helper;

    public BaseTemplateGetFeatureResponse(
            GeoServer gs, TemplateConfiguration configuration, TemplateIdentifier identifier) {
        super(gs, identifier.getOutputFormat());
        this.configuration = configuration;
        this.helper = new TemplateGetFeatureResponseHelper(gs.getCatalog(), identifier);
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws ServiceException {

        try (TemplateOutputWriter writer = helper.getOutputWriter(output)) {
            writer.startTemplateOutput();
            iterateFeatureCollection(writer, featureCollection);
            writer.endTemplateOutput();
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    /**
     * Iterate over the FeatureCollection list get the template for each featureType and invoke
     * template evaluation
     *
     * @param writer the template writer
     * @param featureCollection the FeatureCollectionResponse
     * @throws IOException
     * @throws ExecutionException
     */
    protected void iterateFeatureCollection(
            TemplateOutputWriter writer, FeatureCollectionResponse featureCollection)
            throws IOException, ExecutionException {
        List<FeatureCollection> collectionList = featureCollection.getFeature();

        for (FeatureCollection collection : collectionList) {
            FeatureTypeInfo fti = helper.getFeatureTypeInfo(collection);
            RootBuilder root = configuration.getTemplate(fti, getMimeType(null, null));
            beforeEvaluation(writer, root);
            doBuilderEvaluation(root, writer, collection);
        }
    }

    /**
     * Start the builder evaluation from the RootBuilder
     *
     * @param rootBuilder the root builder representing the template
     * @param writer the template writer
     * @param collection the feature collection to be evaluated
     * @throws IOException
     */
    protected void doBuilderEvaluation(
            RootBuilder rootBuilder, TemplateOutputWriter writer, FeatureCollection collection)
            throws IOException {
        FeatureIterator iterator = collection.features();
        try {
            while (iterator.hasNext()) {
                TemplateBuilderContext context = new TemplateBuilderContext(iterator.next());
                rootBuilder.evaluate(writer, context);
            }
        } finally {
            iterator.close();
        }
    }

    /**
     * Allows to perform actions before starting the evaluation process
     *
     * @param writer
     * @param root
     */
    protected abstract void beforeEvaluation(TemplateOutputWriter writer, RootBuilder root);
}
