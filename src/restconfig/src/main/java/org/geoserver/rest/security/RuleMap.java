/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Just a custom subclass of Map so that we can associate a custom message converter to it
 *
 * @param <K>
 * @param <V>
 */
@SuppressWarnings("serial")
public class RuleMap<K, V> extends LinkedHashMap<K, V> {

    public RuleMap() {
        super();
    }

    public RuleMap(Map<K, V> source) {
        super(source);
    }
}
