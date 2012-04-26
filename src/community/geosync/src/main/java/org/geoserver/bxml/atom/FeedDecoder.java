package org.geoserver.bxml.atom;

import static org.geoserver.gss.internal.atom.Atom.author;
import static org.geoserver.gss.internal.atom.Atom.contributor;
import static org.geoserver.gss.internal.atom.Atom.icon;
import static org.geoserver.gss.internal.atom.Atom.id;
import static org.geoserver.gss.internal.atom.Atom.rights;
import static org.geoserver.gss.internal.atom.Atom.subtitle;
import static org.geoserver.gss.internal.atom.Atom.title;
import static org.geoserver.gss.internal.atom.Atom.updated;

import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.geoserver.bxml.BxmlElementIterator;
import org.geoserver.bxml.ChoiceDecoder;
import org.geoserver.bxml.SequenceDecoder;
import org.geoserver.bxml.SetterDecoder;
import org.geoserver.bxml.base.DateDecoder;
import org.geoserver.bxml.base.SimpleDecoder;
import org.geoserver.bxml.base.StringDecoder;
import org.geoserver.gss.internal.atom.Atom;
import org.geoserver.gss.internal.atom.EntryImpl;
import org.geoserver.gss.internal.atom.FeedImpl;
import org.geotools.util.logging.Logging;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.springframework.util.Assert;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;

/**
 * Decodes an atom xml with.
 * 
 * @author cfarina
 */
public class FeedDecoder extends SimpleDecoder<FeedImpl> {

    /** The LOGGER. */
    protected final Logger LOGGER;

    /** The start position. */
    public static QName startPosition = new QName("http://www.w3.org/2005/Atom", "startPosition");

    /** The max entries. */
    public static QName maxEntries = new QName("http://www.w3.org/2005/Atom", "maxEntries");

    /** The entry reader function. */
    private Function<BxmlStreamReader, EntryImpl> entryReaderFunction;

    /**
     * Instantiates a new feed decoder.
     */
    public FeedDecoder() {
        super(Atom.feed);
        LOGGER = Logging.getLogger(getClass());

        entryReaderFunction = new Function<BxmlStreamReader, EntryImpl>() {

            @Override
            public EntryImpl apply(BxmlStreamReader input) {
                EntryImpl entry = null;
                try {
                    entry = new EntryDecoder().decode(input);
                } catch (Exception e) {
                    Throwables.propagate(e);
                }
                return entry;
            }
        };
    }

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the feed impl
     * @throws Exception
     *             the exception
     */
    @Override
    public FeedImpl decode(BxmlStreamReader r) throws Exception {
        // r.nextTag();
        r.require(EventType.START_ELEMENT, elemName.getNamespaceURI(), elemName.getLocalPart());
        final QName name = r.getElementName();
        Assert.isTrue(canHandle(name));

        FeedImpl feed = new FeedImpl();

        String startPositionValue = r.getAttributeValue(null, startPosition.getLocalPart());
        if (startPositionValue != null) {
            feed.setStartPosition(Long.parseLong(startPositionValue));
        }

        String maxEntriesValue = r.getAttributeValue(null, maxEntries.getLocalPart());
        if (maxEntriesValue != null) {
            feed.setMaxEntries(Long.parseLong(maxEntriesValue));
        }

        ChoiceDecoder<Object> choice = new ChoiceDecoder<Object>();
        choice.addOption(new SetterDecoder<Object>(new StringDecoder(id), feed, "id"));
        choice.addOption(new SetterDecoder<Object>(new StringDecoder(title), feed, "title"));
        choice.addOption(new SetterDecoder<Object>(new StringDecoder(subtitle), feed, "subtitle"));
        choice.addOption(new SetterDecoder<Object>(new StringDecoder(icon), feed, "icon"));
        choice.addOption(new SetterDecoder<Object>(new StringDecoder(rights), feed, "rights"));
        choice.addOption(new SetterDecoder<Object>(new DateDecoder(updated), feed, "updated"));
        choice.addOption(new SetterDecoder<Object>(new PersonDecoder(author), feed, "author"));
        choice.addOption(new SetterDecoder<Object>(new PersonDecoder(contributor), feed,
                "contributor"));
        choice.addOption(new SetterDecoder<Object>(new CategoryDecoder(), feed, "category"));
        choice.addOption(new SetterDecoder<Object>(new LinkDecoder(), feed, "link"));
        choice.addOption(new SetterDecoder<Object>(new GeneratorDecoder(), feed, "generator"));

        SequenceDecoder<Object> seq = new FeedSequenceDecoder<Object>(1, 1);
        seq.add(choice, 0, Integer.MAX_VALUE);
        final EventType tag = r.nextTag();
        final QName elementName = r.getElementName();
        final boolean emptyFeed = EventType.END_ELEMENT.equals(tag)
                && Atom.feed.equals(elementName);
        if (!emptyFeed) {
            Iterator<Object> sequenceWithoutEntries = seq.decode(r);
            // consume all elements besides entry, that must come last
            Iterators.toArray(sequenceWithoutEntries, Object.class);

            Iterator<BxmlStreamReader> entryElemIterator = new BxmlElementIterator(r, Atom.entry);

            Iterator<EntryImpl> entryIterator;
            entryIterator = Iterators.transform(entryElemIterator, entryReaderFunction);

            feed.setEntry(entryIterator);
        }

        return feed;
    }

}
