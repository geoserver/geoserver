package org.geoserver.bxml;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.geoserver.bxml.atom.AbstractAtomEncoder;
import org.geotools.util.logging.Logging;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;

/**
 * The Class BXMLDecoderUtil has some static helpers methods.
 * 
 * @author cfarina
 */
public class BXMLDecoderUtil {

    /** The Constant LOGGER. */
    protected static final Logger LOGGER = Logging.getLogger(AbstractAtomEncoder.class);

    /**
     * Parses the long value.
     * 
     * @param stringValue
     *            the string value
     * @param name
     *            the name
     * @return the long
     */
    public static Long parseLongValue(String stringValue, String name) {
        if (stringValue == null || stringValue.equals("")) {
            LOGGER.warning(name + " value mustn't be null.");
        } else {
            Long value = 0l;

            try {
                value = new Long(stringValue);
                return value;
            } catch (NumberFormatException e) {
                LOGGER.warning(name + " value must be numeric.");
                return null;
            }
        }
        return null;
    }

    /**
     * Go to end.
     * 
     * @param r
     *            the r
     * @param name
     *            the name
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static void goToEnd(final BxmlStreamReader r, final QName name) throws IOException {
        if (!EventType.END_DOCUMENT.equals(r.getEventType())) {
            while (!EventType.END_ELEMENT.equals(r.getEventType())
                    || !name.equals(r.getElementName())) {
                r.next();
            }
        }
    }

}
