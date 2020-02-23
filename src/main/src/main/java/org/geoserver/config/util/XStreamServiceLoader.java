/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.ServiceLoader;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;

/**
 * Service loader which loads and saves a service configuration with xstream.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public abstract class XStreamServiceLoader<T extends ServiceInfo> implements ServiceLoader<T> {

    GeoServerResourceLoader resourceLoader;
    String filenameBase;
    XStreamPersisterFactory xpf = new XStreamPersisterFactory();

    public XStreamServiceLoader(GeoServerResourceLoader resourceLoader, String filenameBase) {
        this.resourceLoader = resourceLoader;
        this.filenameBase = filenameBase;
    }

    public String getFilename() {
        return filenameBase + ".xml";
    }

    public void setXStreamPeristerFactory(XStreamPersisterFactory xpf) {
        this.xpf = xpf;
    }

    public final T load(GeoServer gs) throws Exception {
        return load(gs, resourceLoader.get(""));
    }

    public final T load(GeoServer gs, Resource directory) throws Exception {
        // look for file matching classname
        Resource file;

        if (Resources.exists(file = directory.get(getFilename()))) {
            // xstream it in
            try (BufferedInputStream in = new BufferedInputStream(file.in())) {
                XStreamPersister xp = xpf.createXMLPersister();
                initXStreamPersister(xp, gs);
                return initialize(xp.load(in, getServiceClass()));
            }
        } else {
            // create an 'empty' object
            ServiceInfo service = createServiceFromScratch(gs);
            return initialize((T) service);
        }
    }

    /**
     * Fills in all the bits that are normally not loaded automatically by XStream, such as empty
     * collections
     */
    public void initializeService(ServiceInfo info) {
        initialize((T) info);
    }

    /**
     * Fills in the blanks of the service object loaded by XStream. This implementation makes sure
     * all collections in {@link ServiceInfoImpl} are initialized, subclasses should override to add
     * more specific initializations (such as the actual supported versions and so on)
     */
    protected T initialize(T service) {
        if (service instanceof ServiceInfoImpl) {
            // initialize all collections to
            ServiceInfoImpl impl = (ServiceInfoImpl) service;
            if (impl.getClientProperties() == null) {
                impl.setClientProperties(new HashMap());
            }
            if (impl.getExceptionFormats() == null) {
                impl.setExceptionFormats(new ArrayList());
            }
            if (impl.getKeywords() == null) {
                impl.setKeywords(new ArrayList());
            }
            if (impl.getMetadata() == null) {
                impl.setMetadata(new MetadataMap());
            }
            if (impl.getVersions() == null) {
                impl.setVersions(new ArrayList());
            }
        }

        return service;
    }

    public final void save(T service, GeoServer gs) throws Exception {}

    public final void save(T service, GeoServer gs, Resource directory) throws Exception {
        String filename = getFilename();
        Resource resource =
                directory == null ? resourceLoader.get(filename) : directory.get(filename);

        // using resource output stream makes sure we write on a temp file and them move
        try (OutputStream out = resource.out()) {
            XStreamPersister xp = xpf.createXMLPersister();
            initXStreamPersister(xp, gs);
            xp.save(service, out);
        }
    }

    /**
     * Hook for subclasses to configure the xstream.
     *
     * <p>The most common use is to do some aliasing or omit some fields.
     */
    protected void initXStreamPersister(XStreamPersister xp, GeoServer gs) {
        xp.setGeoServer(gs);
        xp.setCatalog(gs.getCatalog());
        xp.getXStream().alias(filenameBase, getServiceClass());
    }

    public final T create(GeoServer gs) {
        return createServiceFromScratch(gs);
    }

    protected abstract T createServiceFromScratch(GeoServer gs);
}
