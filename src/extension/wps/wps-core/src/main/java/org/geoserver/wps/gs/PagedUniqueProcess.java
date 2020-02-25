/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;

/**
 * A WPS process to retrieve unique field values from a layer on Geoserver catalog. Requires a valid
 * layer name and a field name to extract the unique values. It accepts sorting and paging
 * parameters.
 *
 * @author Cesar Martinez Izquierdo
 * @author Sandro Salari
 * @author Mauro Bartolomeoli
 */
@DescribeProcess(
    title = "PagedUnique",
    description =
            "Gets the list of unique values for the given featurecollection on a specified field, allows optional paging"
)
public class PagedUniqueProcess implements GeoServerProcess {

    /** The LOGGER. */
    private static final Logger LOGGER = Logging.getLogger(PagedUniqueProcess.class);

    private final FilterFactory FF = CommonFactoryFinder.getFilterFactory2();

    public static final class Results {
        private String featureTypeName;
        private String fieldName;
        private int size;
        private List<?> values;

        public Results(String featureTypeName, String fieldName, int size, List<?> values) {
            super();
            this.featureTypeName = featureTypeName;
            this.fieldName = fieldName;
            this.size = size;
            this.values = values;
        }

        public String getFeatureTypeName() {
            return featureTypeName;
        }

        public String getFieldName() {
            return fieldName;
        }

        public int getSize() {
            return size;
        }

        public List<?> getValues() {
            return values;
        }
    }

    @DescribeResult(name = "result", type = Results.class, description = "List of values")
    public Results execute(
            @DescribeParameter(
                        name = "features",
                        min = 1,
                        max = 1,
                        description = "Layer from which field values should be retrieved"
                    )
                    SimpleFeatureCollection features,
            @DescribeParameter(
                        name = "fieldName",
                        min = 1,
                        max = 1,
                        description = "Field from which the values should be retrieved"
                    )
                    String fieldName,
            @DescribeParameter(
                        name = "startIndex",
                        min = 0,
                        max = 1,
                        description = "The index of the first feature to retrieve"
                    )
                    Integer startIndex,
            @DescribeParameter(
                        name = "maxFeatures",
                        min = 0,
                        max = 1,
                        description = "The maximum numbers of features to fetch"
                    )
                    Integer maxFeatures)
            throws IOException, CQLException {

        // initial checks on params
        if (features == null) {
            throw new IllegalArgumentException("features param cannot be null");
        }
        if (fieldName == null || fieldName.isEmpty()) {
            throw new IllegalArgumentException("Empty or null fieldName provided!");
        }
        SimpleFeatureType featureType = features.getSchema();
        String featureTypeName = featureType.getTypeName();
        LOGGER.fine(
                "PagedUnique process called on resource: "
                        + featureTypeName
                        + " - field: "
                        + fieldName);

        UniqueVisitor visitor =
                new UniqueVisitor(FF.property(fieldName)) {
                    @Override
                    public boolean hasLimits() {
                        // force usage of visitor limits, also for size extraction "query"
                        return true;
                    }
                };

        Integer listSize = 0;
        List<String> list = new ArrayList<String>();

        try {
            // counts total elements
            features.accepts(visitor, null);
            if (visitor.getResult() == null || visitor.getResult().toList() == null) {
                listSize = 0;
                list = new ArrayList<String>(0);
            } else {
                listSize = visitor.getResult().toList().size();
                if (maxFeatures == null || maxFeatures > listSize) {
                    maxFeatures = listSize;
                }
                // Reset visitor and set pagination
                visitor.reset();
                if (startIndex != null) {
                    visitor.setStartIndex(startIndex);
                }
                if (maxFeatures != null) {
                    visitor.setMaxFeatures(maxFeatures);
                }
                visitor.setPreserveOrder(true);

                features.accepts(visitor, null);
                if (visitor.getResult() == null || visitor.getResult().toList() == null) {
                    list = new ArrayList<String>(0);
                } else {
                    list = visitor.getResult().toList();
                }
            }
            return new Results(featureTypeName, fieldName, listSize, list);
        } catch (Exception e) {
            throw new ProcessException("Error extracting unique values", e);
        }
    }
}
