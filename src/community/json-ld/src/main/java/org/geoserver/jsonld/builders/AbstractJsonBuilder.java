/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.builders;

import java.io.IOException;
import org.geoserver.jsonld.JsonLdGenerator;
import org.geoserver.jsonld.builders.impl.JsonBuilderContext;
import org.geoserver.jsonld.expressions.JsonLdCQLManager;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.filter.Filter;
import org.xml.sax.helpers.NamespaceSupport;

/** Abstract implementation of {@link JsonBuilder} who groups some common attributes and methods. */
public abstract class AbstractJsonBuilder implements JsonBuilder {

    protected String key;

    protected Filter filter;

    protected int filterContextPos = 0;

    protected NamespaceSupport namespaces;

    public AbstractJsonBuilder(String key, NamespaceSupport namespaces) {
        this.key = key;
        this.namespaces = namespaces;
    }

    protected void writeKey(JsonLdGenerator writer) throws IOException {
        if (key != null && !key.equals("")) writer.writeFieldName(key);
        else throw new RuntimeException("Cannot write null key value");
    }

    protected boolean evaluateFilter(JsonBuilderContext context) {
        if (filter == null) return true;
        JsonBuilderContext evaluationContenxt = context;
        for (int i = 0; i < filterContextPos; i++) {
            evaluationContenxt = evaluationContenxt.getParent();
        }
        return filter.evaluate(evaluationContenxt.getCurrentObj());
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(String filter) throws CQLException {
        JsonLdCQLManager cqlManager = new JsonLdCQLManager(filter, namespaces);
        this.filter = cqlManager.getFilterFromString();
        this.filterContextPos = cqlManager.getContextPos();
    }

    public int getFilterContextPos() {
        return filterContextPos;
    }
}
