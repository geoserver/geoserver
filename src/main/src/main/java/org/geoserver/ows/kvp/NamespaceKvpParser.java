/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import javax.xml.XMLConstants;
import org.geoserver.ows.FlatKvpParser;
import org.geoserver.ows.KvpParser;
import org.geoserver.platform.ServiceException;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Parses a list of namespace declarations of the form {@code
 * <xmlns(foo=http://name.space1)[,xmlns(bar=http://name.space2)]+> } into a {@link
 * NamespaceSupport}. Using the {@link PrefixNamespaceSeparator#COMMA} it's also possible to handle
 * the WFS 2.0 suggested syntax, {@code
 * <xmlns(foo,http://name.space1)[,xmlns(bar,http://name.space2)]+> }
 *
 * @author groldan
 */
public class NamespaceKvpParser extends KvpParser {

    private final boolean useComma;

    public NamespaceKvpParser(String key) {
        this(key, false);
    }

    public NamespaceKvpParser(String key, boolean useComma) {
        super(key, NamespaceSupport.class);
        this.useComma = useComma;
    }

    /**
     * @param value a list of namespace declarations of the form {@code
     *     <xmlns(foo=http://name.space1)[,xmlns(bar=http://name.space2)]+> }
     */
    @SuppressWarnings("unchecked")
    @Override
    public NamespaceSupport parse(final String value) throws Exception {
        List<String> declarations;
        if (useComma) {
            String[] parts = value.split(",(?![^()]*+\\))");
            declarations = Arrays.asList(parts);
        } else {
            declarations = (List<String>) new FlatKvpParser("", String.class).parse(value);
        }
        NamespaceSupport ctx = new NamespaceSupport();

        String[] parts;
        String prefix;
        String uri;
        for (String decl : declarations) {
            decl = decl.trim();
            if (!decl.startsWith("xmlns(") || !decl.endsWith(")")) {
                throw new ServiceException(
                        "Illegal namespace declaration, "
                                + "should be of the form xmlns(<prefix>=<ns uri>): "
                                + decl,
                        ServiceException.INVALID_PARAMETER_VALUE,
                        getKey());
            }
            decl = decl.substring("xmlns(".length());
            decl = decl.substring(0, decl.length() - 1);
            String separator = useComma ? "," : "=";
            parts = decl.split(separator);
            if (parts.length == 1) {
                prefix = XMLConstants.DEFAULT_NS_PREFIX;
                uri = parts[0];
            } else if (parts.length == 2) {
                prefix = parts[0];
                uri = parts[1];
            } else {
                throw new ServiceException(
                        "Illegal namespace declaration, "
                                + "should be of the form prefix"
                                + separator
                                + "<namespace uri>: "
                                + decl,
                        ServiceException.INVALID_PARAMETER_VALUE,
                        getKey());
            }

            try {
                new URI(uri);
            } catch (URISyntaxException e) {
                throw new ServiceException(
                        "Illegal namespace declaration: " + decl,
                        ServiceException.INVALID_PARAMETER_VALUE,
                        getKey());
            }
            ctx.declarePrefix(prefix, uri);
        }

        return ctx;
    }
}
