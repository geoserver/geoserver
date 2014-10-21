/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.response;

import java.io.IOException;
import java.io.OutputStream;

import org.geoserver.gss.CentralRevisionsType;
import org.geoserver.gss.CentralRevisionsType.LayerRevision;
import org.geoserver.gss.xml.GSS;
import org.geoserver.gss.xml.GSSConfiguration;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.xml.Encoder;

/**
 * Encodes a {@link CentralRevisionsType} into an XML response
 * 
 * @author aaime
 * 
 */
public class CentralRevisionsOutputFormat extends Response {

    GSSConfiguration configuration;

    GSS gss;

    public CentralRevisionsOutputFormat(GSSConfiguration configuration, GSS gss) {
        super(CentralRevisionsType.class);
        this.configuration = configuration;
        this.gss = gss;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "text/xml";
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException,
            ServiceException {
        CentralRevisionsType cr = (CentralRevisionsType) value;
        Encoder encoder = new Encoder(configuration, gss.getSchema());
        encoder.setIndenting(true);

        // declare namespaces to make the output validate
        for (LayerRevision l : cr.getLayerRevisions()) {
            encoder.getNamespaces().declarePrefix(l.getTypeName().getPrefix(),
                    l.getTypeName().getNamespaceURI());
        }

        encoder.encode(cr, GSS.CentralRevisions, output);
    }

}
