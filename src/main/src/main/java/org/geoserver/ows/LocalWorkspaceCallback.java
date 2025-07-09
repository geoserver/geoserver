/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.NameImpl;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.logging.Logging;

/**
 * Dispatcher callback that sets and clears the {@link LocalWorkspace} and {@link LocalPublished} thread locals.
 *
 * <p>This class is responsible for parsing request context information ({@code workspace/ows?},
 * {@code layergroup/wms?}, {@code workspace/layer}) and resolving the intended local workspace, layer group, and/or
 * layer information which are stored in a thread locale for reference.
 *
 * <p>The logic used here is duplicated by GeoServerHomePage which provides a user interface reflecting this context
 * lookup process.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class LocalWorkspaceCallback implements DispatcherCallback, ExtensionPriority {

    static final Logger LOGGER = Logging.getLogger(LocalWorkspaceCallback.class);

    GeoServer gs;

    public LocalWorkspaceCallback(GeoServer gs) {
        this.gs = gs;
    }

    @Override
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public Request init(Request request) {
        WorkspaceInfo ws = null;
        LayerGroupInfo lg = null;
        if (request.context != null) {
            String first = request.context;
            String last = null;

            int slash = first.indexOf('/');
            if (slash > -1) {
                last = first.substring(slash + 1);
                first = first.substring(0, slash);
            }

            // check if the context matches a workspace
            Catalog catalog = gs.getCatalog();
            ws = catalog.getWorkspaceByName(first);
            if (ws != null) {
                LocalWorkspace.set(ws);

                // set the local layer if it exists
                if (last != null) {
                    // hack up a qualified name
                    NamespaceInfo ns = catalog.getNamespaceByPrefix(ws.getName());
                    if (ns != null) {
                        // can have extra bits, like ws/layer/gwc/service
                        int slashInLayer = last.indexOf('/');
                        if (slashInLayer != -1) {
                            last = last.substring(0, slashInLayer);
                        }

                        LayerInfo l = catalog.getLayerByName(new NameImpl(ns.getURI(), last));
                        if (l != null) {
                            LocalPublished.set(l);
                        } else {
                            LOGGER.log(Level.FINE, "Could not lookup context {0} as a layer, trying as group", first);
                            lg = catalog.getLayerGroupByName(ws, last);
                            if (lg != null) {
                                LocalPublished.set(lg);
                            } else {
                                // TODO: perhaps throw an exception?
                                LOGGER.log(Level.FINE, "Could not lookup context {0} as a group either", first);
                            }
                        }
                    }
                }
            } else {
                LOGGER.log(Level.FINE, "Could not lookup context {0} as a workspace, trying as group", first);
                lg = catalog.getLayerGroupByName((WorkspaceInfo) null, first);
                if (lg != null) {
                    LocalPublished.set(lg);
                } else {
                    LOGGER.log(Level.FINE, "Could not lookup context {0} as a layer group either", first);
                }
            }
            if (ws == null && lg == null) {
                // if no workspace context specified and server configuration not allowing global
                // services throw an error
                if (!gs.getGlobal().isGlobalServices()) {
                    throw new ServiceException(
                            "Could not determine workspace or workspace/layer-or-layergroup from request: '"
                                    + request.context
                                    + "', and global services are off.");
                }
            }
        } else if (!gs.getGlobal().isGlobalServices()) {
            throw new ServiceException("No workspace specified");
        }

        return request;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        return null;
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        return null;
    }

    @Override
    public Response responseDispatched(Request request, Operation operation, Object result, Response response) {
        return null;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        return null;
    }

    @Override
    public void finished(Request request) {
        LocalWorkspace.remove();
        LocalPublished.remove();
    }

    @Override
    public int getPriority() {
        return HIGHEST;
    }
}
