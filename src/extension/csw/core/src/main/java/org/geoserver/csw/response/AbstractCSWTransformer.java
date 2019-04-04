/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import static org.geoserver.ows.util.ResponseUtils.*;

import java.io.Writer;
import javax.xml.transform.TransformerException;
import net.opengis.cat.csw20.RequestBaseType;
import org.geotools.xml.transform.TransformerBase;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Base class for CSW transformers
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractCSWTransformer extends TransformerBase {

    static final String CSW_ROOT_LOCATION = "http://schemas.opengis.net/csw/2.0.2/";

    protected RequestBaseType request;

    protected boolean canonicalSchemaLocation;

    public AbstractCSWTransformer(RequestBaseType request, boolean canonicalSchemaLocation) {
        this.request = request;
        this.canonicalSchemaLocation = canonicalSchemaLocation;
    }

    public void encode(CSWRecordsResult response, Writer writer) throws TransformerException {
        transform(response, writer);
    }

    protected abstract class AbstractCSWTranslator extends TranslatorSupport {

        public AbstractCSWTranslator(ContentHandler handler) {
            super(handler, null, null);
        }

        protected void addAttribute(AttributesImpl attributes, String name, Object value) {
            if (value != null) {
                attributes.addAttribute(
                        "",
                        name,
                        name,
                        "",
                        value instanceof String ? (String) value : String.valueOf(value));
            }
        }

        protected String cswSchemaLocation(String schema) {
            if (canonicalSchemaLocation) {
                return CSW_ROOT_LOCATION + schema;
            } else {
                return buildSchemaURL(request.getBaseUrl(), "csw/2.0.2/" + schema);
            }
        }
    }
}
