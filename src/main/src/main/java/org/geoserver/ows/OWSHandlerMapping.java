/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import jakarta.servlet.http.HttpServletRequest;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.feature.NameImpl;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.logging.Logging;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Handler mapping for OWS services.
 *
 * <p>This handler mapping extends a set of mappings to allow for a request to specifying a local workspace. Consider
 * the following mappings:
 *
 * <pre>
 *   &lt;property name="mappings"&gt;
 *      &lt;props&gt;
 *              &lt;prop key="/wfs"&gt;dispatcher&lt;/prop&gt;
 *              &lt;prop key="/wfs/*"&gt;dispatcher&lt;/prop&gt;
 *      &lt;/props&gt;
 *    &lt;/property&gt;
 * </pre>
 *
 * <p>This handler will allow the above mappings to match "workspace prefixed" requests such as:
 *
 * <pre>
 *   /topp/wfs?...
 *   /nurc/wfs?...
 * </pre>
 *
 * <p>Where "topp" and "nurc" are workspaces.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class OWSHandlerMapping extends SimpleUrlHandlerMapping {

    static final Logger LOGGER = Logging.getLogger(OWSHandlerMapping.class);

    /**
     * Default handler mapping order for GeoServer's URL mappings.
     *
     * <p>GeoServer's handler mappings need an explicit order lower than zero to take precedence over Spring Boot's
     * auto-configured {@code RequestMappingHandlerMapping}, which defaults to order {@code 0}. In a plain Spring
     * Framework deployment, there is no competing {@code RequestMappingHandlerMapping}, so the default order
     * ({@code Integer.MAX_VALUE}) works fine. In Spring Boot (e.g. GeoServer Cloud), without an explicit order, all
     * GeoServer handler mappings are shadowed by Spring Boot's defaults, which forced the use of {@code @Controller}
     * workarounds to delegate to the GeoServer Dispatcher.
     *
     * <p>This value is also appropriate for {@code SimpleUrlHandlerMapping} beans in XML configuration files that serve
     * static resources (styles, schemas, classpath resources, etc.).
     *
     * <p>{@link org.geoserver.gwc.controller.GwcUrlHandlerMapping} uses a separate, higher-precedence (lower numeric)
     * order value to ensure GWC workspace-prefixed URLs are matched before OWS handler mappings attempt to interpret
     * them.
     *
     * @see org.geoserver.gwc.controller.GwcUrlHandlerMapping#DEFAULT_ORDER
     */
    public static final int DEFAULT_ORDER = -50;

    Catalog catalog;

    public OWSHandlerMapping(Catalog catalog) {
        this.catalog = catalog;
        setOrder(DEFAULT_ORDER);
    }

    @Override
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    protected Object lookupHandler(String urlPath, HttpServletRequest request) throws Exception {
        Object h = super.lookupHandler(urlPath, request);
        if (h == null /*&& AdvancedDispatch.isSet(getApplicationContext())*/) {
            // check for a workspace being specified in the request and strip it off
            int i = urlPath.startsWith("/") ? 1 : 0;
            int j = urlPath.indexOf("/", i);
            if (j > i) {
                String first = urlPath.substring(i, j);
                String last = urlPath.substring(j);

                WorkspaceInfo ws = catalog.getWorkspaceByName(first);
                if ((ws == null) && LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.fine("Could not find workspace " + first + ", trying a layer group lookup");
                }
                if (ws != null) {
                    String wsName = first;
                    // check for a layer being specified as well
                    j = last.indexOf("/", 1);
                    if (j != -1) {
                        first = last.substring(1, j);
                        NamespaceInfo ns = catalog.getNamespaceByPrefix(wsName);
                        if (ns != null) {
                            final boolean layerFound = catalog.getLayerByName(new NameImpl(ns.getURI(), first)) != null;
                            if (!layerFound && LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.fine("Could not find layer " + first + ", trying a layer group lookup");
                            }
                            if (layerFound) {
                                // found, strip off layer and allow call to fall through
                                last = last.substring(j);
                            } else if (catalog.getLayerGroupByName(ws, first) != null) {
                                // found, strip off layer and allow call to fall through
                                last = last.substring(j);
                            } else {
                                LOGGER.fine("Could not find a layer group named " + wsName + ":" + first);
                            }
                        }
                    }

                    h = super.lookupHandler(last, request);
                } else if (catalog.getLayerGroupByName((WorkspaceInfo) null, first) != null) {
                    h = super.lookupHandler(last, request);
                } else {
                    LOGGER.fine("Could not find a layer group named " + first);
                }
            }
        }

        return h;
    }

    @Override
    public String toString() {
        return "OWSHandlerMapping[" + this.getHandlerMap() + "]";
    }

    /**
     * Override to disable pattern parser usage. We do this to match the behavior of Spring 5.x. With pattern parser
     * enabled, Spring will strip path elements and will match only against a portion of the path, e.g., <code>/wms/**
     * </code> will not really match <code>wms/reflect</code> because it's trying to match <code>reflect</code> only,
     * rather than <code>wms/reflect</code>.
     *
     * @return <code>null</code> to disable pattern parser usage
     */
    @Override
    public PathPatternParser getPatternParser() {
        return null;
    }
}
