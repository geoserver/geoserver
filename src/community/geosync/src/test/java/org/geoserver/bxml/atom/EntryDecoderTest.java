package org.geoserver.bxml.atom;

import java.util.Date;

import javax.xml.namespace.QName;

import net.opengis.wfs.DeleteElementType;

import org.geoserver.gss.internal.atom.ContentImpl;
import org.geoserver.gss.internal.atom.EntryImpl;
import org.geotools.feature.type.DateUtil;
import org.geotools.filter.text.ecql.ECQL;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Polygon;

public class EntryDecoderTest extends AtomTestSupport {

    public void testDecodeEntry1() throws Exception {
        BxmlStreamReader reader = super.getReader("entry1");
        EntryDecoder decoder = new EntryDecoder();

        reader.nextTag();
        EntryImpl entry = decoder.decode(reader);
        assertNotNull(entry);

        assertEquals("212c812cde528ad912f6eda535c631e6f2e932c9", entry.getId());
        assertEquals("Insert of Feature building.19484", entry.getTitle());
        assertEquals("new building", entry.getSummary());
        assertEquals(new Date(DateUtil.parseDateTime("2011-07-07T22:47:06.507Z")),
                entry.getUpdated());
        assertEquals(1, entry.getAuthor().size());
        assertEquals("gabriel", entry.getAuthor().get(0).getName());
        assertEquals(1, entry.getContributor().size());
        assertEquals("geoserver", entry.getContributor().get(0).getName());
        reader.close();
    }

    public void testDecodeEntry2() throws Exception {
        BxmlStreamReader reader = super.getReader("entry2");
        EntryDecoder decoder = new EntryDecoder();

        reader.nextTag();
        EntryImpl entry = decoder.decode(reader);
        assertNotNull(entry);

        assertEquals("453e5a7ba8917ed3550e088d69ff1ac0aa6d1a4d", entry.getId());
        assertEquals("Delte of Feature planet_osm_point.100", entry.getTitle());

        assertNotNull(entry.getContent());

        ContentImpl content1 = entry.getContent();
        assertEquals("type1", content1.getType());
        assertEquals("source1", content1.getSrc());

        DeleteElementType deleteElement = (DeleteElementType) content1.getValue();
        QName deleteTypeName = deleteElement.getTypeName();
        // assertEquals("http://opengeo.org/osm", deleteTypeName.getNamespaceURI());
        assertEquals("planet_osm_point", deleteTypeName.getLocalPart());
        Filter filter = deleteElement.getFilter();
        assertNotNull(deleteElement.getFilter());
        final Filter expected = ECQL
                .toFilter("(( addresses = Adrees1 ) OR ( NOT (( depth BETWEEN 100 AND 200 ) AND ( street = Street1 )) ))");

        assertEquals(expected.toString(), filter.toString());
        Polygon where = (Polygon) entry.getWhere();
        assertNotNull(where);

        reader.close();
    }

    public void testDecodeEntry3() throws Exception {
        BxmlStreamReader reader = super.getReader("entry3");
        EntryDecoder decoder = new EntryDecoder();

        reader.nextTag();
        EntryImpl entry = decoder.decode(reader);

        assertEquals("453e5a7ba8917ed3550e088d69ff1ac0aa6d1a4d", entry.getId());
        assertEquals("Delte of Feature planet_osm_point.100", entry.getTitle());
        assertEquals("Commit automatically accepted as it comes from a WFS transaction",
                entry.getSummary());
        assertEquals(new Date(DateUtil.parseDateTime("2012-05-28T18:27:15.466Z")),
                entry.getUpdated());

        assertEquals(2, entry.getAuthor().size());

        testPerson("msanchez", "msanchez@example.com", "www.msanchez.org", entry.getAuthor().get(0));
        testPerson("pmolina", null, "www.pmolina.org", entry.getAuthor().get(1));

        assertEquals(1, entry.getContributor().size());
        testPerson("contributor1", "contributor1@test.com", null, entry.getContributor().get(0));

        assertEquals(3, entry.getCategory().size());

        testCategory("categoryTerm1", "categoryScheme1", entry.getCategory().get(0));
        testCategory("categoryTerm2", null, entry.getCategory().get(1));
        testCategory(null, "categoryScheme3", entry.getCategory().get(2));

        assertEquals(2, entry.getLink().size());

        testLink("http://entryexample.org/", null, "text/html", "en", "Entry Title 1", null, entry
                .getLink().get(0));
        testLink("http://entryexample2.org/", "alternate", "text/html", "es", "Entry Title 3",
                null, entry.getLink().get(1));

        assertEquals(new Date(DateUtil.parseDateTime("2010-08-20T21:48:06.466Z")),
                entry.getPublished());

        assertEquals("This are the entry rights.", entry.getRights());
        assertEquals("Entry source.", entry.getSource());
        reader.close();
    }
}
