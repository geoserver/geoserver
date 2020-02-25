/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver;

import com.thoughtworks.xstream.XStream;

/**
 * Can be used to encode AtomLink with X-Stream. Not using annotations because x-stream
 * configuration is different for XML and JSON (the latter not using namespaces), instead providing
 * static methods for this purpose.
 *
 * @author Niels Charlier
 */
public class AtomLink {

    @SuppressWarnings("unused")
    private final String NAMESPACE = "http://www.w3.org/2005/Atom";

    private String href;
    private String rel;
    private String type;

    public AtomLink(String href, String rel, String type) {
        this.href = href;
        this.rel = rel;
        this.type = type;
    }

    public String getHref() {
        return href;
    }

    public String getRel() {
        return rel;
    }

    public String getType() {
        return type;
    }

    /** Configure x-stream for XML encoding of link */
    public static void configureXML(XStream xStream) {
        xStream.alias("atom:link", AtomLink.class);
        xStream.useAttributeFor(AtomLink.class, "href");
        xStream.useAttributeFor(AtomLink.class, "rel");
        xStream.useAttributeFor(AtomLink.class, "type");
        xStream.aliasAttribute(AtomLink.class, "NAMESPACE", "xmlns:atom");
    }

    /** Configure x-stream for JSON encoding of link */
    public static void configureJSON(XStream xStream) {
        xStream.alias("link", AtomLink.class);
        xStream.omitField(AtomLink.class, "NAMESPACE");
    }
}
