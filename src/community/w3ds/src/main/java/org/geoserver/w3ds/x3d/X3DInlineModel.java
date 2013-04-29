/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.x3d;

import org.geoserver.w3ds.styles.ModelImpl;
import org.geotools.styling.Rule;
import org.opengis.feature.Feature;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class X3DInlineModel {

	private Rule rule;
	private ModelImpl model;
	private StyleFilter styleFilter;

	public X3DInlineModel(Rule rule, ModelImpl model) {
		this.rule = rule;
		this.model = model;
		styleFilter = new StyleFilter(rule.getFilter());
	}

	public boolean acceptFeature(Feature feature) {
		return this.styleFilter.match(feature);
	}

	public X3DNode getInlineModel(Feature feature, Point point) {
		X3DNode modelInline = new X3DNode("Inline");
		X3DNode modelPosition = new X3DNode("Transform");
		modelPosition.addX3DNode(modelInline);
		modelInline.addX3DAttribute("url",
				styleFilter.getExpressionValue(model.getHref(), feature));
		Coordinate coordinate = point.getCoordinate();
		modelPosition.addX3DAttribute("translation", coordinate.y + " "
				+ coordinate.z + " " + coordinate.x);
		return modelPosition;
	}
}
