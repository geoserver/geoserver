package org.geoserver.bxml.wfs_1_1;

import static org.geoserver.wfs.xml.v1_1_0.WFS.UPDATE;
import static org.geotools.filter.v1_1.OGC.Filter;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import javax.xml.namespace.QName;

import net.opengis.wfs.PropertyType;
import net.opengis.wfs.UpdateElementType;

import org.geoserver.bxml.AbstractEncoder;
import org.geoserver.bxml.filter_1_1.FilterEncoder;
import org.geoserver.bxml.gml_3_1.PropertyValueEncoder;
import org.geoserver.gss.impl.GSS;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.gvsig.bxml.stream.BxmlStreamWriter;
import org.opengis.feature.FeatureFactory;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.util.Assert;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Handles the XML encoding of an {@link UpdateElementType} as the content of an {@code atom:entry}
 * 
 */
public class UpdateElementTypeEncoder extends AbstractEncoder<UpdateElementType> {

    private final FeatureFactory featureFactory;

    public UpdateElementTypeEncoder() {
        featureFactory = CommonFactoryFinder.getFeatureFactory(null);
    }

    @Override
    public void encode(final UpdateElementType update, final BxmlStreamWriter w) throws IOException {

        final QName typeName = update.getTypeName();

        final String namespaceURI = typeName.getNamespaceURI();
        final String localTypeName = typeName.getLocalPart();

        final GSS gss = GSS.get();
        final FeatureType featureType = gss.getFeatureType(namespaceURI, localTypeName);

        startElement(w, UPDATE);
        w.writeNamespace("f", namespaceURI);
        attributes(w, true, "typeName", "f:" + localTypeName);
        {
            @SuppressWarnings("unchecked")
            final List<PropertyType> properties = update.getProperty();
            QName propertyName;
            Object propertyValue;
            String propertyNsUri;
            PropertyDescriptor descriptor;

            for (PropertyType wfsProperty : properties) {
                propertyName = wfsProperty.getName();
                propertyValue = wfsProperty.getValue();
                propertyNsUri = propertyName.getNamespaceURI();

                String simplePropertyName = propertyName.getLocalPart();
                descriptor = featureType.getDescriptor(new NameImpl(propertyNsUri,
                        simplePropertyName));

                // well, SimpleFeatureType is not behaving correctly and returning null...
                if (descriptor == null && featureType instanceof SimpleFeatureType) {
                    descriptor = ((SimpleFeatureType) featureType)
                            .getDescriptor(simplePropertyName);
                }

                Assert.notNull(descriptor);

                startElement(w, WFS.PROPERTY);
                {
                    String prefix = w.getPrefix(propertyNsUri);
                    if (prefix == null) {
                        // we found a property whose namespace is not yet bound. It has to be as the
                        // content of wfs:PropertyName is QName
                        prefix = "p" + Math.abs(new Random().nextInt());
                        w.writeNamespace(prefix, propertyNsUri);
                    }
                    String qName = prefix + ":" + simplePropertyName;
                    element(w, new QName(WFS.NAMESPACE, "Name"), true, qName, true);
                    startElement(w, new QName(WFS.NAMESPACE, "Value"));
                    {
                        Property property = property(featureType, propertyValue, descriptor);
                        PropertyValueEncoder<Property> propertyEncoder = new PropertyValueEncoder<Property>();
                        propertyEncoder.encode(property, w);
                    }
                    endElement(w);
                }
                endElement(w);
            }

            final Filter filter = update.getFilter();
            startElement(w, Filter);
            {
                FilterEncoder filterEncoder = new FilterEncoder();
                filterEncoder.encode(filter, w);
            }
            endElement(w);
        }
        endElement(w);
    }

    private Property property(final FeatureType featureType, Object propertyValue,
            PropertyDescriptor descriptor) {
        Property property;
        if (descriptor instanceof GeometryDescriptor) {
            CoordinateReferenceSystem crs;
            if (propertyValue instanceof Geometry
                    && ((Geometry) propertyValue).getUserData() instanceof CoordinateReferenceSystem) {
                crs = (CoordinateReferenceSystem) ((Geometry) propertyValue).getUserData();
            } else {
                crs = ((GeometryDescriptor) descriptor).getCoordinateReferenceSystem();
            }
            if (crs == null) {
                crs = featureType.getCoordinateReferenceSystem();
            }
            property = featureFactory.createGeometryAttribute(propertyValue,
                    (GeometryDescriptor) descriptor, null, crs);

        } else {
            property = featureFactory.createAttribute(propertyValue,
                    (AttributeDescriptor) descriptor, null);
        }
        return property;
    }
}
