/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Nuno Oliveira - PTInovacao
 */

package org.geoserver.w3ds.x3d;

import org.geoserver.w3ds.styles.ModelImpl;
import org.geoserver.w3ds.utilities.StyleFilter;
import org.geotools.styling.Rule;
import org.opengis.feature.Feature;
import org.opengis.filter.expression.Expression;

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
		X3DNode rootNode = modelPosition;
		modelPosition.addX3DNode(modelInline);
		modelInline.addX3DAttribute("url",
				styleFilter.getExpressionValue(model.getHref(), feature));
		Coordinate coordinate = point.getCoordinate();
		modelPosition.addX3DAttribute("translation", coordinate.y + " "
				+ coordinate.z + " " + coordinate.x);
		X3DNode rollRotation = getGenericRotation(feature, model.getRoll(),
				"1 0 0");
		if (rollRotation != null) {
			rollRotation.addX3DNode(rootNode.clone());
			rootNode = rollRotation;
		}
		X3DNode titlRotation = getGenericRotation(feature, model.getTilt(),
				"0 0 1");
		if (titlRotation != null) {
			titlRotation.addX3DNode(rootNode.clone());
			rootNode = titlRotation;
		}
		X3DNode headingRotation = getGenericRotation(feature,
				model.getHeading(), "0 1 0");
		if (headingRotation != null) {
			headingRotation.addX3DNode(rootNode.clone());
			rootNode = headingRotation;
		}
		X3DNode altituteTranslation = getAltitudeTranslation(feature, point);
		if (altituteTranslation != null) {
			altituteTranslation.addX3DNode(rootNode.clone());
			rootNode = altituteTranslation;
		}
		return rootNode;
	}

	private X3DNode getAltitudeTranslation(Feature feature, Point point) {
		String altituteText = styleFilter.getExpressionValue(
				model.getAltitude(), feature);
		if (altituteText == null) {
			return null;
		}
		float altitude = computeAltitudeForAltitudeMode(feature, point,
				Float.valueOf(altituteText));
		X3DNode translation = new X3DNode("Transform");
		translation.addX3DAttribute("translation", "0 " + altitude + " 0");
		return translation;

	}

	private float computeAltitudeForAltitudeMode(Feature feature, Point point,
			float altitude) {
		String altitudeModelText = styleFilter.getExpressionValue(
				model.getAltitudeModel(), feature);
		if (altitudeModelText == null) {
			return 0;
		}
		int altitudeModel = ModelImpl.decodeAltitudeModel(altitudeModelText);
		switch (altitudeModel) {
		case 0:
			return altitude - point.getCoordinate().Z;
		case 1:
			return 0;
		case 2:
			return 0;
		case 3:
			return altitude;
		case 4:
			return altitude;
		default:
			return 0;
		}
	}

	private X3DNode getGenericRotation(Feature feature,
			Expression rotationExpression, String rotationAxe) {
		String rotationText = styleFilter.getExpressionValue(
				rotationExpression, feature);
		if (rotationText == null) {
			return null;
		}
		float rotationValue = Float.valueOf(rotationText);
		X3DNode rotation = new X3DNode("Transform");
		rotation.addX3DAttribute("rotation", rotationAxe + rotationValue);
		return rotation;
	}
}
