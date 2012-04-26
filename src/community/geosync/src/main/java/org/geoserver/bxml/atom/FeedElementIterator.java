package org.geoserver.bxml.atom;

import static org.geoserver.gss.internal.atom.Atom.entry;

import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.BxmlElementIterator;
import org.gvsig.bxml.stream.BxmlStreamReader;

/**
 * The Class FeedElementIterator.
 * 
 * @author cfarina
 */
public class FeedElementIterator extends BxmlElementIterator {

    /**
     * Instantiates a new feed element iterator.
     * 
     * @param reader
     *            the reader
     * @param elemName
     *            the elem name
     */
    public FeedElementIterator(BxmlStreamReader reader, QName elemName) {
        super(reader, elemName);
    }

    /**
     * Instantiates a new feed element iterator.
     * 
     * @param reader
     *            the reader
     * @param siblingNames
     *            the sibling names
     */
    public FeedElementIterator(final BxmlStreamReader reader, final Set<QName> siblingNames) {
        super(reader, siblingNames);
    }

    /**
     * Finish.
     * 
     * @param reader
     *            the reader
     * @return true, if successful
     */
    protected boolean finish(BxmlStreamReader reader) {
        QName name = reader.getElementName();
        return name.equals(entry);
    }

}
