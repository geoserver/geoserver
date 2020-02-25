/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geotools.geopkg.wps.GeoPackageProcessRequest;
import org.geotools.geopkg.wps.xml.GPKGConfiguration;
import org.geotools.ows.ServiceException;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Parser;

public class GeoPackageProcessRequestPPIO extends ComplexPPIO {

    Configuration config = new GPKGConfiguration();

    EntityResolverProvider resolverProvider;

    protected GeoPackageProcessRequestPPIO(EntityResolverProvider resolverProvider) {
        super(
                GeoPackageProcessRequest.class,
                GeoPackageProcessRequest.class,
                "text/xml; subtype=geoserver/geopackage");
        this.resolverProvider = resolverProvider;
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        Parser p = new Parser(config);
        p.setEntityResolver(resolverProvider.getEntityResolver());
        p.validate(input);

        if (!p.getValidationErrors().isEmpty()) {
            throw new ServiceException(
                    "Errors were encountered while parsing GeoPackage contents: "
                            + p.getValidationErrors());
        }

        input.reset();
        return p.parse(input);
    }

    @Override
    public Object decode(Object input) throws Exception {
        if (input == null) {
            return null;
        } else if (input instanceof GeoPackageProcessRequest) {
            return input;
        } else if (input instanceof String) {
            return decode(IOUtils.toInputStream((String) input));
        } else {
            throw new IllegalArgumentException(
                    "Cannot convert " + input + " into a GeoPackageProcessRequest object");
        }
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFileExtension() {
        return null;
    }
}
