/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ows.wfs;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.configuration.TemplateLoader;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * An abstract template response to be extended from the output specific implementation, defining
 * the steps of the output production and providing hooks
 */
public abstract class BaseTemplateGetFeatureResponse extends WFSGetFeatureOutputFormat {

    protected TemplateLoader configuration;
    protected TemplateGetFeatureResponseHelper helper;
    protected TemplateIdentifier identifier;

    protected boolean hasGeometry;

    public BaseTemplateGetFeatureResponse(
            GeoServer gs, TemplateLoader configuration, TemplateIdentifier identifier) {
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
            writer.startTemplateOutput(null);
            iterateFeatureCollection(writer, featureCollection, getFeature);
            writer.endTemplateOutput(null);
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
            TemplateOutputWriter writer,
            FeatureCollectionResponse featureCollection,
            Operation operation)
            throws IOException, ExecutionException {
        List<FeatureCollection> collectionList = featureCollection.getFeature();

        for (FeatureCollection collection : collectionList) {
            FeatureTypeInfo fti = helper.getFeatureTypeInfo(collection);
            RootBuilder root = configuration.getTemplate(fti, getMimeType(null, operation));
            beforeFeatureIteration(writer, root, fti);
            iterateFeatures(root, writer, collection);
        }
    }

    protected void iterateFeatureCollection(
            TemplateOutputWriter writer, FeatureCollectionResponse featureCollection)
            throws IOException, ExecutionException {
        iterateFeatureCollection(writer, featureCollection, null);
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
                afterEvaluation(writer, rootBuilder, feature);
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
     * Allows subclasses to perform actions before evaluatig the feature
     *
     * @param writer the current TemplateWriter
     * @param root the current RootBuilder
     * @param feature the feature being evaluated by the builders' tree
     */
    protected void beforeEvaluation(TemplateOutputWriter writer, RootBuilder root, Feature feature)
            throws IOException {
        writer.incrementNumberReturned();
        if (!hasGeometry) {
            GeometryDescriptor descriptor = feature.getType().getGeometryDescriptor();
            if (descriptor != null) {
                Property geometry = feature.getProperty(descriptor.getName());
                hasGeometry = geometry != null;
                if (writer.getCrs() == null) {
                    CoordinateReferenceSystem featureCrs =
                            descriptor.getCoordinateReferenceSystem();
                    writer.setCrs(featureCrs);
                    writer.setAxisOrder(CRS.getAxisOrder(featureCrs));
                }
            }
        }
    }

    protected void afterEvaluation(TemplateOutputWriter writer, RootBuilder root, Feature feature)
            throws IOException {}

    /**
     * Method that trigger the encoding of a FeatureCollection additional infos like numberReturned,
     * numberMatched, CRS and boundingBox.
     *
     * @param writer the template writer being used to produce the format.
     * @param featureCollection the FeatureCollectionResponse object from which eventually retrieve
     *     the additional infos.
     * @param getFeature the getFeature Operation from which eventually retrieve addition infos.
     * @throws IOException
     */
    protected void writeAdditionalFields(
            TemplateOutputWriter writer,
            FeatureCollectionResponse featureCollection,
            Operation getFeature)
            throws IOException {
        BigInteger totalNumberOfFeatures = featureCollection.getTotalNumberOfFeatures();
        BigInteger featureCount =
                (totalNumberOfFeatures != null && totalNumberOfFeatures.longValue() < 0)
                        ? null
                        : totalNumberOfFeatures;
        boolean isFeatureBounding = getInfo().isFeatureBounding();

        ReferencedEnvelope featuresBounds =
                getBoundsFromFeatureCollections(featureCollection.getFeature(), isFeatureBounding);
        writeAdditionalFieldsInternal(
                writer, featureCollection, getFeature, featureCount, featuresBounds);
    }

    private ReferencedEnvelope getBoundsFromFeatureCollections(
            List<FeatureCollection> featureCollectionList, boolean isFeatureBounding) {
        ReferencedEnvelope e = null;
        if (hasGeometry && isFeatureBounding) {
            for (int i = 0; i < featureCollectionList.size(); i++) {
                FeatureCollection collection = featureCollectionList.get(i);
                if (e == null) e = collection.getBounds();
                else e.expandToInclude(collection.getBounds());
            }
        }
        return e;
    }

    /**
     * Method that has to be overridden by subclasses, to provide specific instruction for the
     * encoding of a FeatureCollection additional info like numberReturned, numberMatched, CRS and
     * boundingBox.
     *
     * @param writer the template writer being used to produce the format.
     * @param featureCollection the FeatureCollectionResponse object from which eventually retrieve
     *     the additional infos.
     * @param getFeature the getFeature Operation from which eventually retrieve addition infos.
     * @param featureCount the featureCount. Can be null.
     * @param bounds the featureBounds. Can be null.
     * @throws IOException
     */
    protected abstract void writeAdditionalFieldsInternal(
            TemplateOutputWriter writer,
            FeatureCollectionResponse featureCollection,
            Operation getFeature,
            BigInteger featureCount,
            ReferencedEnvelope bounds)
            throws IOException;
}
