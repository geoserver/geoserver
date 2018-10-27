/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.xml.v1_0_0;

import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.wps.WPSException;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.geotools.wps.WPSConfiguration;
import org.geotools.xsd.Parser;

/**
 * WPS XML parser
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public class WpsXmlReader extends XmlRequestReader {
    public Logger LOGGER = Logging.getLogger("org.geoserver.wps");

    private WPSConfiguration configuration;

    private EntityResolverProvider resolverProvider;

    public WpsXmlReader(
            String element,
            String version,
            WPSConfiguration configuration,
            EntityResolverProvider resolverProvider) {
        super(new QName(org.geotools.wps.WPS.NAMESPACE, element), new Version("1.0.0"), "wps");
        this.configuration = configuration;
        this.resolverProvider = resolverProvider;
    }

    @SuppressWarnings("unchecked")
    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        Parser parser = new Parser(configuration);
        parser.setValidating(true);
        parser.setEntityResolver(resolverProvider.getEntityResolver());

        Object parsed;
        try {
            parsed = parser.parse(reader);
        } catch (Exception e) {
            throw new WPSException("Could not parse XML request.", e);
        }

        if (!parser.getValidationErrors().isEmpty()) {
            WPSException exception = new WPSException("Invalid request", "InvalidParameterValue");

            for (Exception error : (List<Exception>) parser.getValidationErrors()) {
                LOGGER.warning(error.getLocalizedMessage());
                exception.getExceptionText().add(error.getLocalizedMessage());
            }
        }

        return parsed;
    }
}
