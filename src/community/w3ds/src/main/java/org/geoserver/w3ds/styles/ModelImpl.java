/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Nuno Oliveira - PTInovacao
 */

package org.geoserver.w3ds.styles;

import org.geotools.styling.GraphicImpl;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.style.AnchorPoint;

public class ModelImpl extends GraphicImpl {

	public static int ALTITUDE_MODEL_AINVALID = -1;
	public static int ALTITUDE_MODEL_ABSOLUTE = 0;
	public static int ALTITUDE_MODEL_CLAMPTOGROUND = 1;
	public static int ALTITUDE_MODEL_CLAMPTOSEAFLOOR = 2;
	public static int ALTITUDE_MODEL_RELATIVETOGROUND = 3;
	public static int ALTITUDE_MODEL_RELATIVETOSEAFLOOR = 4;

	public static String ALTITUDE_MODEL_ABSOLUTE_TEXT = "absolute";
	public static String ALTITUDE_MODEL_CLAMPTOGROUND_TEXT = "clampToGround";
	public static String ALTITUDE_MODEL_CLAMPTOSEAFLOOR_TEXT = "clampToSeaFloor";
	public static String ALTITUDE_MODEL_RELATIVETOGROUND_TEXT = "relativeToGround";
	public static String ALTITUDE_MODEL_RELATIVETOSEAFLOOR_TEXT = "relativeToSeaFloor";

	private Expression altitudeModel;
	private Expression altitude;
	private Expression heading;
	private Expression tilt;
	private Expression roll;
	private Expression href;
	private Expression label;

	public ModelImpl() {
		super();
	}

	public ModelImpl(FilterFactory factory, AnchorPoint anchor, Expression gap,
			Expression initialGap) {
		super(factory, anchor, gap, initialGap);
	}

	public ModelImpl(FilterFactory factory) {
		super(factory);
	}

	public Expression getAltitudeModel() {
		return altitudeModel;
	}

	public void setAltitudeModel(Expression altitudeModel) {
		this.altitudeModel = altitudeModel;
	}

	public Expression getAltitude() {
		return altitude;
	}

	public void setAltitude(Expression altitude) {
		this.altitude = altitude;
	}

	public Expression getHeading() {
		return heading;
	}

	public void setHeading(Expression heading) {
		this.heading = heading;
	}

	public Expression getTilt() {
		return tilt;
	}

	public void setTilt(Expression tilt) {
		this.tilt = tilt;
	}

	public Expression getRoll() {
		return roll;
	}

	public void setRoll(Expression roll) {
		this.roll = roll;
	}

	public Expression getHref() {
		return href;
	}

	public void setHref(Expression href) {
		this.href = href;
	}

	public Expression getLabel() {
		return label;
	}

	public void setLabel(Expression label) {
		this.label = label;
	}

	public static int decodeAltitudeModel(String altitudeModelText) {
		if (altitudeModelText.equalsIgnoreCase(ALTITUDE_MODEL_ABSOLUTE_TEXT)) {
			return ALTITUDE_MODEL_ABSOLUTE;
		}
		if (altitudeModelText
				.equalsIgnoreCase(ALTITUDE_MODEL_CLAMPTOGROUND_TEXT)) {
			return ALTITUDE_MODEL_CLAMPTOGROUND;
		}
		if (altitudeModelText
				.equalsIgnoreCase(ALTITUDE_MODEL_CLAMPTOSEAFLOOR_TEXT)) {
			return ALTITUDE_MODEL_CLAMPTOSEAFLOOR;
		}
		if (altitudeModelText
				.equalsIgnoreCase(ALTITUDE_MODEL_RELATIVETOGROUND_TEXT)) {
			return ALTITUDE_MODEL_RELATIVETOGROUND;
		}
		if (altitudeModelText
				.equalsIgnoreCase(ALTITUDE_MODEL_RELATIVETOSEAFLOOR_TEXT)) {
			return ALTITUDE_MODEL_RELATIVETOSEAFLOOR;
		}
		return ALTITUDE_MODEL_AINVALID;
	}
}
