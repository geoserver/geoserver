package org.geoserver.bxml.wfs_1_1;

import static org.geoserver.wfs.xml.v1_1_0.WFS.INSERT;

import java.io.IOException;
import java.util.Iterator;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;

import org.geoserver.bxml.AbstractEncoder;
import org.geoserver.bxml.gml_3_1.FeatureEncoder;
import org.gvsig.bxml.stream.BxmlStreamWriter;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Handles the encoding of a {@link DeleteElementType} as the content of an {@code atom:enty}
 */
public class InsertElementTypeEncoder extends AbstractEncoder<InsertElementType> {

    @SuppressWarnings("unchecked")
    @Override
    public void encode(final InsertElementType insert, final BxmlStreamWriter w) throws IOException {

        // WFS namespace is already bound at root document
        w.writeStartElement(INSERT.getNamespaceURI(), INSERT.getLocalPart());
        {
            FeatureEncoder<Feature> featureEncoder = new FeatureEncoder<Feature>();
            Feature feature;
            for (Iterator<Feature> it = insert.getFeature().iterator(); it.hasNext();) {
                feature = it.next();

                final String namespaceURI = feature.getType().getName().getNamespaceURI();
                if (null == w.getPrefix(namespaceURI)) {
                    w.writeNamespace("f", namespaceURI);
                }

                featureEncoder.encode((SimpleFeature) feature, w);
            }
        }
        w.writeEndElement();
    }

}
