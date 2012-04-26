package org.geoserver.bxml.atom;

import org.geoserver.bxml.BxmlTestSupport;
import org.geoserver.gss.internal.atom.CategoryImpl;
import org.geoserver.gss.internal.atom.GeneratorImpl;
import org.geoserver.gss.internal.atom.LinkImpl;
import org.geoserver.gss.internal.atom.PersonImpl;

public abstract class AtomTestSupport extends BxmlTestSupport {

    protected void testPerson(String name, String email, String uri, PersonImpl person) {
        assertEquals(name, person.getName());
        assertEquals(email, person.getEmail());
        assertEquals(uri, person.getUri());
    }

    protected void testCategory(String term, String scheme, CategoryImpl category) {
        assertEquals(term, category.getTerm());
        assertEquals(scheme, category.getScheme());
    }

    protected void testGenerator(String value, String uri, String version, GeneratorImpl generator) {
        assertEquals(value, generator.getValue());
        assertEquals(uri, generator.getUri());
        assertEquals(version, generator.getVersion());
    }

    protected void testLink(String href, String rel, String type, String hreflang, String title,
            Long length, LinkImpl link) {
        assertEquals(href, link.getHref());
        assertEquals(rel, link.getRel());
        assertEquals(type, link.getType());
        assertEquals(hreflang, link.getHreflang());
        assertEquals(title, link.getTitle());
        assertEquals(length, link.getLength());

    }
}
