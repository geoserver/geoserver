package org.geoserver.bxml.feature;

import static org.geoserver.wfs.xml.v1_1_0.WFS.PROPERTY;

import java.util.Iterator;

import javax.xml.namespace.QName;

import net.opengis.wfs.PropertyType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs.impl.WfsFactoryImpl;

import org.geoserver.bxml.ChoiceDecoder;
import org.geoserver.bxml.SequenceDecoder;
import org.geoserver.bxml.SetterDecoder;
import org.geoserver.bxml.base.SimpleDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.springframework.util.Assert;

import com.google.common.collect.Iterators;

/**
 * The Class PropertyDecoder.
 * 
 * @author cfarina
 */
public class PropertyDecoder extends SimpleDecoder<PropertyType> {

    /** The factory. */
    final WfsFactory factory = WfsFactoryImpl.eINSTANCE;

    /** The type name. */
    private final QName typeName;

    /**
     * Instantiates a new property decoder.
     * 
     * @param typeName
     *            the type name
     */
    public PropertyDecoder(QName typeName) {
        super(PROPERTY);
        this.typeName = typeName;
    }

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the property type
     * @throws Exception
     *             the exception
     */
    @Override
    public PropertyType decode(BxmlStreamReader r) throws Exception {
        final QName elementName = r.getElementName();
        Assert.isTrue(canHandle(elementName));
        r.require(EventType.START_ELEMENT, elementName.getNamespaceURI(),
                elementName.getLocalPart());
        PropertyType property = factory.createPropertyType();

        ChoiceDecoder<Object> choice = new ChoiceDecoder<Object>();

        choice.addOption(new SetterDecoder<Object>(new PropertyNameDecoder(typeName), property,
                "name"));
        choice.addOption(new SetterDecoder<Object>(new PropertyValueDecoder(), property, "value"));

        SequenceDecoder<Object> seq = new SequenceDecoder<Object>(1, 1);
        seq.add(choice, 0, Integer.MAX_VALUE);

        r.nextTag();
        Iterator<Object> iterator = seq.decode(r);
        Iterators.toArray(iterator, Object.class);

        r.require(EventType.END_ELEMENT, elementName.getNamespaceURI(), elementName.getLocalPart());
        return property;
    }
}
