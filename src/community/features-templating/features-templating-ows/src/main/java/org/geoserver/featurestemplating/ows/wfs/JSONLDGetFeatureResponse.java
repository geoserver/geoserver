/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ows.wfs;

import java.io.*;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.EncodingHints;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.VendorOptions;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.configuration.TemplateLoader;
import org.geoserver.featurestemplating.validation.JSONLDContextValidation;
import org.geoserver.featurestemplating.writers.JSONLDOutputHelper;
import org.geoserver.featurestemplating.writers.JSONLDWriter;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.api.feature.Feature;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * Encodes features in json-ld output format by means of a ${@link TemplateBuilder} tree obtained by
 * a JSON-LD template
 */
public class JSONLDGetFeatureResponse extends BaseTemplateGetFeatureResponse {

    private static final String ELEMENT_NAME = "JSON-LD";

    public JSONLDGetFeatureResponse(GeoServer gs, TemplateLoader configuration) {
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

        JSONLDContextValidation validator = null;
        try {
            EncodingHints encodingHints =
                    new JSONLDOutputHelper().optionsToEncodingHints(featureCollection.getFeature());
            boolean validate = isSemanticValidation();
            // setting it back to false
            if (validate) {
                validate(featureCollection, encodingHints, getFeature);
            }
            try (JSONLDWriter writer = (JSONLDWriter) helper.getOutputWriter(output)) {
                write(featureCollection, writer, encodingHints, getFeature);
            } catch (Exception e) {
                throw new ServiceException(e);
            }
        } catch (Exception e) {
            throw new ServiceException(e);
        } finally {
            if (validator != null) {
                validator.validate();
            }
        }
    }

    private void validate(
            FeatureCollectionResponse featureCollection,
            EncodingHints encodingHints,
            Operation operation) {
        JSONLDContextValidation validator = new JSONLDContextValidation();
        try (JSONLDWriter writer =
                (JSONLDWriter) helper.getOutputWriter(new FileOutputStream(validator.init()))) {
            write(featureCollection, writer, encodingHints, operation);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
        validator.validate();
    }

    private void write(
            FeatureCollectionResponse featureCollection,
            JSONLDWriter writer,
            EncodingHints encodingHints,
            Operation operation)
            throws IOException, ExecutionException {
        writer.startTemplateOutput(encodingHints);
        iterateFeatureCollection(writer, featureCollection, operation);
        writer.endTemplateOutput(encodingHints);
    }

    @Override
    protected void beforeFeatureIteration(
            TemplateOutputWriter writer, RootBuilder root, FeatureTypeInfo typeInfo) {
        Boolean encodeAsString =
                root.getVendorOptions()
                        .get(VendorOptions.JSON_LD_STRING_ENCODE, Boolean.class, false);
        JSONLDWriter jsonldWriter = (JSONLDWriter) writer;
        jsonldWriter.setEncodeAsString(encodeAsString);
    }

    @Override
    protected void beforeEvaluation(
            TemplateOutputWriter writer, RootBuilder root, Feature feature) {}

    @Override
    protected void writeAdditionalFieldsInternal(
            TemplateOutputWriter writer,
            FeatureCollectionResponse featureCollection,
            Operation getFeature,
            BigInteger featureCount,
            ReferencedEnvelope bounds)
            throws IOException {
        // do nothing
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return TemplateIdentifier.JSONLD.getOutputFormat();
    }

    private boolean isSemanticValidation() {
        Request request = Dispatcher.REQUEST.get();
        Map rawKvp = request.getRawKvp();
        Object value = rawKvp != null ? rawKvp.get("validation") : null;
        boolean result = false;
        if (value != null) {
            result = Boolean.valueOf(value.toString());
        }
        return result;
    }

    @Override
    public String getCapabilitiesElementName() {
        return ELEMENT_NAME;
    }
}
