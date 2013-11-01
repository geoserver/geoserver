/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Nuno Oliveira - PTInovacao
 */

package org.geoserver.w3ds.x3d;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.w3ds.styles.ModelImpl;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.GraphicImpl;
import org.geotools.styling.PointSymbolizerImpl;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;

public final class X3DUtils {

	private X3DUtils() {
	}

	public static List<X3DInlineModel> getInlineModels(Style style) {
		List<X3DInlineModel> inlineModels = new ArrayList<X3DInlineModel>();
		List<FeatureTypeStyle> featureTypeStyles = style.featureTypeStyles();
		for (FeatureTypeStyle featureTypeStyle : featureTypeStyles) {
			List<Rule> rules = featureTypeStyle.rules();
			for (Rule rule : rules) {
				List<Symbolizer> symbolizers = rule.symbolizers();
				for (Symbolizer symbolizer : symbolizers) {
					if (symbolizer.getClass().isAssignableFrom(
							PointSymbolizerImpl.class)) {
						PointSymbolizerImpl pointSymbolizerImpl = (PointSymbolizerImpl) symbolizer;
						GraphicImpl graphicImpl = pointSymbolizerImpl
								.getGraphic();
						if (graphicImpl.getClass().isAssignableFrom(
								ModelImpl.class)) {
							inlineModels.add(new X3DInlineModel(rule,
									(ModelImpl) graphicImpl));
						}
					}
				}
			}
		}
		return inlineModels;
	}
}
