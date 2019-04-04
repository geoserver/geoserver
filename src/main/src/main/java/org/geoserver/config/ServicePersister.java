/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

public class ServicePersister extends ConfigurationListenerAdapter {

    static Logger LOGGER = Logging.getLogger("org.geoserver");

    List<XStreamServiceLoader> loaders;
    GeoServer geoServer;
    GeoServerResourceLoader resourceLoader;

    public ServicePersister(List<XStreamServiceLoader> loaders, GeoServer geoServer) {
        this.loaders = loaders;
        this.geoServer = geoServer;
        this.resourceLoader = geoServer.getCatalog().getResourceLoader();
    }

    @Override
    public void handleServiceChange(
            ServiceInfo service,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {

        XStreamServiceLoader loader = findServiceLoader(service);

        // handle the case of a service changing workspace and move the file
        int i = propertyNames.indexOf("workspace");
        if (i != -1) {
            // TODO: share code with GeoServerPersister
            WorkspaceInfo old = (WorkspaceInfo) oldValues.get(i);
            if (old != null) {
                WorkspaceInfo ws = (WorkspaceInfo) newValues.get(i);
                Resource f;
                try {
                    f = dir(ws).get(loader.getFilename());
                    f.renameTo(dir(ws).get(loader.getFilename()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void handlePostServiceChange(ServiceInfo service) {
        XStreamServiceLoader loader = findServiceLoader(service);

        try {
            // TODO: handle workspace move, factor this class out into
            // separate persister class
            Resource directory =
                    service.getWorkspace() != null ? dir(service.getWorkspace()) : null;
            loader.save(service, geoServer, directory);
        } catch (Throwable t) {
            throw new RuntimeException(t);
            // LOGGER.log(Level.SEVERE, "Error occurred while saving configuration", t);
        }
    }

    public void handleServiceRemove(ServiceInfo service) {
        XStreamServiceLoader loader = findServiceLoader(service);
        try {
            Resource dir =
                    service.getWorkspace() != null
                            ? dir(service.getWorkspace())
                            : resourceLoader.get(Paths.BASE);
            dir.get(loader.getFilename()).delete();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    XStreamServiceLoader findServiceLoader(ServiceInfo service) {
        XStreamServiceLoader loader = null;
        for (XStreamServiceLoader<ServiceInfo> l : loaders) {
            if (l.getServiceClass().isInstance(service)) {
                loader = l;
                break;
            }
        }

        if (loader == null) {
            throw new IllegalArgumentException("No loader for " + service.getName());
        }
        return loader;
    }

    Resource dir(WorkspaceInfo ws) throws IOException {
        return resourceLoader.get(Paths.path("workspaces", ws.getName()));
    }
}
