/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.ows.NestedKvpParser;

public class QNameNestedKvpParser extends NestedKvpParser {

    QNameKvpParser delegate;

    public QNameNestedKvpParser(String key, Catalog catalog) {
        super(key, QName.class);
        delegate = new QNameKvpParser(key, catalog);
    }

    @Override
    protected Object parseToken(String token) throws Exception {
        return delegate.parseToken(token);
    }
}
