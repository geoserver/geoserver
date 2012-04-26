package org.geoserver.bxml.gml_3_1;

import java.io.IOException;

import org.gvsig.bxml.geoserver.Gml3Encoder;
import org.gvsig.bxml.geoserver.Gml3Encoder.AttributeEncoder;
import org.gvsig.bxml.stream.BxmlStreamWriter;
import org.opengis.feature.Attribute;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.springframework.util.Assert;

/**
 * Encodes the <b>value</b> (i.e. doesn't output the property name) of an {@link Attribute}.
 * 
 * @author groldan
 * 
 * @param <T>
 */
public class PropertyValueEncoder<T extends Property> extends AbstractGMLEncoder<T> {

    @Override
    public void encode(final T property, final BxmlStreamWriter w) throws IOException {
        Assert.notNull(property, "property can't be null");

        final Gml3Encoder gml3Encoder = super.getGmlEncoder();

        Attribute attribute = (Attribute) property;
        AttributeType type = attribute.getType();
        Class<?> binding = type.getBinding();
        AttributeEncoder attributeEncoder = Gml3Encoder.getAttributeEncoder(binding);
        AttributeDescriptor descriptor = attribute.getDescriptor();

        attributeEncoder.encode(gml3Encoder, attribute.getValue(), descriptor, w);
    }
}
