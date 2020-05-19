/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.labeling;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.Request;
import org.geoserver.util.XCQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;

/**
 * GeoServer KVP parser for labeling attributes. Parses the RENDERLABEL vendor parameter into a list
 * of {@link AttributeLabelParameter} instances. <br>
 * Example:
 *
 * <pre>
 * RENDERLABEL=st:stations;my_id IN (1,2,4);my_id,name,area|st:other;my_id IN (7,8,20);my_id,name,area
 * </pre>
 */
public class AttributesGlobeKvpParser extends KvpParser {

    private static final Logger LOG = Logging.getLogger(AttributesGlobeKvpParser.class);

    public static final String RENDERLABEL = "RENDERLABEL";
    public static final String VALUE_SEPARATOR = ";";
    public static final String RULE_SEPARATOR = "|";
    public static final String ATTRIBUTE_NAME_SEPARATOR = ",";

    public AttributesGlobeKvpParser() {
        super(RENDERLABEL, List.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        LOG.fine(
                () ->
                        "Starting parsing "
                                + RENDERLABEL
                                + " vendor parameters for content: "
                                + value);
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(RENDERLABEL + " parameter value is empty or null");
        }
        // convert the rules string array to a List<AttributeLabelParameter>
        String[] rulesString = splitRulesString(value);
        return Arrays.stream(rulesString)
                .map(ruleStr -> parseRule(ruleStr))
                .collect(Collectors.toList());
    }

    /**
     * Parses a render label rule to a {@link AttributeLabelParameter} instance.
     *
     * <p>A rule has the form:
     *
     * <pre>
     * st:stations;my_id IN (1,2,4);my_id,name,area
     * </pre>
     *
     * @param ruleStr the rule string definition
     * @return the resulting {@link AttributeLabelParameter} instance
     */
    protected AttributeLabelParameter parseRule(String ruleStr) {
        String[] valuesArray = ruleStr.split(Pattern.quote(VALUE_SEPARATOR));
        if (valuesArray.length < 3 || StringUtils.isAnyBlank(valuesArray)) {
            throw new IllegalArgumentException(
                    "RENDERLABEL rule requires at least 3 parameters. Found: " + ruleStr);
        }
        String layerName = valuesArray[0];
        Filter filter = parseFilter(valuesArray[1]);
        Set<String> attributeNames = getAttributeNames(valuesArray[2]);
        return new AttributeLabelParameter(layerName, filter, attributeNames);
    }

    private Set<String> getAttributeNames(String attributesStr) {
        String[] attrsStr = attributesStr.split(Pattern.quote(ATTRIBUTE_NAME_SEPARATOR));
        Set<String> attributes =
                Arrays.stream(attrsStr)
                        .filter(att -> StringUtils.isNotBlank(att))
                        .map(att -> att.trim())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException(
                    "RENDERLABEL rule attributes set is empty. Found: " + attributesStr);
        }
        return attributes;
    }

    private Filter parseFilter(String filterStr) {
        try {
            return XCQL.toFilter(filterStr);
        } catch (CQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String[] splitRulesString(String value) {
        String[] rulesString = value.split(Pattern.quote(RULE_SEPARATOR));
        rulesString =
                Arrays.stream(rulesString)
                        .filter(rule -> StringUtils.isNotBlank(rule))
                        .map(rule -> rule.trim())
                        .toArray(String[]::new);
        return rulesString;
    }

    /**
     * Checks if there is a layer name available on the RENDERLABEL vendor parameter.
     *
     * @param layerName the layer name
     * @return the parameter if found. Returns null if no matching parameter exists
     */
    public static AttributeLabelParameter getParameterForLayerName(String layerName) {
        Request request = Dispatcher.REQUEST.get();
        if (request == null || !request.getKvp().containsKey(AttributesGlobeKvpParser.RENDERLABEL))
            return null;
        // get the attribute labels params from kvp
        @SuppressWarnings("unchecked")
        List<AttributeLabelParameter> labels =
                (List<AttributeLabelParameter>)
                        request.getKvp().get(AttributesGlobeKvpParser.RENDERLABEL);
        // check if the layer is included on the label parameters list
        AttributeLabelParameter labelParameter =
                labels.stream()
                        .filter(lp -> lp.getLayerName().equals(layerName))
                        .findFirst()
                        .orElse(null);
        return labelParameter;
    }
}
