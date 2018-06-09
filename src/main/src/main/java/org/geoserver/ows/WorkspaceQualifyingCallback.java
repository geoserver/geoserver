/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

/**
 * A dispatcher callback used to "qualify" requests based on the presence of {@link LocalWorkspace}
 * and {@link LocalPublished}.
 *
 * <p>The term "qualifying" in this sense means fill in any information that can be derived from the
 * the local workspace or layer. For example, if a client specifies a local workspace then they
 * should not have to namespace qualify every layer or feature type name. A subclass of this
 * callback can do that automatically.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class WorkspaceQualifyingCallback implements DispatcherCallback {

    protected Catalog catalog;

    protected WorkspaceQualifyingCallback(Catalog catalog) {
        this.catalog = catalog;
    }

    public Request init(Request request) {
        return null;
    }

    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        if (LocalWorkspace.get() != null) {
            qualifyRequest(LocalWorkspace.get(), LocalPublished.get(), service, request);
        }
        return service;
    }

    public Operation operationDispatched(Request request, Operation operation) {
        if (LocalWorkspace.get() != null) {
            qualifyRequest(LocalWorkspace.get(), LocalPublished.get(), operation, request);
        }

        return operation;
    }

    public Object operationExecuted(Request request, Operation operation, Object result) {
        return null;
    }

    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        return null;
    }

    public void finished(Request request) {}

    protected <T> T parameter(Operation op, Class<T> clazz) {
        return (T) OwsUtils.parameter(op.getParameters(), clazz);
    }

    protected abstract void qualifyRequest(
            WorkspaceInfo workspace, PublishedInfo layer, Service service, Request request);

    protected abstract void qualifyRequest(
            WorkspaceInfo workspace, PublishedInfo layer, Operation operation, Request request);

    protected String qualifyName(String name, WorkspaceInfo ws) {

        int colon = name.indexOf(':');
        if (colon == -1) {
            name = ws.getName() + ":" + name;
        } else {
            String prefix = name.substring(0, colon);
            if (!prefix.equalsIgnoreCase(ws.getName())) {
                name = ws.getName() + ":" + name.substring(colon + 1);
            }
        }
        return name;
    }

    protected void qualifyLayerNames(List<String> names, WorkspaceInfo ws) {
        for (int i = 0; i < names.size(); i++) {
            names.set(i, qualifyName(names.get(i), ws));
        }
    }
}
