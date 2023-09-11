/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.translate.renderer;

import static org.geoserver.gsr.model.symbol.HorizontalAlignmentEnum.CENTER;
import static org.geoserver.gsr.model.symbol.HorizontalAlignmentEnum.LEFT;
import static org.geoserver.gsr.model.symbol.HorizontalAlignmentEnum.RIGHT;
import static org.geoserver.gsr.model.symbol.VerticalAlignmentEnum.BOTTOM;
import static org.geoserver.gsr.model.symbol.VerticalAlignmentEnum.MIDDLE;
import static org.geoserver.gsr.model.symbol.VerticalAlignmentEnum.TOP;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.geoserver.gsr.model.font.FontDecorationEnum;
import org.geoserver.gsr.model.font.FontStyleEnum;
import org.geoserver.gsr.model.font.FontWeightEnum;
import org.geoserver.gsr.model.label.Label;
import org.geoserver.gsr.model.label.LineLabel;
import org.geoserver.gsr.model.label.LineLabelPlacementEnum;
import org.geoserver.gsr.model.label.PointLabel;
import org.geoserver.gsr.model.label.PointLabelPlacementEnum;
import org.geoserver.gsr.model.symbol.HorizontalAlignmentEnum;
import org.geoserver.gsr.model.symbol.TextSymbol;
import org.geoserver.gsr.model.symbol.VerticalAlignmentEnum;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.style.Displacement;
import org.geotools.api.style.Font;
import org.geotools.api.style.Halo;
import org.geotools.api.style.PointPlacement;
import org.geotools.api.style.Rule;
import org.geotools.api.style.TextSymbolizer;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.styling.AbstractStyleVisitor;

public class LabelInfoVisitor extends AbstractStyleVisitor {

    List<Label> labels = new ArrayList<>();
    Label currentLabel;

    public List<Label> getLabelInfo() {
        if (labels.isEmpty()) {
            return null;
        }
        return labels;
    }

    @Override
    public void visit(Rule rule) {
        super.visit(rule);

        if (currentLabel != null) {
            // tricky: the min scale in ESRI is the max scale denominator (technically true,
            // but it's confusing they call it scale and assign it a scale denominator instead)
            if (!Double.isInfinite(rule.getMaxScaleDenominator())
                    && rule.getMaxScaleDenominator() < Integer.MAX_VALUE) {
                // really wondering if scales should be a generic double....
                currentLabel.setMinScale((int) rule.getMaxScaleDenominator());
            } else {
                // zero means unassigned
                currentLabel.setMinScale(0);
            }
            if (rule.getMinScaleDenominator() > 0) {
                currentLabel.setMaxScale((int) rule.getMinScaleDenominator());
            }

            if (rule.getFilter() != null && !Filter.INCLUDE.equals(rule.getFilter())) {
                // expression language is not the same, but close enough
                currentLabel.setWhere(CQL.toCQL(rule.getFilter()));
            }

            labels.add(currentLabel);

            currentLabel = null;
        }
    }

    @Override
    public void visit(TextSymbolizer text) {
        // TODO: support more complex expression
        if (!(text.getLabel() instanceof Literal) && !(text.getLabel() instanceof PropertyName))
            return;

        String labelExpression;
        if (text.getLabel() instanceof Literal) {
            labelExpression = "\"" + text.getLabel().evaluate(null) + "\"";
        } else {
            labelExpression = "[" + ((PropertyName) text.getLabel()).getPropertyName() + "]";
        }

        TextSymbol ts = getTextSymbol(text);

        if (text.getLabelPlacement() == null
                || text.getLabelPlacement() instanceof PointPlacement) {
            this.currentLabel =
                    new PointLabel(
                            PointLabelPlacementEnum.CENTER_CENTER,
                            labelExpression,
                            false,
                            ts,
                            0,
                            0);
        } else {
            this.currentLabel =
                    new LineLabel(
                            LineLabelPlacementEnum.CENTER_ALONG, labelExpression, false, ts, 0, 0);
        }
    }

    private TextSymbol getTextSymbol(TextSymbolizer text) {
        // point placement
        Optional<PointPlacement> pointPlacement =
                Optional.ofNullable(text.getLabelPlacement())
                        .filter(p -> p instanceof PointPlacement)
                        .map(p -> (PointPlacement) p);
        double angle =
                pointPlacement
                        .map(p -> p.getRotation())
                        .map(r -> r.evaluate(null, Double.class))
                        .orElse(0d);
        int xoffset =
                pointPlacement
                        .map(p -> p.getDisplacement())
                        .map(Displacement::getDisplacementX)
                        .map(e -> e.evaluate(null, Integer.class))
                        .orElse(0);
        int yoffset =
                pointPlacement
                        .map(p -> p.getDisplacement())
                        .map(Displacement::getDisplacementY)
                        .map(e -> e.evaluate(null, Integer.class))
                        .orElse(0);

        VerticalAlignmentEnum verticalAlignment =
                pointPlacement
                        .map(p -> p.getAnchorPoint())
                        .map(ap -> ap.getAnchorPointY())
                        .map(e -> mapPoint(e, BOTTOM, MIDDLE, TOP))
                        .orElse(null);
        HorizontalAlignmentEnum horizontalAlighment =
                pointPlacement
                        .map(p -> p.getAnchorPoint())
                        .map(ap -> ap.getAnchorPointX())
                        .map(e -> mapPoint(e, LEFT, CENTER, RIGHT))
                        .orElse(null);

        // fill color
        int[] color = new int[] {0, 0, 0, 255};
        if (text.getFill() != null) {
            color =
                    evaluateColor(
                            text.getFill().getColor(), text.getFill().getOpacity(), Color.BLACK);
        }

        // "halo" color... not the same thing, but hopefully close enough in the intentions
        Optional<Halo> halo = Optional.ofNullable(text.getHalo());
        int[] haloColor =
                halo.map(h -> h.getFill())
                        .map(f -> evaluateColor(f.getColor(), f.getOpacity(), Color.BLACK))
                        .orElse(null);
        Integer haloSize =
                halo.map(h -> h.getRadius())
                        .map(r -> r.evaluate(null, Integer.class))
                        .orElseGet(() -> haloColor == null ? null : 1);

        Optional<Font> tsFont = Optional.of(text.getFont());
        String family =
                tsFont.map(f -> f.getFamily())
                        .map(e -> e.get(0).evaluate(null, String.class))
                        .orElse("Sans");
        int size =
                tsFont.map(f -> f.getSize()).map(e -> e.evaluate(null, Integer.class)).orElse(12);
        FontStyleEnum fontStyle =
                tsFont.map(f -> f.getStyle())
                        .map(e -> e.evaluate(null, String.class))
                        .map(s -> FontStyleEnum.valueOf(s.toUpperCase()))
                        .orElse(null);
        FontWeightEnum fontWeight =
                tsFont.map(f -> f.getWeight())
                        .map(e -> e.evaluate(null, String.class))
                        .map(s -> FontWeightEnum.valueOf(s.toUpperCase()))
                        .orElse(null);
        Optional<Map<String, String>> options = Optional.of(text.getOptions());
        FontDecorationEnum fontDecoration =
                options.map(o -> o.get(TextSymbolizer.STRIKETHROUGH_TEXT_KEY))
                        .map(s -> Boolean.valueOf(s) ? FontDecorationEnum.LINE_THROUGH : null)
                        .orElse(null);
        if (fontDecoration == null) {
            fontDecoration =
                    options.map(o -> o.get(TextSymbolizer.UNDERLINE_TEXT_KEY))
                            .map(s -> Boolean.valueOf(s) ? FontDecorationEnum.UNDERLINE : null)
                            .orElse(null);
        }
        org.geoserver.gsr.model.font.Font font =
                new org.geoserver.gsr.model.font.Font(
                        family, size, fontStyle, fontWeight, fontDecoration);

        TextSymbol textSymbol =
                new TextSymbol(
                        angle,
                        xoffset,
                        yoffset,
                        color,
                        null,
                        null,
                        verticalAlignment,
                        horizontalAlighment,
                        false,
                        font);
        if (haloSize != null) {
            textSymbol.setHaloColor(haloColor);
            textSymbol.setHaloSize(haloSize);
        }
        return textSymbol;
    }

    private <T> T mapPoint(Expression e, T low, T mid, T high) {
        Double position = e.evaluate(null, Double.class);
        if (position == null) {
            return null;
        } else if (position < 0.25) {
            return low;
        } else if (position < 0.75) {
            return mid;
        }
        return high;
    }

    private int[] evaluateColor(Expression color, Expression opacity, Color defaultColor) {
        int[] result =
                new int[] {
                    defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), 255
                };
        if (color != null && color.evaluate(null, Color.class) != null) {
            Color evaluated = color.evaluate(null, Color.class);
            result[0] = evaluated.getRed();
            result[1] = evaluated.getGreen();
            result[2] = evaluated.getBlue();
        }
        if (opacity != null && opacity.evaluate(null, Double.class) != null) {
            result[3] = (int) Math.round(opacity.evaluate(null, Double.class) * 255);
        }

        return result;
    }
}
