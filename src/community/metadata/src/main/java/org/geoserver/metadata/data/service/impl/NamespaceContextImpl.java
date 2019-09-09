/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public class NamespaceContextImpl implements NamespaceContext {

    private Map<String, String> prefix2Uri = new HashMap<String, String>();
    private Map<String, String> uri2Prefix = new HashMap<String, String>();

    public void register(String prefix, String uri) {
        prefix2Uri.put(prefix, uri);
        uri2Prefix.put(uri, prefix);
    }

    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            return prefix2Uri.get(XMLConstants.DEFAULT_NS_PREFIX);
        } else {
            return prefix2Uri.get(prefix);
        }
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return uri2Prefix.get(namespaceURI);
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        return prefix2Uri.keySet().iterator();
    }
}
