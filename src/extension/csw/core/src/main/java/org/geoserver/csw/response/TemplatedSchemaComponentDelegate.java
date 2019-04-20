/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import static org.geoserver.ows.util.ResponseUtils.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;
import net.opengis.cat.csw20.DescribeRecordType;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.CSWInfo;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;

/**
 * An implementation of {@link SchemaComponentDelegate} using a fixed file in the classpath to build
 * the SchemaComponent representation, with a simple templating mechanism for the root of the schema
 * locations (local vs schemas.opengis.net)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class TemplatedSchemaComponentDelegate implements SchemaComponentDelegate {

    Name typeName;

    String schemaPath;

    GeoServer gs;

    public TemplatedSchemaComponentDelegate(
            GeoServer gs, String namespaceURI, String name, String schemaPath) {
        this.gs = gs;
        this.typeName = new NameImpl(namespaceURI, name);
        this.schemaPath = schemaPath;
    }

    @Override
    public boolean canHandle(AttributeDescriptor descriptor) {
        return typeName.equals(descriptor.getName());
    }

    @Override
    public void writeSchemaComponent(
            DescribeRecordType request, Writer bw, AttributeDescriptor descriptor)
            throws IOException {
        if (!canHandle(descriptor)) {
            throw new IllegalArgumentException("Cannot handle schema " + descriptor.getName());
        }

        // FeatureType schema = (FeatureType) descriptor.getType();

        // find the root of the schema location
        CSWInfo csw = gs.getService(CSWInfo.class);
        String schemaLocationRoot;
        if (csw.isCanonicalSchemaLocation()) {
            schemaLocationRoot = "http://schemas.opengis.net";
        } else {
            schemaLocationRoot = buildSchemaURL(request.getBaseUrl(), "");
            // remove the trailing /
            schemaLocationRoot = schemaLocationRoot.substring(0, schemaLocationRoot.length() - 1);
        }

        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                getClass().getResourceAsStream(schemaPath),
                                Charset.forName("UTF-8")))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replace("%SCHEMAS_ROOT%", schemaLocationRoot);
                bw.write(line);
                bw.write("\n");
            }
        }
    }
}
