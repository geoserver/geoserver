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

    @Override
    public final T load(GeoServer gs) throws Exception {
        return load(gs, resourceLoader.get(""));
    }

    public final T load(GeoServer gs, Resource directory) throws Exception {
        // look for file matching classname
        Resource file;

        if (Resources.exists(file = directory.get(getFilename()))) {
            // xstream it in
            try (BufferedInputStream in = new BufferedInputStream(file.in())) {
                XStreamPersister xp = getXstreamPersister(gs);
                return initialize(xp.load(in, getServiceClass()));
            }
        } else {
            // create an 'empty' object
            T service = createServiceFromScratch(gs);
            return initialize(service);
        }
    }

    private volatile GeoServer geoserver;
    private volatile XStreamPersister persister;

    /**
     * Creates and initializes a new {@link XStreamPersister} only if it wasn't created before or it's been called for a
     * different {@link GeoServer} instance, and then caches it as an instance variable; thus avoiding the overhead on
     * each call to load(), which can be significant when the number of services is large (e.g. 3 seconds instead of 35
     * seconds to load about 17k service files)
     */
    private XStreamPersister getXstreamPersister(GeoServer gs) {
        if (this.geoserver != gs) {
            this.geoserver = gs;
            this.persister = xpf.createXMLPersister();
            initXStreamPersister(persister, gs);
        }
        return persister;
    }

    /** Fills in all the bits that are normally not loaded automatically by XStream, such as empty collections */
    public void initializeService(T info) {
        initialize(info);
    }

    /**
     * Fills in the blanks of the service object loaded by XStream. This implementation makes sure all collections in
     * {@link ServiceInfoImpl} are initialized, subclasses should override to add more specific initializations (such as
     * the actual supported versions and so on)
     */
    protected T initialize(T service) {
        if (service instanceof ServiceInfoImpl impl) {
            if (impl.getClientProperties() == null) {
                impl.setClientProperties(new HashMap<>());
            }
            if (impl.getExceptionFormats() == null) {
                impl.setExceptionFormats(new ArrayList<>());
            }
            if (impl.getKeywords() == null) {
                impl.setKeywords(new ArrayList<>());
            }
            if (impl.getMetadata() == null) {
                impl.setMetadata(new MetadataMap());
            }
            if (impl.getVersions() == null) {
                impl.setVersions(new ArrayList<>());
            }
            if (impl.getName() == null) {
                impl.setName(impl.getType());
            }
        }

        return service;
    }

    @Override
    public final void save(T service, GeoServer gs) throws Exception {}

    public final void save(T service, GeoServer gs, Resource directory) throws Exception {
        String filename = getFilename();
        Resource resource = directory == null ? resourceLoader.get(filename) : directory.get(filename);

        // using resource output stream makes sure we write on a temp file and them move
        try (OutputStream out = resource.out()) {
            XStreamPersister xp = getXstreamPersister(gs);
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

    @Override
    public final T create(GeoServer gs) {
        return createServiceFromScratch(gs);
    }

    protected abstract T createServiceFromScratch(GeoServer gs);
}
