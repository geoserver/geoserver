/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2007 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import java.io.OutputStream;
import javax.xml.transform.TransformerException;
import net.opengis.cat.csw20.RequestBaseType;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.CSWInfo;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.iso.MetaDataDescriptor;
import org.geoserver.platform.ServiceException;

/**
 * Encodes responses with ISO MetaData records
 *
 * @author Niels Charlier
 */
public class MetaDataResponse extends AbstractRecordsResponse {

    public MetaDataResponse(GeoServer gs) {
        super(CSWRecordDescriptor.RECORD_TYPE, MetaDataDescriptor.NAMESPACE_GMD, gs);
    }

    protected void transformResponse(
            OutputStream output, CSWRecordsResult result, RequestBaseType request, CSWInfo csw) {
        MetaDataTransformer transformer =
                new MetaDataTransformer(request, csw.isCanonicalSchemaLocation());
        transformer.setIndentation(2);
        try {
            transformer.transform(result, output);
        } catch (TransformerException e) {
            throw new ServiceException(e);
        }
    }
}
