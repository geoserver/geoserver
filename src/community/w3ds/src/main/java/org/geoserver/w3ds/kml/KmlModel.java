/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Nuno Oliveira - PTInovacao
 */

package org.geoserver.w3ds.kml;

import org.geoserver.w3ds.styles.ModelImpl;
import org.geoserver.w3ds.utilities.StyleFilter;
import org.geotools.styling.Rule;
import org.opengis.feature.Feature;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.Model;
import de.micromata.opengis.kml.v_2_2_0.RefreshMode;

public class KmlModel {

	private Rule rule;
	private ModelImpl model;
	private StyleFilter styleFilter;

	public KmlModel(Rule rule, ModelImpl model) {
		this.rule = rule;
		this.model = model;
		styleFilter = new StyleFilter(rule.getFilter());
	}

	public boolean acceptFeature(Feature feature) {
		return this.styleFilter.match(feature);
	}

	public Model getKmlModel(Feature feature, Point point) {
		Model kmlModel = KmlFactory.createModel()
				.withId(feature.getName().toString())
				.withAltitudeMode(getAltitudeMode(feature, point));
		kmlModel.createAndSetLocation().withLongitude(point.getCoordinate().x)
				.withLatitude(point.getCoordinate().y)
				.withAltitude(getAltitude(feature, point));
		kmlModel.createAndSetOrientation()
				.withHeading(
						getGenericRotationValue(feature, model.getHeading()))
				.withTilt(getGenericRotationValue(feature, model.getTilt()))
				.withRoll(getGenericRotationValue(feature, model.getRoll()));
		kmlModel.createAndSetLink()
				.withHref(
						styleFilter.getExpressionValue(model.getHref(), feature))
				.withRefreshMode(RefreshMode.ON_CHANGE);
		return kmlModel;
	}

	private double getAltitude(Feature feature, Point point) {
		String altituteText = styleFilter.getExpressionValue(
				model.getAltitude(), feature);
		if (altituteText == null) {
			return point.getCoordinate().z;
		}
		return Float.valueOf(altituteText);

	}

	private AltitudeMode getAltitudeMode(Feature feature, Point point) {
		String altitudeModelText = styleFilter.getExpressionValue(
				model.getAltitudeModel(), feature);

		if (altitudeModelText == null) {
			return AltitudeMode.CLAMP_TO_GROUND;
		}
		int altitudeModel = ModelImpl.decodeAltitudeModel(altitudeModelText);
		switch (altitudeModel) {
		case 0:
			return AltitudeMode.ABSOLUTE;
		case 1:
			return AltitudeMode.CLAMP_TO_GROUND;
		case 2:
			return AltitudeMode.CLAMP_TO_SEA_FLOOR;
		case 3:
			return AltitudeMode.RELATIVE_TO_GROUND;
		case 4:
			return AltitudeMode.RELATIVE_TO_SEA_FLOOR;
		default:
			return AltitudeMode.CLAMP_TO_GROUND;
		}
	}

	private float getGenericRotationValue(Feature feature,
			Expression rotationExpression) {
		String rotationText = styleFilter.getExpressionValue(
				rotationExpression, feature);
		if (rotationText == null) {
			return 0;
		}
		return Float.valueOf(rotationText);
	}
}
