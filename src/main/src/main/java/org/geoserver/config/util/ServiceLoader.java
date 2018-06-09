/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import java.util.List;
import java.util.Map;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;

/**
 * Extension point for loading services from the services.xml file.
 *
 * <p>Instances of this class are registered in a spring context:
 *
 * <pre>
 * &lt;bean id="org.geoserver.wfs.WFSLoader"/>
 * </pre>
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public abstract class ServiceLoader {

    /**
     * Creates the service configuration object.
     *
     * @param reader The services.xml reader.
     */
    public abstract ServiceInfo load(LegacyServicesReader reader, GeoServer geoServer)
            throws Exception;

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
    protected void load(ServiceInfo service, Map<String, Object> properties, GeoServer gs)
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

        List keywords = (List) properties.get("keywords");
        if (keywords != null) {
            service.getKeywords().addAll(keywords);
        }

        service.setOnlineResource((String) properties.get("onlineResource"));
        service.setFees((String) properties.get("fees"));
        service.setAccessConstraints((String) properties.get("accessConstraints"));
        service.setCiteCompliant((Boolean) properties.get("citeConformanceHacks"));
        service.setMaintainer((String) properties.get("maintainer"));
        service.setSchemaBaseURL((String) properties.get("SchemaBaseUrl"));
    }
}
