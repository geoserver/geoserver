/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ows.wfs;

import static org.geoserver.featurestemplating.builders.EncodingHints.isSingleFeatureRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.EncodingHints;
import org.geoserver.featurestemplating.builders.VendorOptions;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.configuration.TemplateLoader;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.geometry.jts.ReferencedEnvelope;

/** Write a valid GeoJSON output from a template */
public class GeoJSONTemplateGetFeatureResponse extends BaseTemplateGetFeatureResponse {

    protected boolean hasGeometry;

    public GeoJSONTemplateGetFeatureResponse(
            GeoServer gs, TemplateLoader configuration, TemplateIdentifier identifier) {
        super(gs, configuration, identifier);
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws ServiceException {

        try (GeoJSONWriter writer = getOutputWriter(output)) {
            EncodingHints encodingHints = new EncodingHints();
            writer.startTemplateOutput(encodingHints);
            iterateFeatureCollection(writer, featureCollection);
            if (!isSingleFeatureRequest() || !identifier.equals(TemplateIdentifier.GEOJSON))
                writer.endArray(null, null);
            writeAdditionalFields(writer, featureCollection, getFeature);
            writer.endTemplateOutput(encodingHints);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    protected GeoJSONWriter getOutputWriter(OutputStream output) throws IOException {
        return (GeoJSONWriter) helper.getOutputWriter(output);
    }

    @Override
    protected void beforeFeatureIteration(
            TemplateOutputWriter writer, RootBuilder root, FeatureTypeInfo typeInfo) {
        RootBuilder rb = root;
        GeoJSONWriter jsonWriter = (GeoJSONWriter) writer;
        boolean flatOutput =
                rb.getVendorOptions()
                        .get(VendorOptions.FLAT_OUTPUT, Boolean.class, false)
                        .booleanValue();
        jsonWriter.setFlatOutput(flatOutput);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return identifier.getOutputFormat();
    }

    @Override
    protected void writeAdditionalFieldsInternal(
            TemplateOutputWriter writer,
            FeatureCollectionResponse featureCollection,
            Operation getFeature,
            BigInteger featureCount,
            ReferencedEnvelope bounds)
            throws IOException {

        writer.writeCollectionCounts(featureCount);
        writer.writeTimeStamp();
        String previous = featureCollection.getPrevious();
        String next = featureCollection.getNext();
        if (next != null || previous != null)
            ((GeoJSONWriter) writer).writePagingLinks(identifier.getOutputFormat(), previous, next);
        writer.writeCrs();

        if (bounds != null) writer.writeCollectionBounds(bounds);
    }
}
