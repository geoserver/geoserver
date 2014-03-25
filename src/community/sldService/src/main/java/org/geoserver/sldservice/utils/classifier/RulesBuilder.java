/*
 *  Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.geoserver.sldservice.utils.classifier;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.ExplicitClassifier;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.styling.Rule;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

/**
 * Creates a List of Rule using different classification methods Sets up only
 * filter not Symbolyzer Available Classification: Quantile,Unique Interval &
 * Equal Interval
 *
 * @author kappu
 *
 */
public class RulesBuilder {

	private final static Logger LOGGER = Logger.getLogger(RulesBuilder.class.toString());

	private FilterFactory2 ff;
	private StyleFactory styleFactory;
	private StyleBuilder sb;

	public RulesBuilder() {
		ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		styleFactory = CommonFactoryFinder.getStyleFactory(GeoTools.getDefaultHints());
		sb = new StyleBuilder();
	}

	/**
	 * Generate a List of rules using quantile classification Sets up only
	 * filter not symbolizer
	 *
	 * @param features
	 * @param property
	 * @param classNumber
	 * @return
	 */
	public List<Rule> quantileClassification(FeatureCollection features,
			String property, int classNumber, boolean open) {

		FeatureType fType;
		Classifier groups = null;
		try {
			final Function classify = ff.function("Quantile", ff.property(property),
					ff.literal(classNumber));
			groups = (Classifier) classify.evaluate(features);
			if (groups instanceof RangedClassifier)
			    if(open)
			        return openRangedRules((RangedClassifier) groups, property);
			    else
			        return closedRangedRules((RangedClassifier) groups, property);
			else if (groups instanceof ExplicitClassifier)
				return this.explicitRules((ExplicitClassifier) groups, property);

		} catch (Exception e) {
			if (LOGGER.isLoggable(Level.INFO))
				LOGGER.log(Level.INFO, "Failed to build Quantile Classification"
						+ e.getLocalizedMessage(), e);
		}
		return null;
	}

	/**
	 * Generate a List of rules using equal interval classification Sets up only
	 * filter not symbolizer
	 *
	 * @param features
	 * @param property
	 * @param classNumber
	 * @return
	 */
	public List<Rule> equalIntervalClassification(FeatureCollection features, String property, int classNumber, boolean open) {
		Classifier groups = null;
		try {
			final Function classify = ff.function("EqualInterval", ff.property(property), ff.literal(classNumber));
			groups = (Classifier) classify.evaluate(features);
			//System.out.println(groups.getSize());
			if (groups instanceof RangedClassifier)
			    if(open)
			        return openRangedRules((RangedClassifier) groups, property);
			    else
			        return closedRangedRules((RangedClassifier) groups, property);
			else if (groups instanceof ExplicitClassifier)
				return this.explicitRules((ExplicitClassifier) groups, property);

		} catch (Exception e) {
			if (LOGGER.isLoggable(Level.INFO))
				LOGGER.log(Level.INFO, "Failed to build EqualInterval Classification"
						+ e.getLocalizedMessage(), e);
		}
		return null;
	}

	/**
	 * Generate a List of rules using unique interval classification Sets up
	 * only filter not symbolizer
	 *
	 * @param features
	 * @param property
	 * @return
	 */
	public List<Rule> uniqueIntervalClassification(FeatureCollection features,
			String property) {
		Classifier groups = null;
		int classNumber = features.size();
		try {
			final Function classify = ff.function("UniqueInterval", ff.property(property), ff.literal(classNumber));
			groups = (Classifier) classify.evaluate(features);
			if (groups instanceof RangedClassifier)
				return this.closedRangedRules((RangedClassifier) groups, property);
			else if (groups instanceof ExplicitClassifier)
				return this.explicitRules((ExplicitClassifier) groups, property);

		} catch (Exception e) {
			if (LOGGER.isLoggable(Level.INFO))
				LOGGER.log(Level.INFO, "Failed to build UniqueInterval Classification"
						+ e.getLocalizedMessage(), e);
		}
		return null;
	}

	/**
     * Generate a List of rules using Jenks Natural Breaks classification Sets up only
     * filter not symbolizer
     *
     * @param features
     * @param property
     * @param classNumber
     * @return
     */
    public List<Rule> jenksClassification(FeatureCollection features, String property, int classNumber, boolean open) {
        Classifier groups = null;
        try {
            final Function classify = ff.function("Jenks", ff.property(property), ff.literal(classNumber));
            groups = (Classifier) classify.evaluate(features);
            //System.out.println(groups.getSize());
            if (groups instanceof RangedClassifier)
                if(open)
                    return openRangedRules((RangedClassifier) groups, property);
                else
                    return closedRangedRules((RangedClassifier) groups, property);
            else if (groups instanceof ExplicitClassifier)
                return this.explicitRules((ExplicitClassifier) groups, property);

        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.log(Level.INFO, "Failed to build Jenks classification"
                        + e.getLocalizedMessage(), e);
        }
        return null;
    }


    /**
	 * Generate Polygon Symbolyzer for each rule in list
	 * Fill color is choose from rampcolor
	 * @param rules
	 * @param fillRamp
     * @param reverseColors
	 */
	public void polygonStyle(List<Rule> rules, ColorRamp fillRamp, boolean reverseColors) {

		Iterator<Rule> it;
		Rule rule;
		Iterator<Color> colors;
		Color color;

		try {
			//adjust the colorRamp with the correct number of classes
			fillRamp.setNumClasses(rules.size());
            if (reverseColors) {
                fillRamp.revert();
            }
			colors = fillRamp.getRamp().iterator();

			it = rules.iterator();
			while (it.hasNext() && colors.hasNext()) {
				color = colors.next();
				rule = it.next();
				rule.setSymbolizers(new Symbolizer[] { sb.createPolygonSymbolizer(sb.createStroke(Color.BLACK,1),sb.createFill(color)) });
			}
		} catch (Exception e) {
			if (LOGGER.isLoggable(Level.INFO))
				LOGGER.log(Level.INFO, "Failed to build polygon Symbolizer"
						+ e.getLocalizedMessage(), e);
		}

	}

	/**
	 * Generate Line Symbolyzer for each rule in list
	 * Stroke color is choose from rampcolor
	 * @param rules
	 * @param fillRamp
     * @param reverseColors
	 */
	public void lineStyle(List<Rule> rules, ColorRamp fillRamp, boolean reverseColors) {

		Iterator<Rule> it;
		Rule rule;
		Iterator<Color> colors;
		Color color;

		try {
			//adjust the colorRamp with the correct number of classes
			fillRamp.setNumClasses(rules.size());
            if (reverseColors) {
                fillRamp.revert();
            }
			colors = fillRamp.getRamp().iterator();

			it = rules.iterator();
			while (it.hasNext() && colors.hasNext()) {
				color = colors.next();
				rule = it.next();
				rule.setSymbolizers(new Symbolizer[] { sb.createLineSymbolizer(color) });
			}
		} catch (Exception e) {
			if (LOGGER.isLoggable(Level.INFO))
				LOGGER.log(Level.INFO, "Failed to build Line Symbolizer"
						+ e.getLocalizedMessage(), e);
		}


	}

	public StyleFactory getStyleFactory() {
		return this.styleFactory;
	}

	/**
     * Generate Rules from Rangedclassifier groups
     * build a List of rules
     * @param groups
     * @param property
     * @return
     */
    private List<Rule> openRangedRules(RangedClassifier groups, String property) {

        Rule r;
        Filter f;
        List<Rule> list = new ArrayList();
        PropertyName att = ff.property(property);
        try {
            /* First class */
            r = styleFactory.createRule();
            if(groups.getMin(0).equals(groups.getMax(0))){
                f = CQL.toFilter(att + " =" + ff.literal(groups.getMax(0)));
                r.setFilter(f);
                r.setTitle( ff.literal(groups.getMax(0)).toString());
                list.add(r);
            }else{
                f = CQL.toFilter(att + " <=" + ff.literal(groups.getMax(0)));
                r.setFilter(f);
                r.setTitle(" <= " + ff.literal(groups.getMax(0)));
                list.add(r);
            }
            for (int i = 1; i < groups.getSize() - 1; i++) {
                r = styleFactory.createRule();
                if(groups.getMin(i).equals(groups.getMax(i))){
                    f = CQL.toFilter(att + "=" + ff.literal(groups.getMin(i)));
                    r.setTitle( ff.literal(groups.getMin(i)).toString());
                    r.setFilter(f);
                    list.add(r);
                }else{
                    f = CQL.toFilter(att + ">" + ff.literal(groups.getMin(i)) + " AND " + att + " <=" + ff.literal(groups.getMax(i)));
                    r.setTitle(" > " + ff.literal(groups.getMin(i)) + " AND <= " + ff.literal(groups.getMax(i)));
                    r.setFilter(f);
                    list.add(r);
                }
            }
            /* Last class */
            r = styleFactory.createRule();
            if(groups.getMin(groups.getSize() - 1).equals(groups.getMax(groups.getSize() - 1))){
                f = CQL.toFilter(att + "=" + ff.literal(groups.getMin(groups.getSize() - 1)));
                r.setFilter(f);
                r.setTitle( ff.literal(groups.getMin(groups.getSize() - 1)).toString());
                list.add(r);
            }else{
                f = CQL.toFilter(att + ">" + ff.literal(groups.getMin(groups.getSize() - 1)));
                r.setFilter(f);
                r.setTitle(" > " + ff.literal(groups.getMin(groups.getSize() - 1)));
                list.add(r);
            }
            return list;
        } catch (CQLException e) {
        	if (LOGGER.isLoggable(Level.INFO))
				LOGGER.log(Level.INFO, "Failed to build Open Ranged rules"
						+ e.getLocalizedMessage(), e);
        }
        return null;
    }

	/**
	 * Generate Rules from Rangedclassifier groups
	 * build a List of rules
	 * @param groups
	 * @param property
	 * @return
	 */
	private List<Rule> closedRangedRules(RangedClassifier groups, String property) {

		Rule r;
		Filter f;
		List<Rule> list = new ArrayList();
		PropertyName att = ff.property(property);
		try {
			/* First class */
			r = styleFactory.createRule();
			for (int i = 0; i < groups.getSize(); i++) {
				r = styleFactory.createRule();
				if(i > 0 && groups.getMax(i).equals(groups.getMax(i -1)))
				    continue;
				if(groups.getMin(i).equals(groups.getMax(i))){
					f = CQL.toFilter(att + "=" + ff.literal(groups.getMin(i)));
					r.setTitle( ff.literal(groups.getMin(i)).toString());
					r.setFilter(f);
					list.add(r);
				} else {
					f = CQL.toFilter(att + (i == 0 ? ">=" : ">") + ff.literal(groups.getMin(i)) + " AND " + att + " <=" + ff.literal(groups.getMax(i)));
					r.setTitle((i == 0 ? " >= " : " > ") + ff.literal(groups.getMin(i)) + " AND <= " + ff.literal(groups.getMax(i)));
					r.setFilter(f);
					list.add(r);
				}
			}
			return list;
		} catch (CQLException e) {
			if (LOGGER.isLoggable(Level.INFO))
				LOGGER.log(Level.INFO, "Failed to build closed Ranged Rules"
						+ e.getLocalizedMessage(), e);
		}
		return null;
	}

	/**
	 * Generate Rules from Explicit classifier groups
	 * build a List of rules
	 * @param groups
	 * @param property
	 * @return
	 */
	private List<Rule> explicitRules(ExplicitClassifier groups, String property) {

		Rule r;
		Filter f;
		List<Rule> list = new ArrayList();
		PropertyName att = ff.property(property);
		String szFilter = "";
		String szTitle = "";
		Literal val;

		try {
			for (int i = 0; i < groups.getSize(); i++) {
				r = styleFactory.createRule();
				Set ls = groups.getValues(i);
				Iterator it = ls.iterator();
				val = ff.literal(it.next());
				szFilter = att + "=\'" + val + "\'";
				szTitle = "" + val;

				while (it.hasNext()) {
					val = ff.literal(it.next());
					szFilter += " OR " + att + "=\'" + val + "\'";
					szTitle += " OR " + val;
				}
				f = CQL.toFilter(szFilter);
				r.setTitle(szTitle);
				r.setFilter(f);
				list.add(r);
			}
			return list;
		} catch (CQLException e) {
			if (LOGGER.isLoggable(Level.INFO))
				LOGGER.log(Level.INFO, "Failed to build explicit Rules"
						+ e.getLocalizedMessage(), e);
		}
		return null;

	}
}
