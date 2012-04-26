package org.geoserver.bxml.feature;

import javax.xml.namespace.QName;

import org.geoserver.bxml.base.SimpleDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.springframework.util.Assert;

/**
 * The Class PropertyNameDecoder.
 * 
 * @author cfarina
 */
public class PropertyNameDecoder extends SimpleDecoder<QName> {

    /** The type name. */
    private final QName typeName;

    /**
     * Instantiates a new property name decoder.
     * 
     * @param typeName
     *            the type name
     */
    public PropertyNameDecoder(QName typeName) {
        super(new QName("http://www.opengis.net/wfs", "Name"));
        this.typeName = typeName;
    }

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the q name
     * @throws Exception
     *             the exception
     */
    @SuppressWarnings("unused")
    @Override
    public QName decode(BxmlStreamReader r) throws Exception {
        final QName elementName = r.getElementName();
        Assert.isTrue(canHandle(elementName));
        r.require(EventType.START_ELEMENT, elementName.getNamespaceURI(),
                elementName.getLocalPart());

        StringBuilder sb = new StringBuilder();
        EventType event;
        while ((event = r.next()).isValue()) {
            String chunk = r.getStringValue();
            sb.append(chunk);
        }

        r.require(EventType.END_ELEMENT, elementName.getNamespaceURI(), elementName.getLocalPart());

        return FeatureTypeUtil.buildQName(sb.toString(), typeName.getNamespaceURI());

    }

}
