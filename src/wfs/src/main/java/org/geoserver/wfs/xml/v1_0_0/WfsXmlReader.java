/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import java.io.Reader;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.wfs.CatalogNamespaceSupport;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.xml.WFSURIHandler;
import org.geoserver.wfs.xml.WFSXmlUtils;
import org.geotools.util.Version;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Parser;

/**
 * Xml reader for wfs 1.0.0 xml requests.
 *
 * @author Justin Deoliveira, The Open Planning Project
 *     <p>TODO: there is too much duplication with the 1.1.0 reader, factor it out.
 */
public class WfsXmlReader extends XmlRequestReader {

    /** Xml Configuration */
    Configuration configuration;
    /** geoserver configuration */
    GeoServer geoServer;

    EntityResolverProvider entityResolverProvider;

    public WfsXmlReader(String element, Configuration configuration, GeoServer geoServer) {
        this(element, configuration, geoServer, "wfs");
    }

    protected WfsXmlReader(
            String element, Configuration configuration, GeoServer geoServer, String serviceId) {
        super(new QName(WFS.NAMESPACE, element), new Version("1.0.0"), serviceId);
        this.configuration = configuration;
        this.geoServer = geoServer;
        this.entityResolverProvider = new EntityResolverProvider(geoServer);
    }

    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        // TODO: refactor this method to use WFSXmlUtils
        Catalog catalog = geoServer.getCatalog();

        // check the strict flag to determine if we should validate or not
        Boolean strict = (Boolean) kvp.get("strict");
        if (strict == null) {
            strict = Boolean.FALSE;
        }

        // create the parser instance
        Parser parser = new Parser(configuration);
        parser.setEntityResolver(entityResolverProvider.getEntityResolver());

        // "inject" namespace mappings
        parser.getNamespaces().add(new CatalogNamespaceSupport(catalog));

        // set validation based on strict or not
        parser.setValidating(strict.booleanValue());
        WFSURIHandler.addToParser(geoServer, parser);
        // set entity expansion limit
        parser.setEntityExpansionLimit(WFSXmlUtils.getEntityExpansionLimitConfiguration());

        // parse
        Object parsed = parser.parse(reader);

        // if strict was set, check for validation errors and throw an exception
        if (strict.booleanValue() && !parser.getValidationErrors().isEmpty()) {
            WFSException exception = new WFSException("Invalid request", "InvalidParameterValue");

            for (Iterator e = parser.getValidationErrors().iterator(); e.hasNext(); ) {
                Exception error = (Exception) e.next();
                exception.getExceptionText().add(error.getLocalizedMessage());
            }

            throw exception;
        }

        return parsed;
    }
}
