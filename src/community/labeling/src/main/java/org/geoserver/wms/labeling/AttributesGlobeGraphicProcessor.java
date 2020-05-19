/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.labeling;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.expression.Expression;

/** Builds the attributes globe image based on provided URL and feature. */
class AttributesGlobeGraphicProcessor {

    private static Logger LOG = Logging.getLogger(AttributesGlobeGraphicProcessor.class);

    static final char ESTIMATION_CHAR = '_';
    static final String FONT_SIZE = "fontSize";
    static final String TITLE_FONT_NAME = "titleFontName";
    static final String VALUE_FONT_NAME = "valueFontName";
    static final String TITLE_COLOR_CODE = "titleColorCode";
    static final String VALUE_COLOR_CODE = "valueColorCode";
    static final String INTER_LINE_SPACE = "interLineSpace";
    static final String ROUND_CORNER_RADIUS = "roundCornerRadius";
    static final String MAX_VALUE_CHARS = "maxValueChars";
    static final String TAIL_HEIGHT = "tailHeight";
    static final String TAIL_WIDTH = "tailWidth";
    static final String MARGIN = "margin";
    public static final String LAYER = "layer";

    private final Expression url;
    private final SimpleFeature feature;

    private AttributesGlobeConfiguration config = new AttributesGlobeConfiguration();
    private List<NameValuePair> pairs;
    private String layerName;
    private AttributeLabelParameter labelParameter;
    private Map<String, String> attributes;

    public AttributesGlobeGraphicProcessor(Expression url, SimpleFeature feature) {
        this.url = url;
        this.feature = feature;
        initdUrlParameterpairs();
        initLayerName();
        buildLabelParameter();
        mergeConfigurations();
        buildAttributesMap();
    }

    /**
     * Builds the attributes globe image and returns it.
     *
     * @return the generated attributes globe image
     */
    public BufferedImage buildImage() {
        AttributesGlobeGenerator generator =
                new AttributesGlobeGenerator(labelParameter.getFonts(), config, attributes);
        return generator.generateImage();
    }

    private void mergeConfigurations() {
        // merge draw configurations
        config.setInterLineSpace(getIntParamValue(INTER_LINE_SPACE, config.getInterLineSpace()));
        config.setMargin(getIntParamValue(MARGIN, config.getMargin()));
        config.setRoundCornerRadius(
                getIntParamValue(ROUND_CORNER_RADIUS, config.getRoundCornerRadius()));
        config.setMaxValueChars(getIntParamValue(MAX_VALUE_CHARS, config.getMaxValueChars()));
        config.setTailHeight(getIntParamValue(TAIL_HEIGHT, config.getTailHeight()));
        config.setTailWidth(getIntParamValue(TAIL_WIDTH, config.getTailWidth()));

        // merge fonts configurations
        AttributeGlobeFonts oldFonts = labelParameter.getFonts();
        int fontSize = getIntParamValue(FONT_SIZE, oldFonts.getFontSize());
        String titleFontName = getParamValueOrDefault(TITLE_FONT_NAME, oldFonts.getTitleFontName());
        String valueFontName = getParamValueOrDefault(VALUE_FONT_NAME, oldFonts.getValueFontName());
        String titleColorCode =
                getParamValueOrDefaultColor(TITLE_COLOR_CODE, oldFonts.getTitleColorCode());
        String valueColorCode =
                getParamValueOrDefaultColor(VALUE_COLOR_CODE, oldFonts.getValueColorCode());
        AttributeGlobeFonts fonts =
                new AttributeGlobeFonts(
                        fontSize, titleFontName, valueFontName, titleColorCode, valueColorCode);
        AttributeLabelParameter labelParamNew =
                new AttributeLabelParameter(
                        labelParameter.getLayerName(),
                        labelParameter.getFilter(),
                        fonts,
                        labelParameter.getAttributes());
        this.labelParameter = labelParamNew;
    }

    private int getIntParamValue(String name, int defaultVal) {
        String strValue = getParamValue(name).orElse(null);
        if (!StringUtils.isBlank(strValue)) {
            try {
                return Integer.parseInt(strValue);
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "Error on parameter integer format", e);
            }
        }
        return defaultVal;
    }

    private Optional<String> getParamValue(String name) {
        return pairs.stream()
                .filter(p -> name.equals(p.getName()))
                .map(NameValuePair::getValue)
                .findFirst();
    }

    private String getParamValueOrDefault(String name, String defaultValue) {
        String value = getParamValue(name).orElse(null);
        if (value == null) return defaultValue;
        return value;
    }

    private String getParamValueOrDefaultColor(String name, String defaultValue) {
        String value = getParamValue(name).orElse(null);
        if (value == null) return defaultValue;
        if (value.startsWith("#")) return value;
        return "#" + value;
    }

    private void buildAttributesMap() {
        if (feature != null) {
            attributes = new LinkedHashMap<String, String>(labelParameter.getAttributes().size());
            for (String attrName : labelParameter.getAttributes()) {
                Object value = feature.getAttribute(attrName);
                attributes.put(attrName, processValue(value));
            }
        } else {
            buildMockAttributesMap();
        }
    }

    /** Builds a mock globe image for the style estimator. */
    private void buildMockAttributesMap() {
        attributes = new LinkedHashMap<String, String>(labelParameter.getAttributes().size());
        for (String attrName : labelParameter.getAttributes()) {
            attributes.put(
                    attrName, StringUtils.repeat(ESTIMATION_CHAR, config.getMaxValueChars()));
        }
    }

    private String processValue(Object value) {
        int maxValueChars = config.getMaxValueChars();
        if (value == null) return "";
        String valueStr = Converters.convert(value, String.class);
        if (valueStr == null) return "";
        valueStr = valueStr.trim();
        if (valueStr.length() > maxValueChars) {
            valueStr = valueStr.substring(0, maxValueChars);
        }
        return valueStr;
    }

    /** Retrieves the layer name identifier from the provided URL expression. */
    private void initdUrlParameterpairs() {
        String urlStr = url.evaluate(null, String.class);
        try {
            pairs = URLEncodedUtils.parse(new URI(urlStr), StandardCharsets.UTF_8);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /** Retrieves the layer name identifier from the provided URL expression. */
    private String initLayerName() {
        layerName =
                pairs.stream()
                        .filter(p -> LAYER.equals(p.getName()))
                        .map(NameValuePair::getValue)
                        .findFirst()
                        .orElse(null);
        if (StringUtils.isBlank(layerName)) return null;

        return layerName;
    }

    private void buildLabelParameter() {
        labelParameter = AttributesGlobeKvpParser.getParameterForLayerName(layerName);
    }
}
