/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.response.v1_0_0;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.xml.v1_1_0.XmlSchemaEncoder;
import org.geoserver.wfsv.VersionedDescribeResults;

/**
 * 
 * @author Andrea Aime
 *
 */
public class VersionedXmlSchemaEncoder extends Response {

    private static String[][] REPLACEMENTS;

    static {
        REPLACEMENTS = new String[4][2];
        REPLACEMENTS[0][0] = "\"http://www.opengis.net/gml\"";
        REPLACEMENTS[0][1] = "\"http://www.opengis.net/wfsv\"";
        REPLACEMENTS[1][0] = "schemas/gml/2.1.2.1/feature.xsd\"";
        REPLACEMENTS[1][1] = "schemas/wfs/1.0.0/WFS-versioning.xsd\"";
        REPLACEMENTS[2][0] = "base=\"gml:AbstractFeatureType\"";
        REPLACEMENTS[2][1] = "base=\"wfsv:AbstractVersionedFeatureType\"";
        REPLACEMENTS[3][0] = "substitutionGroup=\"gml:_Feature\"";
        REPLACEMENTS[3][1] = "substitutionGroup=\"wfsv:_VersionedFeature\"";
    }

    private XmlSchemaEncoder delegate;

    public VersionedXmlSchemaEncoder(XmlSchemaEncoder delegate) {
        super(VersionedDescribeResults.class, delegate.getOutputFormats());
        this.delegate = delegate;
    }

    public String getMimeType(Object value, Operation operation)
            throws org.geoserver.platform.ServiceException {
        return delegate.getMimeType(value, operation);
    }

    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, org.geoserver.platform.ServiceException {
        VersionedDescribeResults results = (VersionedDescribeResults) value;

        if (!results.isVersioned()) {
            delegate.write(results.getFeatureTypeInfo(), output, operation);
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            delegate.write(results.getFeatureTypeInfo(), bos, operation);
            String describe = bos.toString();

            // now let's do the transformation magic, sigh...
            for (int i = 0; i < REPLACEMENTS.length; i++) {
                describe = describe.replaceAll(REPLACEMENTS[i][0],
                        REPLACEMENTS[i][1]);
            }

            // back on the output stream
            output.write(describe.getBytes());
        }
    }

    public boolean canHandle(Operation operation) {
        return "DescribeVersionedFeatureType".equalsIgnoreCase(operation
                .getId())
                && operation.getService().getId().equalsIgnoreCase("wfsv");
    }

}
