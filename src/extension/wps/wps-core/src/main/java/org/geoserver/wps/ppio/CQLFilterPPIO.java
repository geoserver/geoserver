/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;

/**
 * Parses and encodes an OGC filter using ECQL
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CQLFilterPPIO extends CDataPPIO {

    public CQLFilterPPIO() {
        super(Filter.class, Filter.class, "text/plain; subtype=cql");
    }

    @Override
    public Object decode(String value) throws Exception {
        return ECQL.toFilter(value);
    }

    @Override
    public void encode(Object value, OutputStream os) throws IOException {
        String cql = ECQL.toCQL((Filter) value);
        os.write(cql.getBytes("UTF-8"));
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        return decode(IOUtils.toString(input, "UTF-8"));
    }

    @Override
    public String getFileExtension() {
        return "txt";
    }
}
