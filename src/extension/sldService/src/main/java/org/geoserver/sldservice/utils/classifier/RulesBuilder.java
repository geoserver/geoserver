/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2007-2008-2009 GeoSolutions S.A.S., http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.ExplicitClassifier;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.styling.Mark;
import org.geotools.styling.Rule;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.util.factory.GeoTools;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

/**
 * Creates a List of Rule using different classification methods Sets up only filter not Symbolyzer
 * Available Classification: Quantile,Unique Interval & Equal Interval
 *
 * @author kappu
 */
public class RulesBuilder {

    private static final Logger LOGGER = Logger.getLogger(RulesBuilder.class.toString());

    private static FilterFactory2 FF =
            CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    private static StyleFactory SF =
            CommonFactoryFinder.getStyleFactory(GeoTools.getDefaultHints());

    private StyleBuilder sb;

    private double strokeWeight = 1;
    private Color strokeColor = Color.BLACK;
    private int pointSize = 15;
    private boolean includeStrokeForPoints = false;

    private boolean outputPercentages = false;

    private Integer percentagesScale;

    public RulesBuilder() {
        sb = new StyleBuilder(SF, FF);
    }

    public RulesBuilder(boolean outputPercentages, Integer percentagesScale) {
        sb = new StyleBuilder(SF, FF);
        this.outputPercentages = outputPercentages;
        this.percentagesScale = percentagesScale;
    }

    public void setStrokeWeight(double strokeWeight) {
        this.strokeWeight = strokeWeight;
    }

    public void setStrokeColor(Color strokeColor) {
        if (strokeColor != null) {
            this.strokeColor = strokeColor;
        }
    }

    public void setPointSize(int pointSize) {
        this.pointSize = pointSize;
    }

    public void setIncludeStrokeForPoints(boolean includeStrokeForPoints) {
        this.includeStrokeForPoints = includeStrokeForPoints;
    }

    private List<Rule> getRules(
            FeatureCollection features,
            String property,
            Class<?> propertyType,
            int classNumber,
            boolean open,
            boolean normalize,
            String functionName) {
        try {
            final Function classify;
            if (functionName.equals("EqualArea")) {
                classify =
                        FF.function(
                                functionName,
                                FF.property(property),
                                FF.literal(classNumber),
                                FF.literal(""),
                                FF.literal(outputPercentages));
            } else {
                classify =
                        FF.function(
                                functionName,
                                FF.property(property),
                                FF.literal(classNumber),
                                FF.literal(outputPercentages));
            }
            Classifier groups = (Classifier) classify.evaluate(features);
            if (groups instanceof RangedClassifier)
                if (open)
                    return openRangedRules(
                            (RangedClassifier) groups, property, propertyType, normalize);
                else
                    return closedRangedRules(
                            (RangedClassifier) groups, property, propertyType, normalize);
            else if (groups instanceof ExplicitClassifier)
                return this.explicitRules((ExplicitClassifier) groups, property, propertyType);

        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.log(
                        Level.INFO,
                        "Failed to build "
                                + functionName
                                + " Classification"
                                + e.getLocalizedMessage(),
                        e);
        }
        return null;
    }

    /** Generate a List of rules using quantile classification Sets up only filter not symbolizer */
    public List<Rule> quantileClassification(
            FeatureCollection features,
            String property,
            Class<?> propertyType,
            int classNumber,
            boolean open,
            boolean normalize) {
        return getRules(features, property, propertyType, classNumber, open, normalize, "Quantile");
    }

    /**
     * Generate a List of rules using equal interval classification Sets up only filter not
     * symbolizer
     */
    public List<Rule> equalIntervalClassification(
            FeatureCollection features,
            String property,
            Class<?> propertyType,
            int intervals,
            boolean open,
            boolean normalize) {
        return getRules(
                features, property, propertyType, intervals, open, normalize, "EqualInterval");
    }

    /**
     * Generate a List of rules using unique interval classification Sets up only filter not
     * symbolizer
     */
    public List<Rule> uniqueIntervalClassification(
            FeatureCollection features,
            String property,
            Class<?> propertyType,
            int intervals,
            boolean normalize)
            throws IllegalArgumentException {
        List<Rule> rules =
                getRules(
                        features,
                        property,
                        propertyType,
                        features.size(),
                        false,
                        normalize,
                        "UniqueInterval");
        if (intervals > 0 && rules.size() > intervals) {
            throw new IllegalArgumentException("Intervals: " + rules.size());
        }
        return rules;
    }

    /**
     * Generate a List of rules using Jenks Natural Breaks classification Sets up only filter not
     * symbolizer
     */
    public List<Rule> jenksClassification(
            FeatureCollection features,
            String property,
            Class<?> propertyType,
            int classNumber,
            boolean open,
            boolean normalize) {
        return getRules(features, property, propertyType, classNumber, open, normalize, "Jenks");
    }

    /**
     * Generate a List of rules using Equal Area classification. Sets up only filter not symbolizer.
     */
    public List<Rule> equalAreaClassification(
            FeatureCollection features,
            String property,
            Class<?> propertyType,
            int classNumber,
            boolean open,
            boolean normalize) {
        return getRules(
                features, property, propertyType, classNumber, open, normalize, "EqualArea");
    }

    /** Generate Polygon Symbolyzer for each rule in list Fill color is choose from rampcolor */
    public void polygonStyle(List<Rule> rules, ColorRamp fillRamp, boolean reverseColors) {

        Iterator<Rule> it;
        Rule rule;
        Iterator<Color> colors;
        Color color;

        try {
            // adjust the colorRamp with the correct number of classes
            fillRamp.setNumClasses(rules.size());
            if (reverseColors) {
                fillRamp.revert();
            }
            colors = fillRamp.getRamp().iterator();

            it = rules.iterator();
            while (it.hasNext() && colors.hasNext()) {
                color = colors.next();
                rule = it.next();
                rule.symbolizers().clear();
                rule.symbolizers()
                        .add(
                                sb.createPolygonSymbolizer(
                                        strokeWeight < 0
                                                ? null
                                                : sb.createStroke(strokeColor, strokeWeight),
                                        sb.createFill(color)));
            }
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.log(
                        Level.INFO,
                        "Failed to build polygon Symbolizer" + e.getLocalizedMessage(),
                        e);
        }
    }

    /** Generate Polygon Symbolyzer for each rule in list Fill color is choose from rampcolor */
    public void pointStyle(List<Rule> rules, ColorRamp fillRamp, boolean reverseColors) {

        Iterator<Rule> it;
        Rule rule;
        Iterator<Color> colors;
        Color color;

        try {
            // adjust the colorRamp with the correct number of classes
            fillRamp.setNumClasses(rules.size());
            if (reverseColors) {
                fillRamp.revert();
            }
            colors = fillRamp.getRamp().iterator();

            it = rules.iterator();
            while (it.hasNext() && colors.hasNext()) {
                color = colors.next();
                rule = it.next();
                // sb.createStroke(Color.BLACK,1),

                Mark mark =
                        sb.createMark(
                                StyleBuilder.MARK_CIRCLE,
                                sb.createFill(color),
                                includeStrokeForPoints && strokeWeight >= 0
                                        ? sb.createStroke(strokeColor, strokeWeight)
                                        : null);
                rule.symbolizers().clear();
                rule.symbolizers()
                        .add(
                                sb.createPointSymbolizer(
                                        sb.createGraphic(null, mark, null, 1.0, pointSize, 0.0)));
            }
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.log(
                        Level.INFO,
                        "Failed to build polygon Symbolizer" + e.getLocalizedMessage(),
                        e);
        }
    }

    /** Generate Line Symbolyzer for each rule in list Stroke color is choose from rampcolor */
    public void lineStyle(List<Rule> rules, ColorRamp fillRamp, boolean reverseColors) {

        Iterator<Rule> it;
        Rule rule;
        Iterator<Color> colors;
        Color color;

        try {
            // adjust the colorRamp with the correct number of classes
            fillRamp.setNumClasses(rules.size());
            if (reverseColors) {
                fillRamp.revert();
            }
            colors = fillRamp.getRamp().iterator();

            it = rules.iterator();
            while (it.hasNext() && colors.hasNext()) {
                color = colors.next();
                rule = it.next();
                rule.symbolizers().clear();
                rule.symbolizers().add(sb.createLineSymbolizer(color));
            }
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.log(
                        Level.INFO, "Failed to build Line Symbolizer" + e.getLocalizedMessage(), e);
        }
    }

    public StyleFactory getStyleFactory() {
        return this.SF;
    }

    /** Generate Rules from Rangedclassifier groups build a List of rules */
    public List<Rule> openRangedRules(
            RangedClassifier groups, String property, Class<?> propertyType, boolean normalize) {

        Rule r;
        Filter f;
        List<Rule> list = new ArrayList();
        Expression att = normalizeProperty(FF.property(property), propertyType, normalize);
        PercentagesManager percMan = new PercentagesManager(groups.getPercentages());
        try {
            /* First class */
            r = SF.createRule();
            if (groups.getMin(0).equals(groups.getMax(0))) {
                f = FF.equals(att, FF.literal(groups.getMin(0)));
                r.getDescription().setTitle((FF.literal(groups.getMin(0)).toString()));
            } else {
                f = FF.less(att, FF.literal(groups.getMax(0)));
                r.getDescription().setTitle(" < " + FF.literal(groups.getMax(0)));
            }
            r.setFilter(f);
            list.add(r);
            percMan.collectRulePercentage(0);
            for (int i = 1; i < groups.getSize() - 1; i++) {
                r = SF.createRule();
                if (groups.getMin(i).equals(groups.getMax(i))) {
                    f = FF.equals(att, FF.literal(groups.getMax(i)));
                    if (!isDuplicatedClass(list, f, percMan, i)) {
                        r.getDescription().setTitle((FF.literal(groups.getMin(i)).toString()));
                        r.setFilter(f);
                        list.add(r);
                        percMan.collectRulePercentage(i);
                    }
                } else {
                    f =
                            FF.and(
                                    getNotOverlappingFilter(i, groups, att),
                                    FF.less(att, FF.literal(groups.getMax(i))));
                    if (!isDuplicatedClass(list, f, percMan, i)) {
                        r.getDescription()
                                .setTitle(
                                        (" >= "
                                                + FF.literal(groups.getMin(i))
                                                + " AND < "
                                                + FF.literal(groups.getMax(i))));
                        r.setFilter(f);
                        list.add(r);
                        percMan.collectRulePercentage(i);
                    }
                }
            }
            /* Last class */
            r = SF.createRule();
            f = getNotOverlappingFilter(groups.getSize() - 1, groups, att);
            r.setFilter(f);
            r.getDescription().setTitle((" >= " + FF.literal(groups.getMin(groups.getSize() - 1))));
            list.add(r);
            percMan.collectRulePercentage(groups.getSize() - 1);
            percMan.appendPercentagesToLabels(list);
            return list;
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.log(
                        Level.INFO,
                        "Failed to build Open Ranged rules" + e.getLocalizedMessage(),
                        e);
        }
        return null;
    }

    private Expression normalizeProperty(
            PropertyName property, Class<?> propertyType, boolean normalize) {
        if (normalize
                && (Integer.class.isAssignableFrom(propertyType)
                        || Long.class.isAssignableFrom(propertyType))) {
            return FF.function("parseDouble", property);
        }
        return property;
    }

    /** Generate Rules from Rangedclassifier groups build a List of rules */
    public List<Rule> closedRangedRules(
            RangedClassifier groups, String property, Class<?> propertyType, boolean normalize) {

        Rule r;
        Filter f;
        List<Rule> list = new ArrayList();
        Expression att = normalizeProperty(FF.property(property), propertyType, normalize);
        PercentagesManager percMan = null;
        percMan = new PercentagesManager(groups.getPercentages());
        try {
            /* First class */
            r = SF.createRule();
            for (int i = 0; i < groups.getSize(); i++) {
                r = SF.createRule();
                if (groups.getMin(i).equals(groups.getMax(i))) {
                    f = FF.equals(att, FF.literal(groups.getMin(i)));
                    if (!isDuplicatedClass(list, f, percMan, i)) {
                        r.getDescription().setTitle((FF.literal(groups.getMin(i)).toString()));
                        r.setFilter(f);
                        list.add(r);
                        percMan.collectRulePercentage(i);
                    }
                } else {
                    f =
                            FF.and(
                                    getNotOverlappingFilter(i, groups, att),
                                    i == (groups.getSize() - 1)
                                            ? FF.lessOrEqual(att, FF.literal(groups.getMax(i)))
                                            : FF.less(att, FF.literal(groups.getMax(i))));
                    if (!isDuplicatedClass(list, f, percMan, i)) {
                        r.getDescription()
                                .setTitle(
                                        (" >= "
                                                + FF.literal(groups.getMin(i))
                                                + " AND "
                                                + (i == (groups.getSize() - 1) ? "<=" : "<")
                                                + FF.literal(groups.getMax(i))));
                        r.setFilter(f);
                        list.add(r);
                        percMan.collectRulePercentage(i);
                    }
                }
            }
            percMan.appendPercentagesToLabels(list);
            return list;
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.log(
                        Level.INFO,
                        "Failed to build closed Ranged Rules" + e.getLocalizedMessage(),
                        e);
        }
        return null;
    }

    /** Generate Rules from Explicit classifier groups build a List of rules */
    public List<Rule> explicitRules(
            ExplicitClassifier groups, String property, Class<?> propertyType) {

        Rule r;
        Filter f;
        List<Rule> list = new ArrayList();
        PropertyName att = FF.property(property);
        String szFilter = "";
        String szTitle = "";
        Literal val;
        PercentagesManager percMan = new PercentagesManager(groups.getPercentages());

        try {
            for (int i = 0; i < groups.getSize(); i++) {
                r = SF.createRule();
                Set ls = groups.getValues(i);
                Iterator it = ls.iterator();
                val = FF.literal(it.next());
                szFilter = att + "=\'" + val + "\'";
                szTitle = "" + val;

                while (it.hasNext()) {
                    val = FF.literal(it.next());
                    szFilter += " OR " + att + "=\'" + val + "\'";
                    szTitle += " OR " + val;
                }
                f = CQL.toFilter(szFilter);
                r.getDescription().setTitle(szTitle);
                r.setFilter(f);
                list.add(r);
                percMan.collectRulePercentage(i);
            }
            percMan.appendPercentagesToLabels(list);
            return list;
        } catch (CQLException e) {
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.log(
                        Level.INFO, "Failed to build explicit Rules" + e.getLocalizedMessage(), e);
        }
        return null;
    }

    public double[] getCustomPercentages(
            FeatureCollection features,
            RangedClassifier classifier,
            String attribute,
            Class<?> propertyType,
            boolean normalize) {
        int size = classifier.getSize();
        PropertyName prop = new AttributeExpressionImpl(attribute);
        List<Filter> filters = new ArrayList<>(size);
        Expression attr = normalizeProperty(prop, propertyType, normalize);
        for (int i = 0; i < size; i++) {
            Object min = classifier.getMin(i);
            Object max = classifier.getMax(i);
            if (min.equals(max)) {
                filters.add(FF.equals(attr, FF.literal(min)));
            } else if (i == size - 1) {
                Filter f1 = FF.greaterOrEqual(attr, FF.literal(min));
                Filter f2 = FF.lessOrEqual(attr, FF.literal(max));
                Filter and = FF.and(f1, f2);
                filters.add(and);
            } else {
                Filter f1 = FF.greaterOrEqual(attr, FF.literal(min));
                Filter f2 = FF.less(attr, FF.literal(max));
                Filter and = FF.and(f1, f2);
                filters.add(and);
            }
        }
        int[][] bins = new int[size][1];
        try (FeatureIterator it = features.features()) {
            while (it.hasNext()) {
                Feature f = it.next();
                int i = 0;
                for (Filter filter : filters) {
                    if (filter.evaluate(f)) {
                        bins[i][0]++;
                        break;
                    }
                    i++;
                }
            }
        }
        return computeCustomPercentages(bins, features.size());
    }

    private double[] computeCustomPercentages(int[][] bins, double totalSize) {
        double[] percentages = new double[bins.length];
        for (int i = 0; i < bins.length; i++) {
            double classMembers = bins[i][0];
            if (classMembers != 0d && totalSize != 0d)
                percentages[i] = (classMembers / totalSize) * 100;
            else percentages[i] = 0d;
        }
        return percentages;
    }

    private boolean isDuplicatedClass(
            List<Rule> rules, Filter f, PercentagesManager percMan, int currentIdx) {
        Optional<Rule> opRule = rules.stream().filter(r -> r.getFilter().equals(f)).findFirst();
        boolean result = opRule.isPresent();
        if (percMan != null) {
            if (result) {
                percMan.collapsePercentages(rules, opRule.get(), currentIdx);
            }
        }
        return result;
    }

    /**
     * Compares current min and previous min avoiding the production of overlapping Rules
     *
     * @param currentIdx
     * @param groups
     * @param att
     * @return
     */
    private Filter getNotOverlappingFilter(
            int currentIdx, RangedClassifier groups, Expression att) {
        Filter f;
        if (currentIdx > 0) {
            Object currMin = groups.getMin(currentIdx);
            Object prevMin = groups.getMin(currentIdx - 1);
            if (!prevMin.equals(currMin))
                f = FF.greaterOrEqual(att, FF.literal(groups.getMin(currentIdx)));
            else f = FF.greater(att, FF.literal(groups.getMin(currentIdx)));
        } else {
            f = FF.greaterOrEqual(att, FF.literal(groups.getMin(currentIdx)));
        }

        return f;
    }

    private class PercentagesManager {

        private double[] percentages;
        List<Double> collapsedPercentages;
        boolean collapsePercentages;

        public PercentagesManager(double[] percentages) {
            this.percentages = percentages;
            this.collapsePercentages =
                    percentages != null && !(DoubleStream.of(percentages).sum() > 100.0);
            this.collapsedPercentages = new ArrayList<>();
        }

        public void collapsePercentages(List<Rule> rules, Rule current, int index) {
            if (outputPercentages && collapsePercentages) {
                int ruleIdx = rules.indexOf(current);
                double toSumTo = percentages[index];
                double toBeSummed = collapsedPercentages.get(ruleIdx);
                collapsedPercentages.set(ruleIdx, toBeSummed + toSumTo);
            }
        }

        public void collectRulePercentage(int currentIndex) {
            if (outputPercentages) collapsedPercentages.add(percentages[currentIndex]);
        }

        public void appendPercentagesToLabels(List<Rule> rules) {
            if (outputPercentages) {
                collapsedPercentages =
                        new PercentagesRoundHandler(percentagesScale)
                                .roundPercentages(collapsedPercentages);
                for (int i = 0; i < rules.size(); i++) {
                    Rule rule = rules.get(i);
                    String percLabel =
                            rule.getDescription().getTitle()
                                    + " ("
                                    + collapsedPercentages.get(i)
                                    + "%)";
                    rule.getDescription().setTitle(percLabel);
                }
            }
        }
    }
}
