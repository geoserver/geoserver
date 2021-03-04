/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.DeepUnwrap;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServer;
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
 * Base class for {@link org.springframework.http.converter.HttpMessageConverter} that encode a HTML
 * document based on a Freemarker template
 *
 * @param <T>
 */
public abstract class AbstractHTMLMessageConverter<T> extends AbstractHttpMessageConverter<T> {
    static final Logger LOGGER = Logging.getLogger(AbstractHTMLMessageConverter.class);
    protected final Class<?> binding;
    protected final GeoServer geoServer;
    protected final FreemarkerTemplateSupport templateSupport;
    protected final Class<? extends ServiceInfo> serviceConfigurationClass;

    /**
     * Builds a message converter
     *
     * @param binding The bean meant to act as the model for the template
     * @param serviceConfigurationClass The class holding the configuration for the service
     * @param templateSupport A loader used to locate templates
     * @param geoServer The
     */
    public AbstractHTMLMessageConverter(
            Class<?> binding,
            Class<? extends ServiceInfo> serviceConfigurationClass,
            FreemarkerTemplateSupport templateSupport,
            GeoServer geoServer) {
        super(MediaType.TEXT_HTML);
        this.binding = binding;
        this.geoServer = geoServer;
        this.serviceConfigurationClass = serviceConfigurationClass;
        this.templateSupport = templateSupport;
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return binding.isAssignableFrom(clazz);
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
        model.put("service", geoServer.getService(serviceConfigurationClass));
        model.put("contact", geoServer.getGlobal().getSettings().getContact());
        final String baseURL = getBaseURL();
        model.put("baseURL", baseURL);
        addLinkFunctions(baseURL, model);
        return model;
    }

    /**
     * Adds the <code>serviceLink</code>, <code>serviceLink</code> and <code>externalLinks</code>
     * functions to the model, for usage in the tempalte
     */
    protected void addLinkFunctions(String baseURL, Map<String, Object> model) {
        model.put(
                "serviceLink",
                (TemplateMethodModel)
                        arguments -> {
                            APIRequestInfo requestInfo = APIRequestInfo.get();
                            return ResponseUtils.buildURL(
                                    requestInfo.getBaseURL(),
                                    ResponseUtils.appendPath(
                                            requestInfo.getServiceLandingPage(),
                                            (String) arguments.get(0)),
                                    arguments.size() > 1
                                            ? Collections.singletonMap(
                                                    "f", arguments.get(1).toString())
                                            : null,
                                    URLMangler.URLType.SERVICE);
                        });
        model.put(
                "resourceLink",
                (TemplateMethodModel)
                        arguments ->
                                ResponseUtils.buildURL(
                                        baseURL,
                                        (String) arguments.get(0),
                                        null,
                                        URLMangler.URLType.RESOURCE));
        model.put(
                "externalLink",
                (TemplateMethodModel)
                        arguments ->
                                ResponseUtils.buildURL(
                                        baseURL,
                                        (String) arguments.get(0),
                                        null,
                                        URLMangler.URLType.EXTERNAL));
        // TemplateMethodModelEx accepts generic object arguments instead of just string arguments
        model.put(
                "htmlExtensions",
                new TemplateMethodModelEx() {
                    @Override
                    public Object exec(List arguments) throws TemplateModelException {
                        if (arguments != null) {
                            arguments = unwrapArguments(arguments);
                        }
                        return processHtmlExtensions(model, arguments);
                    }
                });
    }

    public List<Object> unwrapArguments(List<Object> arguments) {
        return arguments
                .stream()
                .map(
                        v -> {
                            if (v instanceof TemplateModel) {
                                try {
                                    return DeepUnwrap.permissiveUnwrap((TemplateModel) v);
                                } catch (TemplateModelException e) {
                                    LOGGER.log(Level.WARNING, "", e);
                                }
                            }
                            return v;
                        })
                .collect(Collectors.toList());
    }

    private String processHtmlExtensions(Map<String, Object> model, List arguments) {
        try {
            List<HTMLExtensionCallback> callbacks =
                    GeoServerExtensions.extensions(HTMLExtensionCallback.class);
            StringBuilder sb = new StringBuilder();
            Request dr = Dispatcher.REQUEST.get();
            for (HTMLExtensionCallback callback : callbacks) {
                String html = callback.getExtension(dr, model, arguments);
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
            throw new IllegalArgumentException(
                    "Cannot extract base URL, APIRequestInfo is not set");
        }
        return requestInfo.getBaseURL();
    }

    /** Purges iterators that might have been used when walking over GeoTools features */
    protected void purgeIterators() {
        FreemarkerTemplateSupport.FC_FACTORY.purge();
    }
}
