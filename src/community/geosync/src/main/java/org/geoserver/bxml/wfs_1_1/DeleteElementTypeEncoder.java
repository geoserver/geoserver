package org.geoserver.bxml.wfs_1_1;

import static org.geoserver.wfs.xml.v1_1_0.WFS.DELETE;
import static org.geotools.filter.v1_1.OGC.Filter;

import java.io.IOException;

import javax.xml.namespace.QName;

import net.opengis.wfs.DeleteElementType;

import org.geoserver.bxml.AbstractEncoder;
import org.geoserver.bxml.filter_1_1.FilterEncoder;
import org.gvsig.bxml.stream.BxmlStreamWriter;
import org.opengis.filter.Filter;

/**
 * Handles the encoding of a {@link DeleteElementType} as the content of an {@code atom:enty}
 */
public class DeleteElementTypeEncoder extends AbstractEncoder<DeleteElementType> {

    @Override
    public void encode(final DeleteElementType delete, final BxmlStreamWriter w) throws IOException {
        final QName typeName = delete.getTypeName();
        final Filter filter = delete.getFilter();
        // w.setPrefix("f", typeName.getNamespaceURI());
        startElement(w, DELETE);
        w.writeNamespace("f", typeName.getNamespaceURI());
        attributes(w, "typeName", "f:" + typeName.getLocalPart());
        {
            startElement(w, Filter);
            {
                FilterEncoder filterEncoder = new FilterEncoder();
                filterEncoder.encode(filter, w);
            }
            endElement(w);
        }
        w.writeEndElement();
    }

}
