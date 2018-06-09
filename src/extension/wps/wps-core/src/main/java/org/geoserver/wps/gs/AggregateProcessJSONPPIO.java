/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONObject;
import org.geoserver.wps.ppio.CDataPPIO;
import org.geotools.process.vector.AggregateProcess;

/**
 * Provides a JSON output for the aggregate process result. The result are encoded in a tabular
 * format very similar to the output of SQL query.
 *
 * <p>The tabular data is the composition of the group by attributes values and the aggregation
 * functions results. Both of these values appear in the order they are declared, the group by
 * values appear first and the aggregation values after. If there is no group by attributes, only
 * the aggregations values appear.
 *
 * <p>Follow some examples:
 *
 * <p>The max and min energy consumption:
 *
 * <pre>
 * <code>{
 *   "AggregationAttribute": "energy_consumption",
 *   "AggregationFunctions": ["Max", "Min"],
 *   "GroupByAttributes": ["building_type"],
 *   "AggregationResults": [
 *     ["School", 500.0, 80.0],
 *     ["Fabric", 850.0, 120.0]
 *   ]
 * }</code>
 * </pre>
 *
 * <p>The max and min energy consumption per building type and energy type:
 *
 * <pre>
 * <code>{
 *   "AggregationAttribute": "energy_consumption",
 *   "AggregationFunctions": ["Max", "Min"],
 *   "GroupByAttributes": ["building_type", "energy_type"],
 *   "AggregationResults": [
 *     ["School", "Nuclear", 500.0, 220.0],
 *     ["School", "Wind", 200.0, 120.0],
 *     ["Fabric", "Nuclear", 230.0, 80.0],
 *     ["Fabric", "Fuel", 850.0, 370.0]
 *   ]
 * }</code>
 * </pre>
 */
public class AggregateProcessJSONPPIO extends CDataPPIO {

    protected AggregateProcessJSONPPIO() {
        super(AggregateProcess.Results.class, AggregateProcess.Results.class, "application/json");
    }

    @Override
    public void encode(Object value, OutputStream output) throws Exception {
        AggregateProcess.Results processResult = (AggregateProcess.Results) value;
        Map<Object, Object> json = new HashMap<>();
        // we encode the common parts regardless of the presence of group by attributes
        json.put("AggregationAttribute", processResult.getAggregateAttribute());
        json.put("AggregationFunctions", extractAggregateFunctionsNames(processResult));
        if (processResult.getGroupByAttributes() == null
                || processResult.getGroupByAttributes().isEmpty()) {
            // if there is no group by attributes we only to encode the aggregations function
            // results
            json.put("GroupByAttributes", new String[0]);
            json.put("AggregationResults", new Number[][] {encodeSimpleResult(processResult)});
        } else {
            // there is group by values so we need to encode all the grouped results
            json.put("GroupByAttributes", processResult.getGroupByAttributes().toArray());
            json.put("AggregationResults", processResult.getGroupByResult().toArray());
        }
        output.write(JSONObject.fromObject(json).toString().getBytes());
    }

    /**
     * Helper method that encodes the result of an aggregator process when there is no group by
     * attributes. We encode the value of each aggregation function producing an output very similar
     * of an SQL query result.
     *
     * @param processResult the result of the aggregator process
     * @return aggregation functions result values
     */
    private Number[] encodeSimpleResult(AggregateProcess.Results processResult) {
        return processResult
                .getFunctions()
                .stream()
                .map(function -> processResult.getResults().get(function))
                .toArray(Number[]::new);
    }

    /**
     * Helper that extract the name of the aggregation functions.
     *
     * @param result the result of the aggregator process
     * @return an array that contain the aggregation functions names
     */
    private String[] extractAggregateFunctionsNames(AggregateProcess.Results result) {
        return result.getFunctions().stream().map(Enum::name).toArray(String[]::new);
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        throw new UnsupportedOperationException("JSON parsing is not supported");
    }

    @Override
    public Object decode(String input) throws Exception {
        throw new UnsupportedOperationException("JSON parsing is not supported");
    }

    @Override
    public final String getFileExtension() {
        return "json";
    }
}
