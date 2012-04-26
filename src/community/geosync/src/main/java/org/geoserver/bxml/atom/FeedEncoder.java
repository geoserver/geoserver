package org.geoserver.bxml.atom;

import static org.geoserver.gss.internal.atom.Atom.author;
import static org.geoserver.gss.internal.atom.Atom.category;
import static org.geoserver.gss.internal.atom.Atom.contributor;
import static org.geoserver.gss.internal.atom.Atom.generator;
import static org.geoserver.gss.internal.atom.Atom.id;
import static org.geoserver.gss.internal.atom.Atom.link;
import static org.geoserver.gss.internal.atom.Atom.rights;
import static org.geoserver.gss.internal.atom.Atom.subtitle;
import static org.geoserver.gss.internal.atom.Atom.title;
import static org.geoserver.gss.internal.atom.Atom.updated;

import java.io.IOException;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.geoserver.gss.internal.atom.Atom;
import org.geoserver.gss.internal.atom.CategoryImpl;
import org.geoserver.gss.internal.atom.EntryImpl;
import org.geoserver.gss.internal.atom.FeedImpl;
import org.geoserver.gss.internal.atom.GeneratorImpl;
import org.geoserver.gss.internal.atom.GeoRSS;
import org.geoserver.gss.internal.atom.LinkImpl;
import org.geoserver.gss.internal.atom.PersonImpl;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.geotools.gml3.GML;
import org.gvsig.bxml.stream.BxmlStreamWriter;

public class FeedEncoder extends AbstractAtomEncoder<FeedImpl> {

    public FeedEncoder() {
        //
    }

    @Override
    public void encode(final FeedImpl feed, final BxmlStreamWriter w) throws IOException {

        w.writeStartDocument();

        w.setPrefix("", Atom.NAMESPACE);
        w.setPrefix("georss", GeoRSS.NAMESPACE);
        w.setPrefix("gss", "http://www.opengis.net/gss/1.0");
        w.setPrefix("ows", "http://www.opengis.net/ows/1.1");
        w.setPrefix("fes", "http://www.opengis.net/ogc");
        w.setPrefix("xlink", "http://www.w3.org/1999/xlink");
        w.setPrefix("wfs", WFS.NAMESPACE);
        w.setPrefix("gml", GML.NAMESPACE);

        w.writeStartElement(Atom.NAMESPACE, Atom.feed.getLocalPart());

        w.writeDefaultNamespace(Atom.NAMESPACE);
        // w.writeNamespace("atom", Atom.NAMESPACE);
        w.writeNamespace("georss", GeoRSS.NAMESPACE);
        w.writeNamespace("gss", "http://www.opengis.net/gss/1.0");
        w.writeNamespace("ows", "http://www.opengis.net/ows/1.1");
        w.writeNamespace("fes", "http://www.opengis.net/ogc");
        w.writeNamespace("xlink", "http://www.w3.org/1999/xlink");
        w.writeNamespace("wfs", WFS.NAMESPACE);
        w.writeNamespace("gml", GML.NAMESPACE);

        if (feed.getStartPosition() != null) {
            long startPos = feed.getStartPosition().longValue();
            w.writeStartAttribute("", "startPosition");
            w.writeValue(startPos);
        }
        if (feed.getMaxEntries() != null) {
            w.writeStartAttribute("", "maxEntries");
            w.writeValue(feed.getMaxEntries().longValue());
        }
        w.writeEndAttributes();

        // <xs:element name="author" type="atom:personType" minOccurs="0" maxOccurs="unbounded" />
        // <xs:element name="category" type="atom:categoryType" minOccurs="0" maxOccurs="unbounded"
        // />
        // <xs:element name="contributor" type="atom:personType" minOccurs="0" maxOccurs="unbounded"
        // />
        // <xs:element name="generator" type="atom:generatorType" minOccurs="0" maxOccurs="1" />
        // <xs:element name="icon" type="atom:iconType" minOccurs="0" maxOccurs="1" />
        // <xs:element name="id" type="atom:idType" minOccurs="1" maxOccurs="1" />
        // <xs:element name="link" type="atom:linkType" minOccurs="0" maxOccurs="unbounded" />
        // <xs:element name="logo" type="atom:logoType" minOccurs="0" maxOccurs="1" />
        // <xs:element name="rights" type="atom:textType" minOccurs="0" maxOccurs="1" />
        // <xs:element name="subtitle" type="atom:textType" minOccurs="0" maxOccurs="1" />
        // <xs:element name="title" type="atom:textType" minOccurs="1" maxOccurs="1" />
        // <xs:element name="updated" type="atom:dateTimeType" minOccurs="1" maxOccurs="1" />

        element(w, id, false, feed.getId());
        element(w, title, false, feed.getTitle());
        element(w, subtitle, false, feed.getSubtitle());
        element(w, updated, false, feed.getUpdated());

        for (PersonImpl eauthor : feed.getAuthor()) {
            final QName personElem = author;
            person(w, eauthor, personElem);
        }

        for (PersonImpl econtributor : feed.getContributor()) {
            final QName personElem = contributor;
            person(w, econtributor, personElem);
        }

        for (CategoryImpl cat : feed.getCategory()) {
            element(w, category, true, null, "term", cat.getTerm(), "scheme", cat.getScheme());
        }

        GeneratorImpl fgenerator = feed.getGenerator();
        if (null != fgenerator) {
            element(w, generator, false, fgenerator.getValue(), "version", fgenerator.getVersion(),
                    "uri", fgenerator.getUri());
        }

        element(w, rights, false, feed.getRights());

        for (LinkImpl elink : feed.getLink()) {
            element(w, link, true, null, "rel", elink.getRel(), "href", elink.getHref());
        }

        // Icon?
        // Logo?

        final Iterator<EntryImpl> entries = feed.getEntry();
        final EntryEncoder entryEncoder = new EntryEncoder();
        EntryImpl entry;
        for (; entries.hasNext();) {
            entry = entries.next();
            entryEncoder.encode(entry, w);
        }

        // final BlockingQueue<EntryImpl> queue = new LinkedBlockingQueue<EntryImpl>(10);
        // final ExecutorService executorService = Executors.newSingleThreadExecutor();
        // final Future<?> readTask = executorService.submit(new Runnable() {
        // @Override
        // public void run() {
        // final BlockingQueue<EntryImpl> pool = queue;
        // final Iterator<EntryImpl> iterator = entries;
        // EntryImpl entry;
        // while (iterator.hasNext()) {
        // entry = iterator.next();
        // try {
        // pool.put(entry);
        // } catch (InterruptedException e) {
        // break;
        // }
        // }
        // }
        // });
        //
        // try {
        // while (!readTask.isDone()) {
        // entry = queue.take();
        // if (entry == null) {
        // break;
        // }
        // entryEncoder.encode(entry);
        // w.flush();
        // }
        // } catch (IOException e) {
        // readTask.cancel(true);
        // throw e;
        // } catch (Throwable t) {
        // readTask.cancel(true);
        // Throwables.propagate(t);
        // }

        w.writeEndElement();
        w.writeEndDocument();

    }
}
