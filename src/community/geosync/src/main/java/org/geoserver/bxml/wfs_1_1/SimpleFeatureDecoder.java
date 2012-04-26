package org.geoserver.bxml.wfs_1_1;

import static org.geotools.gml3.GML.id;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.Context;
import org.geoserver.bxml.Decoder;
import org.geoserver.bxml.FeatureTypeProvider;
import org.geoserver.bxml.SetterDecoder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

import com.google.common.collect.Iterators;

/**
 * The Class SimpleFeatureDecoder.
 * 
 * @author cfarina
 */
public class SimpleFeatureDecoder implements Decoder<SimpleFeature> {

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the simple feature
     * @throws Exception
     *             the exception
     */
    @Override
    public SimpleFeature decode(BxmlStreamReader r) throws Exception {
        r.require(EventType.START_ELEMENT, null, null);
        // SimpleFeature simpleFeature =
        QName typeName = r.getElementName();

        final FeatureTypeProvider featureTypeProvider = Context.get(FeatureTypeProvider.class);

        final Name name = new NameImpl(typeName);
        FeatureType featureType = featureTypeProvider.resolveFeatureType(name);

        SimpleFeatureBuilder builder = new SimpleFeatureBuilder((SimpleFeatureType) featureType);
        String featureId = r.getAttributeValue(null, id.getLocalPart());

        SimpleFeatureSequenceDecoder<Object> seq = new SimpleFeatureSequenceDecoder<Object>(1, 1);
        SimpleFeatureAttributes simpleFeatureAttributes = new SimpleFeatureAttributes();
        seq.add(new SetterDecoder<Object>(new SimpleFeatureAttributeDecoder(),
                simpleFeatureAttributes, "attributes"), 0, Integer.MAX_VALUE);

        r.nextTag();
        Iterator<Object> iterator = seq.decode(r);
        Iterators.toArray(iterator, Object.class);

        int index = 0;
        for (Object attribute : simpleFeatureAttributes.getAttributes()) {
            builder.set(index, attribute);
            index++;
        }
        SimpleFeature simpleFeature = builder.buildFeature(featureId);
        return simpleFeature;
    }

    /**
     * Can handle.
     * 
     * @param name
     *            the name
     * @return true, if successful
     */
    @Override
    public boolean canHandle(QName name) {
        return true;
    }

    /**
     * Gets the targets.
     * 
     * @return the targets
     */
    @Override
    public Set<QName> getTargets() {
        return new HashSet<QName>();
    }

}
