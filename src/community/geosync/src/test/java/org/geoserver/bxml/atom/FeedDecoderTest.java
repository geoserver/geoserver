package org.geoserver.bxml.atom;

import java.util.Date;
import java.util.Iterator;

import org.geoserver.gss.internal.atom.EntryImpl;
import org.geoserver.gss.internal.atom.FeedImpl;
import org.geotools.feature.type.DateUtil;
import org.gvsig.bxml.stream.BxmlStreamReader;

public class FeedDecoderTest extends AtomTestSupport {

    public void testFeed1() throws Exception {
        BxmlStreamReader reader = super.getReader("feed1");
        reader.nextTag();
        FeedDecoder feedDecoder = new FeedDecoder();
        FeedImpl feed = feedDecoder.decode(reader);

        assertEquals(new Long(50), feed.getMaxEntries());
        assertEquals(new Long(1), feed.getStartPosition());

        assertEquals("01cbd610bbf9e37714980377ffc6600dc3fef24e", feed.getId());
        assertEquals("This is a feed title.", feed.getTitle());
        assertEquals("This is a feed subtitle.", feed.getSubtitle());

        assertEquals(new Date(DateUtil.parseDateTime("2011-05-31T21:48:06.466Z")),
                feed.getUpdated());

        assertEquals(1, feed.getAuthor().size());
        testPerson("mperez", "mperez@host.com", null, feed.getAuthor().get(0));
        testPerson("fperez", "fperez@host.com", null, feed.getContributor().get(0));

        assertEquals(2, feed.getCategory().size());

        testCategory("term1", "scheme1", feed.getCategory().get(0));
        testCategory("term2", "scheme2", feed.getCategory().get(1));

        testGenerator("Generator Value 1", "generatoruri.org", "v1.3", feed.getGenerator());

        assertEquals(2, feed.getLink().size());

        testLink("http://example.org/", null, "text/html", "en", "title1", null, feed.getLink()
                .get(0));
        testLink("http://example2.org/", "alternate", "text/html", "es", "title2", null, feed
                .getLink().get(1));

        assertTrue(feed.getEntry().hasNext());

        EntryImpl entry = feed.getEntry().next();
        assertNotNull(entry);
        assertEquals("453e5a7ba8917ed3550e088d69ff1ac0aa6d1a4d", entry.getId());
        reader.close();
    }

    public void testFeed2() throws Exception {
        BxmlStreamReader reader = super.getReader("feed2");
        reader.nextTag();
        FeedDecoder feedDecoder = new FeedDecoder();
        FeedImpl feed = feedDecoder.decode(reader);

        assertEquals(new Long(50), feed.getMaxEntries());
        assertEquals(new Long(1), feed.getStartPosition());

        assertEquals("01cbd610bbf9e37714980377ffc6600dc3fef24e", feed.getId());
        assertEquals("This is a feed title.", feed.getTitle());
        assertEquals("This is an icon.", feed.getIcon());
        assertEquals("This is the rights.", feed.getRights());

        assertEquals(new Date(DateUtil.parseDateTime("2010-03-20T21:30:03.466Z")),
                feed.getUpdated());

        assertEquals(2, feed.getAuthor().size());
        testPerson("cfarina", "cfarina@host.com", "www.geoserver.org", feed.getAuthor().get(0));
        testPerson("mperez", "mperez@host.com", null, feed.getAuthor().get(1));

        assertEquals(2, feed.getCategory().size());

        testCategory("term1", "scheme1", feed.getCategory().get(0));
        testCategory("term2", "scheme2", feed.getCategory().get(1));

        assertEquals(1, feed.getLink().size());

        testLink("http://example3.org/", "alternate", "text/html", "en", null, new Long(25), feed
                .getLink().get(0));

        assertTrue(feed.getEntry().hasNext());

        EntryImpl entry = feed.getEntry().next();

        assertEquals("453e5a7ba8917ed3550e088d69ff1ac0aa6d1a4d", entry.getId());
        assertEquals("Delte of Feature planet_osm_point.100", entry.getTitle());
        assertEquals("Commit automatically accepted as it comes from a WFS transaction",
                entry.getSummary());

    }

    public void testFeed3() throws Exception {
        BxmlStreamReader reader = super.getReader("feed3");
        reader.nextTag();
        FeedDecoder feedDecoder = new FeedDecoder();
        final FeedImpl feed = feedDecoder.decode(reader);

        Iterator<EntryImpl> entries = feed.getEntry();
        assertNotNull(entries);
        assertTrue(entries.hasNext());

        EntryImpl entry = entries.next();

        assertEquals("453e5a7ba8917ed3550e088d69ff1ac0aa6d1a4d", entry.getId());
        assertEquals("Delte of Feature planet_osm_point.100", entry.getTitle());
        assertEquals("Commit automatically accepted as it comes from a WFS transaction",
                entry.getSummary());

    }
}
