package org.geoserver.bxml.atom;

import static org.geoserver.gss.internal.atom.Atom.author;
import static org.geoserver.gss.internal.atom.Atom.category;
import static org.geoserver.gss.internal.atom.Atom.contributor;
import static org.geoserver.gss.internal.atom.Atom.entry;
import static org.geoserver.gss.internal.atom.Atom.id;
import static org.geoserver.gss.internal.atom.Atom.link;
import static org.geoserver.gss.internal.atom.Atom.summary;
import static org.geoserver.gss.internal.atom.Atom.title;
import static org.geoserver.gss.internal.atom.Atom.updated;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.geoserver.bxml.gml_3_1.EnvelopeEncoder;
import org.geoserver.bxml.gml_3_1.GeometryEncoder;
import org.geoserver.bxml.wfs_1_1.DeleteElementTypeEncoder;
import org.geoserver.bxml.wfs_1_1.InsertElementTypeEncoder;
import org.geoserver.bxml.wfs_1_1.UpdateElementTypeEncoder;
import org.geoserver.gss.internal.atom.CategoryImpl;
import org.geoserver.gss.internal.atom.ContentImpl;
import org.geoserver.gss.internal.atom.EntryImpl;
import org.geoserver.gss.internal.atom.GeoRSS;
import org.geoserver.gss.internal.atom.LinkImpl;
import org.geoserver.gss.internal.atom.PersonImpl;
import org.gvsig.bxml.stream.BxmlStreamWriter;
import org.opengis.geometry.BoundingBox;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Encodes an {@link EntryImpl} as {@code atom:entry} element.
 * <p>
 * {@link ContentEncoder#findEncoderFor(Object)} is used to locate an appropriate encoder depending
 * on the entry's {@link EntryImpl#getContent() content}
 * </p>
 * 
 * @see ContentEncoder
 * @see InsertElementTypeEncoder
 * @see UpdateElementTypeEncoder
 * @see DeleteElementTypeEncoder
 */
public class EntryEncoder extends AbstractAtomEncoder<EntryImpl> {

    private final EnvelopeEncoder envelopeEncoder;

    private final GeometryEncoder geometryEncoder;

    public EntryEncoder() {
        envelopeEncoder = new EnvelopeEncoder();
        geometryEncoder = new GeometryEncoder();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void encode(final EntryImpl e, final BxmlStreamWriter w) throws IOException {
        startElement(w, entry);
        {
            element(w, title, false, e.getTitle());
            element(w, summary, false, e.getSummary(), true);
            element(w, id, false, e.getId());
            element(w, updated, false, e.getUpdated(), true);

            for (PersonImpl eauthor : e.getAuthor()) {
                final QName personElem = author;
                person(w, eauthor, personElem);
            }

            for (PersonImpl econtributor : e.getContributor()) {
                final QName personElem = contributor;
                person(w, econtributor, personElem);
            }

            for (CategoryImpl cat : e.getCategory()) {
                element(w, category, true, null, true, true, "term", cat.getTerm(), "scheme",
                        cat.getScheme());
            }

            for (LinkImpl elink : e.getLink()) {
                element(w, link, true, null, "rel", elink.getRel(), "href", elink.getHref());
            }

            final ContentImpl econtent = e.getContent();
            if (null != econtent) {
                ContentEncoder contentEncoder = new ContentEncoder();
                contentEncoder.encode(econtent, w);
            }

            Object georssWhere = e.getWhere();
            if (georssWhere != null) {
                startElement(w, GeoRSS.where);
                {
                    if (georssWhere instanceof BoundingBox) {
                        envelopeEncoder.encode((BoundingBox) georssWhere, w);
                    } else if (georssWhere instanceof Geometry) {
                        geometryEncoder.encode((Geometry) georssWhere, w);
                    }
                }
                endElement(w);
            }
        }
        endElement(w);
    }
}
