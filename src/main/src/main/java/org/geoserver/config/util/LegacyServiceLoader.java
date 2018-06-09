/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.ServiceLoader;

/**
 * Base class for service loaders loading from the legacy service.xml file.
 *
 * <p>
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public abstract class LegacyServiceLoader<T extends ServiceInfo> implements ServiceLoader<T> {

    /** reader pointing to services.xml */
    LegacyServicesReader reader;

    /**
     * Sets the legacy services.xml reader.
     *
     * <p>This method is called by the GeoServer startup, it should not be called by client code.
     */
    public void setReader(LegacyServicesReader reader) {
        this.reader = reader;
    }

    /**
     * Loads the service.
     *
     * <p>This method calls through to {@link #load(LegacyServicesReader, GeoServer)}
     */
    public final T load(GeoServer gs) throws Exception {
        return load(reader, gs);
    }

    /**
     * Creates the service configuration object.
     *
     * <p>Subclasses implementing this method can use the {@link #readCommon(ServiceInfo, Map,
     * GeoServer)} method to read those attributes common to all services.
     *
     * @param reader The services.xml reader.
     */
    public abstract T load(LegacyServicesReader reader, GeoServer geoServer) throws Exception;

    /**
     * Reads all the common attributes from the service info class.
     *
     * <p>This method is intended to be called by subclasses after creating an instance of
     * ServiceInfo. Example:
     *
     * <pre>
     *   // read properties
     *   Map<String,Object> props = reader.wfs();
     *
     *   // create config object
     *   WFSInfo wfs = new WFSInfoImpl();
     *
     *   //load common properties
     *   load( wfs, reader );
     *
     *   //load wfs specific properties
     *   wfs.setServiceLevel( map.get( "serviceLevel") );
     *   ...
     * </pre>
     */
    protected void readCommon(ServiceInfo service, Map<String, Object> properties, GeoServer gs)
            throws Exception {

        service.setEnabled((Boolean) properties.get("enabled"));
        service.setName((String) properties.get("name"));
        service.setTitle((String) properties.get("title"));
        service.setAbstract((String) properties.get("abstract"));

        Map metadataLink = (Map) properties.get("metadataLink");
        if (metadataLink != null) {
            MetadataLinkInfo ml = gs.getCatalog().getFactory().createMetadataLink();
            ml.setAbout((String) metadataLink.get("about"));
            ml.setMetadataType((String) metadataLink.get("metadataType"));
            ml.setType((String) metadataLink.get("type"));
            service.setMetadataLink(ml);
        }

        List<String> keywords = (List<String>) properties.get("keywords");
        if (keywords != null) {
            for (String kw : keywords) {
                service.getKeywords().add(new Keyword(kw));
            }
        }

        service.setOnlineResource((String) properties.get("onlineResource"));
        service.setFees((String) properties.get("fees"));
        service.setAccessConstraints((String) properties.get("accessConstraints"));
        service.setCiteCompliant((Boolean) properties.get("citeConformanceHacks"));
        service.setMaintainer((String) properties.get("maintainer"));
        service.setSchemaBaseURL((String) properties.get("SchemaBaseUrl"));
    }

    public void save(T service, GeoServer gs) throws Exception {
        // do nothing, saving implemented elsewhere
    }

    @Override
    public T create(GeoServer gs) throws Exception {
        throw new UnsupportedOperationException("Use xstream loader equivalent instead");
    }
}
