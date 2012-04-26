package org.geoserver.bxml.atom;

import static org.geoserver.gss.internal.atom.Atom.author;
import static org.geoserver.gss.internal.atom.Atom.contributor;
import static org.geoserver.gss.internal.atom.Atom.entry;
import static org.geoserver.gss.internal.atom.Atom.id;
import static org.geoserver.gss.internal.atom.Atom.published;
import static org.geoserver.gss.internal.atom.Atom.rights;
import static org.geoserver.gss.internal.atom.Atom.source;
import static org.geoserver.gss.internal.atom.Atom.summary;
import static org.geoserver.gss.internal.atom.Atom.title;
import static org.geoserver.gss.internal.atom.Atom.updated;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.ChoiceDecoder;
import org.geoserver.bxml.Decoder;
import org.geoserver.bxml.SequenceDecoder;
import org.geoserver.bxml.SetterDecoder;
import org.geoserver.bxml.base.DateDecoder;
import org.geoserver.bxml.base.StringDecoder;
import org.geoserver.gss.internal.atom.Atom;
import org.geoserver.gss.internal.atom.EntryImpl;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;

import com.google.common.collect.Iterators;

/**
 * The Class EntryDecoder.
 * 
 * @author cfarina
 */
public class EntryDecoder implements Decoder<EntryImpl> {

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the entry impl
     * @throws Exception
     *             the exception
     */
    @Override
    public EntryImpl decode(BxmlStreamReader r) throws Exception {
        r.require(EventType.START_ELEMENT, entry.getNamespaceURI(), entry.getLocalPart());

        final EntryImpl entry = new EntryImpl();
        ChoiceDecoder<Object> choice = new ChoiceDecoder<Object>();
        choice.addOption(new SetterDecoder<Object>(new DateDecoder(updated), entry, "updated"));
        choice.addOption(new SetterDecoder<Object>(new PersonDecoder(author), entry, "author"));
        choice.addOption(new SetterDecoder<Object>(new PersonDecoder(contributor), entry,
                "contributor"));
        choice.addOption(new SetterDecoder<Object>(new CategoryDecoder(), entry, "category"));
        choice.addOption(new SetterDecoder<Object>(new LinkDecoder(), entry, "link"));
        choice.addOption(new SetterDecoder<Object>(new DateDecoder(published), entry, "published"));
        choice.addOption(new SetterDecoder<Object>(new StringDecoder(title), entry, "title"));
        choice.addOption(new SetterDecoder<Object>(new StringDecoder(summary), entry, "summary"));
        choice.addOption(new SetterDecoder<Object>(new StringDecoder(id), entry, "id"));
        choice.addOption(new SetterDecoder<Object>(new StringDecoder(rights), entry, "rights"));
        choice.addOption(new SetterDecoder<Object>(new StringDecoder(source), entry, "source"));
        choice.addOption(new SetterDecoder<Object>(new ContentDecoder(), entry, "content"));
        choice.addOption(new SetterDecoder<Object>(new WhereDecoder(), entry, "where"));

        SequenceDecoder<Object> seq = new SequenceDecoder<Object>(1, 1);
        seq.add(choice, 0, Integer.MAX_VALUE);

        r.nextTag();
        Iterator<Object> iterator = seq.decode(r);
        Iterators.toArray(iterator, Object.class);
        return entry;
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
        return Atom.entry.equals(name);
    }

    /**
     * Gets the targets.
     * 
     * @return the targets
     */
    @Override
    public Set<QName> getTargets() {
        return Collections.singleton(Atom.entry);
    }

}
