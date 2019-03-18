/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import net.opengis.wfs.LockFeatureResponseType;
import net.opengis.wfs.LockFeatureType;
import org.eclipse.xsd.XSDSchema;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geotools.util.Version;
import org.geotools.xsd.Encoder;
import org.opengis.filter.identity.FeatureId;

public class LockFeatureTypeResponse extends WFSResponse {

    Catalog catalog;
    WFSConfiguration configuration;

    public LockFeatureTypeResponse(GeoServer gs, WFSConfiguration configuration) {
        super(gs, LockFeatureResponseType.class);

        this.catalog = gs.getCatalog();
        this.configuration = configuration;
    }

    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "text/xml";
    }

    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        WFSInfo wfs = getInfo();

        LockFeatureResponseType lockResponse = (LockFeatureResponseType) value;

        if (new Version("1.1.0").equals(operation.getService().getVersion())) {
            write1_1(lockResponse, output, operation);

            return;
        }

        String indent = wfs.isVerbose() ? "   " : "";
        Charset charset = Charset.forName(wfs.getGeoServer().getSettings().getCharset());
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, charset));

        LockFeatureType lft = (LockFeatureType) operation.getParameters()[0];

        // TODO: get rid of this hardcoding, and make a common utility to get all
        // these namespace imports, as everyone is using them, and changes should
        // go through to all the operations.
        writer.write("<?xml version=\"1.0\" encoding=\"" + charset.name() + "\"?>");
        writer.write("<WFS_LockFeatureResponse " + "\n");
        writer.write(indent + "xmlns=\"http://www.opengis.net/wfs\" " + "\n");
        writer.write(indent + "xmlns:ogc=\"http://www.opengis.net/ogc\" " + "\n");

        writer.write(indent + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + "\n");
        writer.write(indent + "xsi:schemaLocation=\"http://www.opengis.net/wfs ");
        writer.write(buildSchemaURL(lft.getBaseUrl(), "wfs/1.0.0/WFS-transaction.xsd"));
        writer.write("\">" + "\n");

        writer.write(indent + "<LockId>" + lockResponse.getLockId() + "</LockId>" + "\n");

        List featuresLocked = null;

        if (lockResponse.getFeaturesLocked() != null) {
            featuresLocked = lockResponse.getFeaturesLocked().getFeatureId();
        }

        List featuresNotLocked = null;

        if (lockResponse.getFeaturesNotLocked() != null) {
            featuresNotLocked = lockResponse.getFeaturesNotLocked().getFeatureId();
        }

        if ((featuresLocked != null) && !featuresLocked.isEmpty()) {
            writer.write(indent + "<FeaturesLocked>" + "\n");

            for (Iterator i = featuresLocked.iterator(); i.hasNext(); ) {
                writer.write(indent + indent);

                FeatureId featureId = (FeatureId) i.next();
                writer.write("<ogc:FeatureId fid=\"" + featureId + "\"/>" + "\n");
            }

            writer.write(indent + "</FeaturesLocked>" + "\n");
        }

        if ((featuresNotLocked != null) && !featuresNotLocked.isEmpty()) {
            writer.write("<FeaturesNotLocked>" + "\n");

            for (Iterator i = featuresNotLocked.iterator(); i.hasNext(); ) {
                writer.write(indent + indent);

                FeatureId featureId = (FeatureId) i.next();
                writer.write("<ogc:FeatureId fid=\"" + featureId + "\"/>" + "\n");
            }

            writer.write("</FeaturesNotLocked>" + "\n");
        }

        writer.write("</WFS_LockFeatureResponse>");
        writer.flush();
    }

    void write1_1(LockFeatureResponseType lockResponse, OutputStream output, Operation operation)
            throws IOException {
        XSDSchema result;
        try {
            result = configuration.getXSD().getSchema();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Encoder encoder = new Encoder(configuration, result);
        encoder.setEncoding(Charset.forName(getInfo().getGeoServer().getSettings().getCharset()));

        LockFeatureType req = (LockFeatureType) operation.getParameters()[0];

        encoder.setSchemaLocation(
                org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE,
                buildSchemaURL(req.getBaseUrl(), "schemas/wfs/1.1.0/wfs.xsd"));

        encoder.encode(lockResponse, org.geoserver.wfs.xml.v1_1_0.WFS.LOCKFEATURERESPONSE, output);
        output.flush();
    }
}
