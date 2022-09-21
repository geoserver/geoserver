/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions.aggregate;

import java.util.logging.Logger;
import org.geoserver.featurestemplating.expressions.TemplateCQLManager;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.capability.FunctionName;
import org.xml.sax.helpers.NamespaceSupport;

/** An abstraction for a Function accepting as a parameter a cql as string. */
public abstract class StringCQLFunction extends FunctionExpressionImpl {

    private NamespaceSupport namespaceSupport;

    private static final Logger LOGGER = Logging.getLogger(StringCQLFunction.class);

    protected StringCQLFunction(FunctionName name) {
        super(name);
    }

    /**
     * Sets the namespaces to this Function to be injected in the PropertyNames.
     *
     * @param namespaceSupport the namespaces to set.
     */
    public void setNamespaceSupport(NamespaceSupport namespaceSupport) {
        this.namespaceSupport = namespaceSupport;
    }

    /**
     * Convert the cql to a Filter taking care of injecting the Namespaces to every PropertyName in
     * it.
     *
     * @param cql the cql string.
     * @return the Filter.
     * @throws CQLException
     */
    protected Filter cqlToFilter(String cql) throws CQLException {
        LOGGER.fine(() -> "Parsing cql filter " + cql);
        TemplateCQLManager cqlManager = new TemplateCQLManager(cql, namespaceSupport);
        return cqlManager.getFilterFromString();
    }
}
