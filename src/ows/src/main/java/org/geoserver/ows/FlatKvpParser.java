/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.ows.util.KvpUtils;

/**
 * A kvp parser which parses a value consisting of tokens in a flat list.
 *
 * <p>A value in flat form is a list of tokens separated by a single delimiter. The default
 * delimiter is a comma ( , ). Example:
 *
 * <pre><code>
 *         key=token1,token2,...,tokenN
 * </code></pre>
 *
 * <p>Upon processing of each token, the token is parsed into an instance of {@link #getBinding()}.
 * Subclasses should override the method {@link #parseToken(String)}.
 *
 * <p>By default, the {@link #parse(String)} method returns an list which contains instances of
 * {@link #getBinding()}. The {@link #parse(List)} method may be overidden to return a differnt type
 * of object.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class FlatKvpParser extends KvpParser {
    /** the delimiter to use to seperate tokens */
    final String delimiter;

    /**
     * Constructs the flat kvp parser specifying the key and class binding.
     *
     * @param key The key to bind to.
     * @param binding The class of each token in the value.
     */
    public FlatKvpParser(String key, Class binding) {
        this(key, binding, ",");
    }

    /**
     * Constructs the flat kvp parser specifying the key, class binding, and token delimiter.
     *
     * @param key The key to bind to.
     * @param binding The class of each token in the value.
     * @param delimiter The delimiter used to seperate tokens
     */
    public FlatKvpParser(String key, Class binding, String delimiter) {
        super(key, binding);

        this.delimiter = delimiter;
    }

    /** Tokenizes the value and delegates to {@link #parseToken(String)} to parse each token. */
    public final Object parse(String value) throws Exception {
        List tokens = KvpUtils.readFlat(value, delimiter);
        List parsed = new ArrayList(tokens.size());
        final int size = tokens.size();
        for (int i = 0; i < size; i++) {
            String token = (String) tokens.get(i);
            parsed.add(parseToken(token));
        }

        return parse(parsed);
    }

    /**
     * Parses the token into an instance of {@link #getBinding()}.
     *
     * <p>Subclasses should override this method, the default implementation just returns token
     * passed in.
     *
     * @param token Part of the value being parsed.
     * @return The token parsed into an object.
     */
    protected Object parseToken(String token) throws Exception {
        return token;
    }

    /**
     * Parses the parsed tokens into a final representation.
     *
     * <p>Subclasses may choose to override this method. The default implementation just return the
     * array passed in.
     *
     * @param values The parsed tokens, each value is an instance of {@link #getBinding()}.
     * @return The final object.
     */
    protected Object parse(List values) throws Exception {
        return values;
    }
}
