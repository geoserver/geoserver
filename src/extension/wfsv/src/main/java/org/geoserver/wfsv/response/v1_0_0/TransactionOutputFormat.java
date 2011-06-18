package org.geoserver.wfsv.response.v1_0_0;

import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.params;

import java.util.Map;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.wfs.xml.v1_0_0.WFSConfiguration;
import org.geoserver.wfsv.response.v1_1_0.AbstractTransactionOutputFormat;
import org.geotools.xml.Encoder;
import org.opengis.filter.FilterFactory;

public class TransactionOutputFormat extends AbstractTransactionOutputFormat {

    public TransactionOutputFormat(GeoServer gs, WFSConfiguration configuration,
            FilterFactory filterFactory) {
        super(gs, configuration, filterFactory,
                org.geoserver.wfs.xml.v1_0_0.WFS.TRANSACTION,
                "text/xml; subtype=wfs-transaction/1.0.0");

    }

    protected void encodeTypeSchemaLocation(Encoder encoder, String baseURL,
            String namespaceURI, StringBuffer typeNames) {
        Map<String, String> params = params("service", "WFS",
                "version", "1.0.0", 
                "request", "DescribeFeatureType",
                "typeName", typeNames.toString());
        encoder.setSchemaLocation(namespaceURI, buildURL(baseURL, "wfs", params, URLType.RESOURCE));
    }

    protected void encodeWfsSchemaLocation(Encoder encoder, String baseURL) {
        encoder.setSchemaLocation(org.geoserver.wfs.xml.v1_0_0.WFS.NAMESPACE, 
                buildSchemaURL(baseURL, "schemas/wfs/1.0.0/WFS-transaction.xsd"));
    }

}
