/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions.aggregate;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.geotools.util.logging.Logging;

/** Factory class for {@link AggregationOp} instances. */
class AggregateOpFactory {
    enum OpType {
        JOIN,
        MIN,
        MAX,
        AVG,
        UNIQUE
    }

    private static final Logger LOGGER = Logging.getLogger(AggregateOpFactory.class);

    /**
     * Create an {@link AggregationOp} parsing the string value holding its name.
     *
     * @param rawType
     * @return
     */
    static AggregationOp createOperation(String rawType) {
        int endNameIdx = rawType.indexOf("(");
        String agName;
        String param = null;
        if (endNameIdx != -1) {
            param = extractParam(rawType);
            agName = rawType.substring(0, endNameIdx).toUpperCase();
        } else {
            agName = rawType;
        }
        OpType opType = validateOpAndGet(agName);
        return getOp(opType, param);
    }

    private static AggregationOp getOp(OpType opType, String params) {
        AggregationOp op;
        switch (opType) {
            case AVG:
                op = new AvgOp();
                break;
            case MAX:
                op = new MaxOp();
                break;
            case MIN:
                op = new MinOp();
                break;
            case UNIQUE:
                op = new UniqueOp();
                break;
            default:
                op = new JoinOp(params);
                break;
        }
        return op;
    }

    private static OpType validateOpAndGet(String agName) {
        try {
            return OpType.valueOf(agName);
        } catch (EnumConstantNotPresentException e) {
            LOGGER.log(
                    Level.SEVERE,
                    "The aggregate type specified in the aggregate function was not recognized",
                    e);
            throw new UnsupportedOperationException(
                    "The aggregate type specified in the aggregate function was not recognized");
        }
    }

    /**
     * Extract the param from the string aggregate type. Some aggregation might have params eg.
     * JOIN(,).
     *
     * @param aggregateValue the aggregate type value.
     * @return the param value if found, null otherwise.
     */
    private static String extractParam(String aggregateValue) {
        StringBuilder sb = new StringBuilder("");
        char[] chars = aggregateValue.toCharArray();
        boolean startParam = false;
        for (char c : chars) {
            if (startParam && c != ')') sb.append(c);
            else if (c == '(') startParam = true;
        }
        String value = sb.toString();
        if (StringUtils.isBlank(value)) {
            LOGGER.fine(() -> "No param found...");
            return null;
        }
        return value;
    }
}
