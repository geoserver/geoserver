package org.geoserver.bxml.feature;

import javax.xml.namespace.QName;

import org.geoserver.bxml.base.GenericValueDecoder;
import org.geoserver.bxml.base.SimpleDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;

/**
 * The Class PropertyValueDecoder.
 * 
 * @author cfarina
 */
public class PropertyValueDecoder extends SimpleDecoder<Object> {

    /**
     * Instantiates a new property value decoder.
     */
    public PropertyValueDecoder() {
        super(new QName("http://www.opengis.net/wfs", "Value"));
    }

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the object
     * @throws Exception
     *             the exception
     */
    @Override
    public Object decode(BxmlStreamReader r) throws Exception {
        QName name = r.getElementName();
        r.require(EventType.START_ELEMENT, null, name.getLocalPart());

        Object value = new GenericValueDecoder().decode(r);

        r.require(EventType.END_ELEMENT, null, name.getLocalPart());
        return value;
    }

}
