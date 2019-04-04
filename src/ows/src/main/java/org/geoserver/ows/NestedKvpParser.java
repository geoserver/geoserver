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
 * A kvp parser which parses a value consisting of tokens in a nested list.
 *
 * <p>A value in nested form is a number of lists of tokens, seperated by an outer delimeter. The
 * tokens in each list are serarated by an inner delimiter The default outer delimiter is are
 * parentheses ( '()' ) , the default inner delimter is a comma ( ',' ). Example:
 *
 * <pre><code>
 *         key=(token11,token12,...,token1N)(token21,token22,...,token2N)(...)(tokenM1,tokenM2,...,tokenMN)
 *
 * where N = number of tokens in each set, and M = number of sets.
 *         </code>
 *  </pre>
 *
 * <p>Upon processing of each token, the token is parsed into an instance of {@link #getBinding()}.
 * Subclasses should override the method {@link #parseToken(String)}.
 *
 * <p>By default, the {@link #parse(String)} method returns a list of lists. Each of which contains
 * instances of {@link #getBinding()}. The {@link #parseTokenSet(List)} method may be overidden to
 * return a differnt type of object.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *     <p>TODO: add a method to convert return value as to not force returning a list
 */
public class NestedKvpParser extends KvpParser {
    /**
     * Constructs the nested kvp parser specifying the key and class binding.
     *
     * @param key The key to bind to.
     * @param binding The class of each token in the value.
     */
    public NestedKvpParser(String key, Class binding) {
        super(key, binding);
    }

    /** Tokenizes the value and delegates to {@link #parseToken(String)} to parse each token. */
    public Object parse(String value) throws Exception {
        List tokenSets = KvpUtils.readNested(value);

        for (int i = 0; i < tokenSets.size(); i++) {
            List tokens = (List) tokenSets.get(i);
            List parsed = new ArrayList(tokens.size());

            for (int j = 0; j < tokens.size(); j++) {
                String token = (String) tokens.get(j);
                parsed.add(parseToken(token));
            }

            tokenSets.set(i, parseTokenSet(parsed));
        }

        return parse(tokenSets);
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
     * Parses the set of tokens into a final represetnation.
     *
     * <p>Subclasses may choose to override this method. The default implementation just return the
     * list passed in.
     *
     * @param tokenSet The parsed tokens, each value is an instance of {@link #getBinding()}.
     * @return The final object.
     */
    protected Object parseTokenSet(List tokenSet) throws Exception {
        return tokenSet;
    }

    /**
     * Parses the set of token sets into a final representation.
     *
     * <p>Subclasses may choose to override this method. The default implementation just return the
     * list passed in.
     *
     * @param values The parsed token sets, each value is an instance of the class returned from
     *     {@link #parseTokenSet(List)}.
     * @return The final object.
     */
    protected Object parse(List values) throws Exception {
        return values;
    }
}
