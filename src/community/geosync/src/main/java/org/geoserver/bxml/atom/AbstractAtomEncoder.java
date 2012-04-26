package org.geoserver.bxml.atom;

import static org.geoserver.gss.internal.atom.Atom.email;
import static org.geoserver.gss.internal.atom.Atom.name;
import static org.geoserver.gss.internal.atom.Atom.uri;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.geoserver.bxml.AbstractEncoder;
import org.geoserver.gss.internal.atom.PersonImpl;
import org.gvsig.bxml.stream.BxmlStreamWriter;

public abstract class AbstractAtomEncoder<T> extends AbstractEncoder<T> {

    protected void person(BxmlStreamWriter w, PersonImpl person, QName personElem) {
        try {
            w.writeStartElement(personElem.getNamespaceURI(), personElem.getLocalPart());
            {
                element(w, name, false, person.getName(), true);
                element(w, uri, false, person.getUri(), true);
                element(w, email, false, person.getEmail(), true);
            }
            w.writeEndElement();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
