/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.configuration.TemplateConfiguration;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;

/**
 * An abstract template response to be extended from the output specific implementation, defining
 * the steps of the output production and providing hooks
 */
public abstract class BaseTemplateGetFeatureResponse extends WFSGetFeatureOutputFormat {

    private TemplateConfiguration configuration;
    protected TemplateGetFeatureResponseHelper helper;
    protected TemplateIdentifier identifier;

    public BaseTemplateGetFeatureResponse(
            GeoServer gs, TemplateConfiguration configuration, TemplateIdentifier identifier) {
        super(gs, identifier.getOutputFormat());
        this.configuration = configuration;
        this.helper = new TemplateGetFeatureResponseHelper(gs.getCatalog(), identifier);
        this.identifier = identifier;
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
            beforeFeatureIteration(writer, root, fti);
            iterateFeatures(root, writer, collection);
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
    protected void iterateFeatures(
            RootBuilder rootBuilder, TemplateOutputWriter writer, FeatureCollection collection)
            throws IOException {
        FeatureIterator iterator = collection.features();
        try {
            while (iterator.hasNext()) {
                Feature feature = iterator.next();
                beforeEvaluation(writer, rootBuilder, feature);
                TemplateBuilderContext context = new TemplateBuilderContext(feature);
                rootBuilder.evaluate(writer, context);
            }
        } finally {
            iterator.close();
        }
    }

    /**
     * Allows subclasses to perform actions before starting the features iteration
     *
     * @param writer the current TemplateWriter
     * @param root the current RootBuilder
     */
    protected abstract void beforeFeatureIteration(
            TemplateOutputWriter writer, RootBuilder root, FeatureTypeInfo typeInfo);

    /**
     * Allows subclasses to perform actions before evaluation the feature
     *
     * @param writer the current TemplateWriter
     * @param root the current RootBuilder
     * @param feature the feature being evaluated by the builders' tree
     */
    protected abstract void beforeEvaluation(
            TemplateOutputWriter writer, RootBuilder root, Feature feature);
}
