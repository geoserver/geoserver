/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.wfs;

import static org.geoserver.featurestemplating.builders.VendorOptions.HEADER;
import static org.geoserver.featurestemplating.builders.VendorOptions.LINK;
import static org.geoserver.featurestemplating.builders.VendorOptions.SCRIPT;
import static org.geoserver.featurestemplating.builders.VendorOptions.STYLE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.EncodingHints;
import org.geoserver.featurestemplating.builders.VendorOptions;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.configuration.TemplateLoader;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geoserver.featurestemplating.writers.XHTMLTemplateWriter;
import org.geoserver.featurestemplating.writers.XMLTemplateWriter;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;

/** A template response able to write an HTML output format. */
public class HTMLTemplateResponse extends BaseTemplateGetFeatureResponse {

    public HTMLTemplateResponse(GeoServer gs, TemplateLoader configuration) {
        super(gs, configuration, TemplateIdentifier.HTML);
    }

    @Override
    protected void beforeFeatureIteration(
            TemplateOutputWriter writer, RootBuilder root, FeatureTypeInfo typeInfo) {}

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws ServiceException {
        String outputFormat = getMimeType(null, getFeature);
        try (XMLTemplateWriter writer = getOutputWriter(output, outputFormat)) {
            writer.startTemplateOutput(mapVendorOptionsToHints(featureCollection, outputFormat));
            iterateFeatureCollection(writer, featureCollection, getFeature);
            writer.endTemplateOutput(null);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private XMLEventReader getReader(String content) throws XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        return xmlInputFactory.createXMLEventReader(new ByteArrayInputStream(content.getBytes()));
    }

    @Override
    protected void writeAdditionalFieldsInternal(
            TemplateOutputWriter writer,
            FeatureCollectionResponse featureCollection,
            Operation getFeature,
            BigInteger featureCount,
            ReferencedEnvelope bounds)
            throws IOException {}

    protected XHTMLTemplateWriter getOutputWriter(OutputStream output, String outputFormat)
            throws IOException {
        return (XHTMLTemplateWriter) helper.getOutputWriter(output, outputFormat);
    }

    private EncodingHints mapVendorOptionsToHints(
            FeatureCollectionResponse response, String outputFormat) throws ExecutionException {
        List<FeatureCollection> collectionList = response.getFeature();
        List<String> scripts = new ArrayList<>();
        List<String> styles = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        EncodingHints encodingHints = new EncodingHints();
        for (FeatureCollection collection : collectionList) {
            FeatureTypeInfo fti = helper.getFeatureTypeInfo(collection);
            RootBuilder root = configuration.getTemplate(fti, outputFormat);
            String script = root.getVendorOptions().get(SCRIPT, String.class, "");
            String style = root.getVendorOptions().get(VendorOptions.STYLE, String.class, "");
            String header = root.getVendorOptions().get(VendorOptions.HEADER, String.class, "");
            if (!script.isEmpty()) scripts.add(script);
            if (!style.isEmpty()) styles.add(style);
            if (!header.isEmpty()) headers.add(header);
            VendorOptions options = root.getVendorOptions();
            addLinks(options, encodingHints);
        }
        if (!scripts.isEmpty()) encodingHints.put(SCRIPT, scripts);
        if (!styles.isEmpty()) encodingHints.put(STYLE, styles);
        if (!headers.isEmpty()) encodingHints.put(HEADER, styles);
        return encodingHints;
    }

    private void addLinks(VendorOptions options, EncodingHints hints) {
        if (options.keySet().stream().anyMatch(k -> k.startsWith(LINK))) {
            for (String key : options.keySet()) {
                if (key != null && key.startsWith(LINK)) hints.put(key, options.get(key));
            }
        }
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return TemplateIdentifier.HTML.getOutputFormat();
    }

    @Override
    protected void beforeEvaluation(TemplateOutputWriter writer, RootBuilder root, Feature feature)
            throws IOException {
        super.beforeEvaluation(writer, root, feature);
    }

    @Override
    protected void afterEvaluation(TemplateOutputWriter writer, RootBuilder root, Feature feature)
            throws IOException {
        super.afterEvaluation(writer, root, feature);
    }
}
