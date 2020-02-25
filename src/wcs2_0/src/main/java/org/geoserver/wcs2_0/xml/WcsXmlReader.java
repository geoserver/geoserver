/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.xml;

import java.io.Reader;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.util.Version;
import org.geotools.wcs.v2_0.WCS;
import org.geotools.wcs.v2_0.WCSConfiguration;
import org.geotools.xsd.Parser;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Xml reader for wcs 2.0.1 xml requests.
 *
 * @author Andrea Aime, GeoSolutions
 */
public class WcsXmlReader extends XmlRequestReader {
    WCSConfiguration configuration;

    private EntityResolverProvider resolverProvider;

    public WcsXmlReader(
            String element,
            String version,
            WCSConfiguration configuration,
            EntityResolverProvider resolverProvider) {
        super(new QName(WCS.NAMESPACE, element), new Version(version), "wcs");
        this.configuration = configuration;
        this.resolverProvider = resolverProvider;
    }

    @SuppressWarnings("rawtypes")
    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        // create the parser instance
        Parser parser = new Parser(configuration);
        parser.setEntityResolver(resolverProvider.getEntityResolver());

        // uncomment this once we have a working validator (now it fails due to
        // xlink issues)
        //        parser.setValidating(true);
        //        parser.setFailOnValidationError(true);
        //        parser.setStrict(true);

        // parse
        Object parsed;
        try {
            parsed = parser.parse(reader);
        } catch (Exception e) {
            throw new WcsException(
                    "Parsing failed, the xml request is most probably not compliant to the wcs 2.0.1 schema",
                    e);
        }

        return parsed;
    }
}
