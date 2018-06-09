/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import static org.geoserver.ows.util.ResponseUtils.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import net.opengis.cat.csw20.DescribeRecordType;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.CSWInfo;
import org.geoserver.ows.Response;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * Encodes the DescribeRecord response
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DescribeRecordResponse extends Response {

    private GeoServer gs;

    public DescribeRecordResponse(GeoServer gs) {
        super(AttributeDescriptor[].class, "application/xml");
        this.gs = gs;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "application/xml";
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        AttributeDescriptor[] descriptors = (AttributeDescriptor[]) value;

        Writer writer = new OutputStreamWriter(output, Charset.forName("UTF-8"));

        // find the root of the schema location
        DescribeRecordType request = (DescribeRecordType) operation.getParameters()[0];
        CSWInfo csw = gs.getService(CSWInfo.class);
        String schemaLocationRoot;
        if (csw.isCanonicalSchemaLocation()) {
            schemaLocationRoot = "http://schemas.opengis.net/csw/2.0.2";
        } else {
            schemaLocationRoot = buildSchemaURL(request.getBaseUrl(), "csw/2.0.2");
        }

        // write out the container
        writer.write(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<csw:DescribeRecordResponse xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:schemaLocation=\"http://www.opengis.net/cat/csw/2.0.2 "
                        + schemaLocationRoot
                        + "/CSW-discovery.xsd\">\n");

        List<SchemaComponentDelegate> delegates =
                GeoServerExtensions.extensions(SchemaComponentDelegate.class);

        // write out all the schemas
        for (AttributeDescriptor descriptor : descriptors) {
            for (SchemaComponentDelegate delegate : delegates) {
                if (delegate.canHandle(descriptor)) {
                    delegate.writeSchemaComponent(request, writer, descriptor);
                    break;
                }
            }
        }

        // write out the container close up
        writer.write("</csw:DescribeRecordResponse>");
        writer.flush();
    }
}
