/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.ows.FlatKvpParser;
import org.geoserver.wfs.WFSException;

/**
 * Abstract kvp parser for parsing qualified names of the form "([prefix:]local)+".
 *
 * <p>This parser will parse strings of the above format into a list of {@link
 * javax.xml.namespace.QName}
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class QNameKvpParser extends FlatKvpParser {
    /** catalog for namespace lookups. */
    protected Catalog catalog;

    private final boolean strict;

    public QNameKvpParser(String key, Catalog catalog) {
        this(key, catalog, true);
    }

    /**
     * @param key the key this kvp parser parses the value for
     * @param catalog the catalog where to check if the namespace given by the qualified name prefix
     *     exists
     * @param strict if {@code true} and the qname being parsed contains a namespace prefix that
     *     does not match a namespace from {@code catalog}, an exception will be thrown, otherwise a
     *     {@code QName} with prefix and localName but without namespace will be returned.
     */
    protected QNameKvpParser(String key, Catalog catalog, boolean strict) {
        super(key, QName.class);
        this.catalog = catalog;
        this.strict = strict;
    }

    /**
     * Parses the token representing a type name, ( <prefix>:<local>, or <local> ) into a {@link
     * QName }.
     *
     * <p>If the latter form is supplied the QName is given the default namespace as specified in
     * the catalog.
     */
    protected Object parseToken(String token) throws Exception {
        int i = token.indexOf(':');

        if (i != -1) {
            String prefix = token.substring(0, i);
            String local = token.substring(i + 1);

            String uri = null;
            if (prefix != null && !"".equals(prefix)) {
                final NamespaceInfo namespace = catalog.getNamespaceByPrefix(prefix);
                if (strict && namespace == null) {
                    throw new WFSException("Unknown namespace [" + prefix + "]");
                }
                uri = namespace == null ? null : namespace.getURI();
            }

            return new QName(uri, local, prefix);
        } else {
            /*
            String uri = catalog.getDefaultNamespace().getURI();
            String prefix = catalog.getDefaultNamespace().getPrefix();
            String local = token;

            return new QName(uri, local, prefix);
            */
            return new QName(token);
        }
    }
}
