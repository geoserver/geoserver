package org.geoserver.wfsv.response.v1_1_0;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

import net.opengis.wfs.DescribeFeatureTypeType;

import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.util.XSDResourceImpl;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfsv.VersionedDescribeResults;
import org.geoserver.wfsv.xml.v1_1_0.WFSVConfiguration;

public class VersionedXmlSchemaEncoder extends Response {
    /** wfs configuration */
    WFSInfo wfs;

    /** the catalog */
    Catalog catalog;

    WFSVConfiguration configuration;

    public VersionedXmlSchemaEncoder(GeoServer gs,WFSVConfiguration configuration) {
        super(VersionedDescribeResults.class, Collections
                .singleton("text/xml; subtype=gml/3.1.1"));
        this.wfs = gs.getService( WFSInfo.class );
        this.catalog = gs.getCatalog();
        this.configuration = configuration;
    }

    public String getMimeType(Object value, Operation operation)
            throws ServiceException {
        return "text/xml; subtype=gml/3.1.1";
    }

    public void write(Object value, OutputStream output,
            Operation describeFeatureType) throws IOException {
        VersionedDescribeResults results = (VersionedDescribeResults) value;

        // create the schema
        DescribeFeatureTypeType req = (DescribeFeatureTypeType) describeFeatureType
                .getParameters()[0];
        FeatureTypeSchemaBuilder builder = null;
        if (results.isVersioned()) {
            builder = new VersionedSchemaBuilder(wfs.getGeoServer(), configuration);
        } else {
            builder = new FeatureTypeSchemaBuilder.GML3(wfs.getGeoServer());
        }

        XSDSchema schema = builder.build(results.getFeatureTypeInfo(), req.getBaseUrl());

        // serialize
        schema.updateElement();
        XSDResourceImpl.serialize(output, schema.getElement());
    }

    public boolean canHandle(Operation operation) {
        return "DescribeVersionedFeatureType".equalsIgnoreCase(operation
                .getId())
                && operation.getService().getId().equalsIgnoreCase("wfsv");
    }

}
