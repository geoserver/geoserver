package org.geoserver.w3ds.styles;
import org.geotools.filter.ConstantExpression;
import org.geotools.styling.GraphicImpl;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.style.AnchorPoint;

public class ModelImpl extends GraphicImpl {

		private Expression altitudeModel;
		private Expression altitude;
		private Expression heading;
		private Expression tilt;
		private Expression roll;
		private Expression href;
		private Expression label;
		
		public void setDefault() {
			this.altitudeModel = ConstantExpression.constant("clampToGround");
			this.altitude = ConstantExpression.constant(0);
			this.heading = ConstantExpression.constant(0);
			this.tilt = ConstantExpression.constant(0);
			this.roll = ConstantExpression.constant(0);
			this.href = ConstantExpression.constant("...");
			this.label = null;
		}
		
		public ModelImpl() {
			super();
		}

		public ModelImpl(FilterFactory factory, AnchorPoint anchor,
				Expression gap, Expression initialGap) {
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

}
