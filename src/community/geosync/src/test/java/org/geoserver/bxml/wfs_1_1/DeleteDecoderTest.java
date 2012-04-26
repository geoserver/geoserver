package org.geoserver.bxml.wfs_1_1;

import javax.xml.namespace.QName;

import net.opengis.wfs.DeleteElementType;

import org.geoserver.bxml.BxmlTestSupport;
import org.geotools.filter.text.ecql.ECQL;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.opengis.filter.Filter;

public class DeleteDecoderTest extends BxmlTestSupport {

    public void testFeedDecodeDelete1() throws Exception {

        BxmlStreamReader reader = super.getReader("delete1");
        reader.nextTag();
        DeleteElementTypeDecoder decoder = new DeleteElementTypeDecoder();
        DeleteElementType deleteElement = (DeleteElementType) decoder.decode(reader);

        QName deleteTypeName = deleteElement.getTypeName();
        assertEquals("http://opengeo.org/osm", deleteTypeName.getNamespaceURI());
        assertEquals("planet_osm_point", deleteTypeName.getLocalPart());
        Filter filter = deleteElement.getFilter();
        assertNotNull(deleteElement.getFilter());
        assertNotNull(filter);

        reader.close();
    }

    public void testFeedDecodeDelete2() throws Exception {

        BxmlStreamReader reader = super.getReader("delete2");
        reader.nextTag();
        DeleteElementTypeDecoder decoder = new DeleteElementTypeDecoder();
        DeleteElementType deleteElement = (DeleteElementType) decoder.decode(reader);

        QName deleteTypeName = deleteElement.getTypeName();
        assertEquals("http://opengeo.org/osm", deleteTypeName.getNamespaceURI());
        assertEquals("planet_osm_point", deleteTypeName.getLocalPart());
        Filter filter = deleteElement.getFilter();
        assertNotNull(deleteElement.getFilter());

        final Filter expected = ECQL
                .toFilter("(( addresses = Adrees1 ) OR ( NOT (( depth BETWEEN 100 AND 200 ) AND ( street = Street1 )) ))");

        assertEquals(expected.toString(), filter.toString());

        reader.close();

    }

}
