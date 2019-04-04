/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xslt;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.beanutils.BeanUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.xslt.config.TransformInfo;
import org.geoserver.wfs.xslt.config.TransformRepository;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Output format based on XLST transformations
 *
 * @author Andrea Aime - GeoSolutions
 */
public class XSLTOutputFormat extends WFSGetFeatureOutputFormat
        implements ApplicationContextAware, DisposableBean {

    static Map<String, String> formats = new ConcurrentHashMap<String, String>();

    ExecutorService executor = Executors.newCachedThreadPool();

    private TransformRepository repository;

    private List<Response> responses;

    public XSLTOutputFormat(GeoServer gs, TransformRepository repository) {
        // initialize with the key set of formats, so that it will change as
        // we register new formats
        super(gs, formats.keySet());
        this.repository = repository;
    }

    @Override
    public boolean canHandle(Operation operation) {
        // if we don't have formats configured, we cannot respond
        if (formats.isEmpty()) {
            LOGGER.log(Level.FINE, "Empty formats");
            return false;
        }

        if (!super.canHandle(operation)) {
            return false;
        }

        // check the format matches, the Dispatcher just does a case insensitive match,
        // but WFS is supposed to be case sensitive and so is the XSLT code
        Request request = Dispatcher.REQUEST.get();
        if (request != null
                && (request.getOutputFormat() == null
                        || !formats.containsKey(request.getOutputFormat()))) {
            LOGGER.log(Level.FINE, "Formats are: " + formats);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // find all the responses we could use as a source
        List<Response> all = GeoServerExtensions.extensions(Response.class, applicationContext);
        responses = new ArrayList<Response>();
        for (Response response : all) {
            if (response.getBinding().equals(FeatureCollectionResponse.class) && response != this) {
                responses.add(response);
            }
        }
    }

    public static void updateFormats(Set<String> newFormats) {
        if (!formats.keySet().equals(newFormats)) {
            Map<String, String> replacement = new HashMap<>();
            for (String format : newFormats) {
                replacement.put(format, format);
            }
            formats.clear();
            formats.putAll(replacement);
        }
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        try {
            TransformInfo info = locateTransformation((FeatureCollectionResponse) value, operation);
            return info.mimeType();
        } catch (IOException e) {
            throw new WFSException("Failed to load the required transformation", e);
        }
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        try {
            FeatureCollectionResponse featureCollections = (FeatureCollectionResponse) value;
            TransformInfo info = locateTransformation(featureCollections, operation);

            // concatenate all feature types requested
            StringBuilder sb = new StringBuilder();
            for (FeatureCollection<FeatureType, Feature> fc : featureCollections.getFeatures()) {
                sb.append(fc.getSchema().getName().getLocalPart());
                sb.append("_");
            }
            sb.setLength(sb.length() - 1);

            String extension = info.getFileExtension();
            if (extension == null) {
                extension = ".txt";
                sb.append(extension);
            }
            if (!extension.startsWith(".")) {
                sb.append(".");
            }
            sb.append(extension);

            return sb.toString();
        } catch (IOException e) {
            throw new WFSException("Failed to locate the XSLT transformation", e);
        }
    }

    @Override
    protected void write(
            final FeatureCollectionResponse featureCollection,
            OutputStream output,
            Operation operation)
            throws IOException, ServiceException {
        // get the transformation we need
        TransformInfo info = locateTransformation(featureCollection, operation);
        Transformer transformer = repository.getTransformer(info);
        // force Xalan to indent the output
        if (transformer.getOutputProperties() != null
                && "yes".equals(transformer.getOutputProperties().getProperty("indent"))) {
            try {
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.FINE, "Could not set indent amount", e);
                // in case it's not Xalan
            }
        }

        // prepare the fake operation we're providing to the source output format
        final Operation sourceOperation = buildSourceOperation(operation, info);

        // lookup the operation we are going to use
        final Response sourceResponse = findSourceResponse(sourceOperation, info);
        if (sourceResponse == null) {
            throw new WFSException(
                    "Could not locate a response that can generate the desired source format '"
                            + info.getSourceFormat()
                            + "' for transformation '"
                            + info.getName()
                            + "'");
        }

        // prepare the stream connections, so that we can do the transformation on the fly
        PipedInputStream pis = new PipedInputStream();
        final PipedOutputStream pos = new PipedOutputStream(pis);

        // submit the source output format execution, tracking exceptions
        Future<Void> future =
                executor.submit(
                        new Callable<Void>() {

                            @Override
                            public Void call() throws Exception {
                                try {
                                    sourceResponse.write(featureCollection, pos, sourceOperation);
                                } finally {
                                    // close the stream to make sure the transformation won't keep
                                    // on waiting
                                    pos.close();
                                }

                                return null;
                            }
                        });

        // run the transformation
        TransformerException transformerException = null;
        try {
            transformer.transform(new StreamSource(pis), new StreamResult(output));
        } catch (TransformerException e) {
            transformerException = e;
        } finally {
            pis.close();
        }

        // now handle exceptions, starting from the source
        try {
            future.get();
        } catch (Exception e) {
            throw new WFSException(
                    "Failed to run the output format generating the source for the XSTL transformation",
                    e);
        }
        if (transformerException != null) {
            throw new WFSException(
                    "Failed to run the the XSTL transformation", transformerException);
        }
    }

    private Operation buildSourceOperation(Operation operation, TransformInfo info) {
        try {
            EObject originalParam = (EObject) operation.getParameters()[0];
            EObject copy = EcoreUtil.copy(originalParam);
            BeanUtils.setProperty(copy, "outputFormat", info.getSourceFormat());
            final Operation sourceOperation =
                    new Operation(
                            operation.getId(),
                            operation.getService(),
                            operation.getMethod(),
                            new Object[] {copy});
            return sourceOperation;
        } catch (Exception e) {
            throw new WFSException(
                    "Failed to create the source operation for XSLT, this is unexpected", e);
        }
    }

    private Response findSourceResponse(Operation sourceOperation, TransformInfo info) {
        for (Response r : responses) {
            if (r.getOutputFormats().contains(info.getSourceFormat())
                    && r.canHandle(sourceOperation)) {
                return r;
            }
        }

        return null;
    }

    private TransformInfo locateTransformation(
            FeatureCollectionResponse collections, Operation operation) throws IOException {
        GetFeatureRequest req = GetFeatureRequest.adapt(operation.getParameters()[0]);
        String outputFormat = req.getOutputFormat();

        // locate the transformation, and make sure it's the same for all feature types
        Set<FeatureType> featureTypes = getFeatureTypes(collections);
        TransformInfo result = null;
        for (FeatureType ft : featureTypes) {
            TransformInfo curr = locateTransform(outputFormat, ft);
            if (curr == null) {
                throw new WFSException(
                        "Could not find a XSLT transformation generating "
                                + outputFormat
                                + " for feature type "
                                + ft.getName(),
                        ServiceException.INVALID_PARAMETER_VALUE,
                        "typeName");
            } else if (result == null) {
                result = curr;
            } else if (!result.equals(curr)) {
                throw new WFSException(
                        "Multiple feature types are mapped to different XLST transformations, cannot proceed: "
                                + result.getXslt()
                                + ", "
                                + curr.getXslt(),
                        ServiceException.INVALID_PARAMETER_VALUE,
                        "typeName");
            }
        }

        return result;
    }

    private TransformInfo locateTransform(String outputFormat, FeatureType ft) throws IOException {
        // first lookup the type specific transforms
        FeatureTypeInfo info = gs.getCatalog().getFeatureTypeByName(ft.getName());
        TransformInfo result = null;
        if (info != null) {
            List<TransformInfo> transforms = repository.getTypeTransforms(info);
            result = filterByOutputFormat(outputFormat, transforms);
        }

        // we don't have a type specific one, look for a global one instead
        if (result == null) {
            List<TransformInfo> transforms = repository.getGlobalTransforms();
            result = filterByOutputFormat(outputFormat, transforms);
        }

        return result;
    }

    private TransformInfo filterByOutputFormat(
            String outputFormat, List<TransformInfo> transforms) {
        for (TransformInfo tx : transforms) {
            if (outputFormat.equals(tx.getOutputFormat())) {
                return tx;
            }
        }

        return null;
    }

    private Set<FeatureType> getFeatureTypes(FeatureCollectionResponse collections) {
        Set<FeatureType> result = new HashSet<FeatureType>();
        for (FeatureCollection<FeatureType, Feature> fc : collections.getFeatures()) {
            result.add(fc.getSchema());
        }

        return result;
    }

    @Override
    public void destroy() throws Exception {
        // get rid of the execution service
        if (executor != null) {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            executor = null;
        }
    }

    @Override
    public List<String> getCapabilitiesElementNames() {
        return getAllCapabilitiesElementNames();
    }
}
