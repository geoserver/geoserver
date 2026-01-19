/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.xsd.XSDSchema;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geotools.gml3.GMLConfiguration;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Encoder;

public class GML3OutputFormat extends BaseGML3OutputFormat {

    private WFSConfiguration configuration;

    public GML3OutputFormat(GeoServer geoServer, WFSConfiguration configuration) {
        this(new HashSet<>(Arrays.asList("gml3", "text/xml; subtype=gml/3.1.1")), geoServer, configuration);
    }

    public GML3OutputFormat(Set<String> outputFormats, GeoServer geoServer, WFSConfiguration configuration) {
        super(outputFormats, geoServer);
        this.configuration = configuration;
    }

    @Override
    protected void updateConfiguration(
            Configuration configuration,
            int numDecimals,
            boolean padWithZeros,
            boolean forcedDecimal,
            boolean encodeMeasures) {
        GMLConfiguration gml31 = configuration.getDependency(GMLConfiguration.class);
        if (gml31 != null) {
            gml31.setNumDecimals(numDecimals);
            gml31.setPadWithZeros(padWithZeros);
            gml31.setForceDecimalEncoding(forcedDecimal);
            gml31.setEncodeMeasures(encodeMeasures);
        }
    }

    @Override
    protected Configuration createConfiguration(Map<String, Set<ResourceInfo>> resources, Object request) {
        WFSInfo wfs = getInfo();
        // set up the srsname syntax (calls version-specific method if supported)
        configuration.setSrsSyntax(
                wfs.getGML().get(WFSInfo.Version.V_11).getSrsNameStyle().toSrsSyntax());
        return configuration;
    }

    @Override
    protected Encoder createEncoder(
            Configuration configuration, Map<String, Set<ResourceInfo>> resources, Object request) {
        // reuse the WFS configuration feature builder, otherwise build a new one
        FeatureTypeSchemaBuilder schemaBuilder;
        if (configuration instanceof WFSConfiguration sConfiguration) {
            schemaBuilder = sConfiguration.getSchemaBuilder();
        } else {
            schemaBuilder = new FeatureTypeSchemaBuilder.GML3(geoServer);
        }
        // create this request specific schema
        ApplicationSchemaXSD1 schema = new ApplicationSchemaXSD1(schemaBuilder);
        schema.setBaseURL(GetFeatureRequest.adapt(request).getBaseURL());
        schema.setResources(resources);
        if (schema.getFeatureTypes().isEmpty()) {
            // no feature types so let's use the base WFS schema
            XSDSchema result;
            try {
                result = configuration.getXSD().getSchema();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return new Encoder(configuration, result);
        }
        try {
            // let's just instantiate the encoder
            return new Encoder(configuration, schema.getSchema());
        } catch (IOException exception) {
            throw new RuntimeException("Error generating the XSD schema during the encoder instantiation.", exception);
        }
    }
}
