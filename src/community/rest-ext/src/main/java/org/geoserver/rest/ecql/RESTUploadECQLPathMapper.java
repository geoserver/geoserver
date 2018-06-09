/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.ecql;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.rest.util.RESTUploadPathMapperImpl;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;

/**
 * RESTUploadPathMapper implementation which executes a CQL expression on the input file name for
 * remapping it
 *
 * @author Nicola Lagomarsini Geosolutions S.A.S.
 */
public class RESTUploadECQLPathMapper extends RESTUploadPathMapperImpl
        implements ExtensionPriority {

    private static Logger LOGGER =
            Logging.getLogger("org.geoserver.rest.cql.RESTUploadCQLPathMapper");

    /** MetadataMap key associated to the expression value */
    public static final String EXPRESSION_KEY = "expression";

    public static final String PATH = "path";

    public static final String NAME = "name";

    /** Feature type used for creating the input feature associated to the item path */
    private static SimpleFeatureType typePath;

    /** Feature type used for creating the input feature associated to the item name */
    private static SimpleFeatureType typeName;

    /** Feature type used for creating the input feature associated to the item name and path */
    private static SimpleFeatureType typeAll;

    private static FilterAttributeExtractor extractor;

    // Feature type initialization
    static {
        try {
            typePath = DataUtilities.createType("type", PATH + ":string");
            typeName = DataUtilities.createType("type", NAME + ":string");
            typeAll = DataUtilities.createType("type", PATH + ":string," + NAME + ":string");
        } catch (SchemaException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

        // Static initialization of an Attribute extractor for checking the attributes inside the
        // ECQL expression
        extractor = new FilterAttributeExtractor();
    }

    public RESTUploadECQLPathMapper(Catalog catalog) {
        super(catalog);
    }

    public void mapItemPath(
            String workspace,
            String store,
            Map<String, String> storeParams,
            StringBuilder itemPath,
            String itemName)
            throws IOException {

        // expression to use for remapping
        Expression expression = null;

        // Extraction of the ECQL expression from the metadata map
        try {
            expression = getExpression(workspace, store, catalog);
        } catch (CQLException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
        // No expression found, so nothing is executed
        if (expression == null) {
            return;
        }

        // extraction of the attributes of the ECQL expression
        expression.accept(extractor, null);
        List<String> attributes = Arrays.asList(extractor.getAttributeNames());

        // Feature associated to the input path
        SimpleFeature feature = null;

        if (attributes != null) {
            if (attributes.contains(PATH)) {
                if (attributes.contains(NAME)) {
                    feature =
                            SimpleFeatureBuilder.build(
                                    typeAll, new Object[] {itemPath.toString(), itemName}, null);
                } else {
                    feature =
                            SimpleFeatureBuilder.build(
                                    typePath, new Object[] {itemPath.toString()}, null);
                }
            } else if (attributes.contains(NAME)) {
                feature = SimpleFeatureBuilder.build(typeName, new Object[] {itemName}, null);
            }
        } else {
            feature =
                    SimpleFeatureBuilder.build(
                            typeAll, new Object[] {itemPath.toString(), itemName}, null);
        }

        if (feature == null) {
            return;
        }

        // Perform Regular Expression match
        String newPath = expression.evaluate(feature, String.class);

        // If nothing is returned, then the initial path is left untouched
        if (newPath == null || newPath.isEmpty()) {
            return;
        }

        // Removal of the old input path
        itemPath.setLength(0);

        // Setting of the new item path
        itemPath.append(newPath);
    }

    @Override
    public int getPriority() {
        return 1;
    }

    public static Expression getExpression(String workspaceName, String storeName, Catalog catalog)
            throws CQLException {
        String expression = RESTUtils.getItem(workspaceName, storeName, catalog, EXPRESSION_KEY);
        // returns the expression if not null
        if (expression != null) {
            return ECQL.toExpression(expression);
        }
        return null;
    }
}
