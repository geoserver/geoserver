/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.geoserver.ows.URLMangler.URLType.RESOURCE;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.DeepUnwrap;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Base class for {@link org.springframework.http.converter.HttpMessageConverter} that encode a HTML document based on a
 * Freemarker template
 *
 * @param <T>
 */
public abstract class AbstractHTMLMessageConverter<T> extends AbstractHttpMessageConverter<T> {
    static final Logger LOGGER = Logging.getLogger(AbstractHTMLMessageConverter.class);
    protected final GeoServer geoServer;
    protected final FreemarkerTemplateSupport templateSupport;

    /**
     * Builds a message converter
     *
     * @param templateSupport A loader used to locate templates
     * @param geoServer The
     */
    public AbstractHTMLMessageConverter(FreemarkerTemplateSupport templateSupport, GeoServer geoServer) {
        super(MediaType.TEXT_HTML);
        this.geoServer = geoServer;
        this.templateSupport = templateSupport;
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("HTML message conveters can only write HTML");
    }

    /**
     * Prepares the model with the usual suspects:
     *
     * <ul>
     *   <li>model: the model object
     *   <li>service: the ServiceInfo object
     *   <li>contact: the contact information
     *   <li>baseURL: the GeoServer baseURL for link construction
     * </ul>
     */
    protected HashMap<String, Object> setupModel(Object value) {
        HashMap<String, Object> model = new HashMap<>();
        model.put("model", value);
        model.put("service", geoServer.getService(getServiceConfigurationClass()));
        model.put("contact", geoServer.getGlobal().getSettings().getContact());
        final String baseURL = getBaseURL();
        model.put("baseURL", baseURL);
        addLinkFunctions(baseURL, model);
        return model;
    }

    /** Returns the class holding the configuration for the service */
    protected abstract Class<? extends ServiceInfo> getServiceConfigurationClass();

    /**
     * Adds the <code>serviceLink</code>, <code>serviceLink</code> and <code>externalLinks</code> functions to the
     * model, for usage in the tempalte
     */
    @SuppressWarnings("unchecked") // TemplateMethodModelEx is not generified
    protected void addLinkFunctions(String baseURL, Map<String, Object> model) {
        model.put("serviceLink", (TemplateMethodModelEx) arguments -> serviceLink(arguments));
        model.put("genericServiceLink", (TemplateMethodModelEx) arguments -> genericServiceLink(arguments));
        model.put(
                "resourceLink", (TemplateMethodModelEx) arguments -> simpleLinkFunction(baseURL, arguments, RESOURCE));
        model.put("htmlExtensions", (TemplateMethodModelEx)
                arguments -> processHtmlExtensions(model, unwrapArguments(arguments)));
        model.put("loadJSON", parseJSON());
    }

    private String simpleLinkFunction(String baseURL, List arguments, URLMangler.URLType urlType) {
        return ResponseUtils.buildURL(baseURL, (String) unwrapArgument(arguments.get(0)), null, urlType);
    }

    /** Builds a service link back to the same service. Used for backlinks. */
    private String serviceLink(List arguments) {
        APIRequestInfo requestInfo = APIRequestInfo.get();
        return ResponseUtils.buildURL(
                requestInfo.getBaseURL(),
                ResponseUtils.appendPath(
                        requestInfo.getServiceLandingPage(), (String) unwrapArgument(arguments.get(0))),
                arguments.size() > 1 ? Collections.singletonMap("f", (String) unwrapArgument(arguments.get(1))) : null,
                URLMangler.URLType.SERVICE);
    }

    /** Builds a service link with the provided path, does not inject the current service path */
    private String genericServiceLink(List arguments) {
        APIRequestInfo requestInfo = APIRequestInfo.get();
        return ResponseUtils.buildURL(
                requestInfo.getBaseURL(),
                (String) unwrapArgument(arguments.get(0)),
                arguments.size() > 1 ? argumentsToKVP(arguments.subList(1, arguments.size())) : null,
                URLMangler.URLType.SERVICE);
    }

    /** Turns a list of keys alternating with values into a map */
    @SuppressWarnings("unchecked")
    private Map<String, String> argumentsToKVP(List kvp) {
        if (kvp.size() % 2 != 0)
            throw new IllegalArgumentException("Arguments beyond the first must be a list of key value pairs");

        List<Object> unwrapped = unwrapArguments(kvp);
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < unwrapped.size(); i += 2) {
            String key = (String) unwrapped.get(i);
            String value = (String) unwrapped.get(i + 1);
            map.put(key, value);
        }

        return map;
    }

    private TemplateMethodModelEx parseJSON() {
        return arguments -> loadJSON(arguments.get(0).toString());
    }

    private String loadJSON(String filePath) {
        try {
            GeoServerDataDirectory geoServerDataDirectory = GeoServerExtensions.bean(GeoServerDataDirectory.class);

            File file = geoServerDataDirectory.findFile(filePath);
            if (file == null) {
                LOGGER.warning("File is outside of data directory");
                throw new RuntimeException("File " + filePath + " is outside of the data directory");
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(file).toString();
        } catch (Exception e) {
            LOGGER.warning("Failed to parse JSON file " + e.getLocalizedMessage());
        }
        LOGGER.warning("Failed to create a JSON object");
        return "Failed to create a JSON object";
    }

    public List<Object> unwrapArguments(List<Object> arguments) {
        if (arguments == null) return null;
        return arguments.stream().map(v -> unwrapArgument(v)).collect(Collectors.toList());
    }

    private Object unwrapArgument(Object v) {
        if (v instanceof TemplateModel model) {
            try {
                return DeepUnwrap.permissiveUnwrap(model);
            } catch (TemplateModelException e) {
                LOGGER.log(Level.WARNING, "", e);
            }
        }
        return v;
    }

    @Override
    public Charset getDefaultCharset() {
        Charset defaultCharset = super.getDefaultCharset();
        if (defaultCharset == null) {
            defaultCharset = Charset.forName(geoServer.getSettings().getCharset());
        }
        return defaultCharset;
    }

    private String processHtmlExtensions(Map<String, Object> model, List arguments) {
        try {
            List<HTMLExtensionCallback> callbacks = GeoServerExtensions.extensions(HTMLExtensionCallback.class);
            StringBuilder sb = new StringBuilder();
            Request dr = Dispatcher.REQUEST.get();
            for (HTMLExtensionCallback callback : callbacks) {
                String html = callback.getExtension(dr, model, getDefaultCharset(), arguments);
                if (html != null) {
                    // add a separation just for output readability
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(html);
                }
            }

            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getBaseURL() {
        APIRequestInfo requestInfo = APIRequestInfo.get();
        if (requestInfo == null) {
            throw new IllegalArgumentException("Cannot extract base URL, APIRequestInfo is not set");
        }
        return requestInfo.getBaseURL();
    }

    /** Purges iterators and other AutoCloseable resources used during template rendering */
    protected void purgeIterators() {
        // handles TemplateFeatureIterator closing FetureIterators
        FreemarkerTemplateSupport.FC_FACTORY.purge();
        // closes other AutoCloseables such as org.geoserver.catalog.util.CloseableIterator
        AutoCloseableTracker.purge();
    }
}
