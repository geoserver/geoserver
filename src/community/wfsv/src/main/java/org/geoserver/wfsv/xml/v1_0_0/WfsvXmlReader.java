/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.xml.v1_0_0;

import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.geotools.util.Version;
import org.geotools.xml.Parser;
import org.xml.sax.InputSource;

/**
 * (JD) TODO: This class copies too much code from teh normal XML reader, we
 * need to find a way to share code between them. Be it through subclassing or a
 * utility class.
 */
public class WfsvXmlReader extends XmlRequestReader {
    private WFSInfo wfs;

    private WFSVConfiguration configuration;

    public WfsvXmlReader(String element, GeoServer gs, WFSVConfiguration configuration) {
        super(new QName(org.geoserver.wfsv.xml.v1_1_0.WFSV.NAMESPACE, element),
                new Version("1.0.0"), "wfsv");
        this.wfs = gs.getService( WFSInfo.class );
        this.configuration = configuration;
    }

    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        // check the strict flag to determine if we should validate or not
        Boolean strict = (Boolean) kvp.get("strict");
        if ( strict == null ) {
            strict = Boolean.FALSE;
        } else if(wfs.isCiteCompliant()) {
            strict = Boolean.TRUE;
        }

        Parser parser = new Parser(configuration);
        parser.setValidating(strict);

        // "inject" namespace mappings
        List<NamespaceInfo> namespaces = configuration.getCatalog().getNamespaces();
        for (NamespaceInfo ns : namespaces) {
            if (ns.equals( configuration.getCatalog().getDefaultNamespace()))
                continue;

            parser.getNamespaces().declarePrefix(ns.getPrefix(), ns.getURI());
        }

        // set the input source with the correct encoding
        InputSource source = new InputSource(reader);
        source.setEncoding(wfs.getGeoServer().getGlobal().getCharset());

        Object parsed = parser.parse(source);

        // valid request? this should definitley be a configuration option
        // TODO: HACK, disabling validation for transaction
        if (!"Transaction".equalsIgnoreCase(getElement().getLocalPart())) {
            if (!parser.getValidationErrors().isEmpty()) {
                WFSException exception = new WFSException("Invalid request",
                        "InvalidParameterValue");

                for (Iterator e = parser.getValidationErrors().iterator(); e.hasNext();) {
                    Exception error = (Exception) e.next();
                    exception.getExceptionText().add(error.getLocalizedMessage());
                }

                throw exception;
            }
        }

        return parsed;
    }
}
