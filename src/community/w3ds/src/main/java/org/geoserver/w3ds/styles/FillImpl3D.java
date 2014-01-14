package org.geoserver.w3ds.styles;



	import java.util.logging.Logger;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.styling.Fill;
import org.geotools.styling.FillImpl;
import org.geotools.styling.Graphic;
import org.geotools.util.Utilities;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.style.StyleVisitor;
import org.opengis.util.Cloneable;

	/**
	 * 
	 * @source $URL:
	 *         http://svn.osgeo.org/geotools/trunk/modules/library/main/src/main
	 *         /java/org/geotools/styling/FillImpl.java $
	 * @version $Id: FillImpl.java 37292 2011-05-25 03:24:35Z mbedward $
	 * @author James Macgill, CCG
	 */
	public class FillImpl3D implements Fill, Cloneable {
		/**
		 * The logger for the default core module.
		 */
		private static final Logger LOGGER = org.geotools.util.logging.Logging
				.getLogger("org.geotools.core");
		private FilterFactory filterFactory;
		private Expression color = null;
		private Expression backgroundColor = null;
		private Expression opacity = null;
		private Graphic graphicFill = null;

		/*************************/
		/** PROVISORY SUGESTION **/
		/*************************/

		private Expression diffuseColor;
		private Expression textureURL;
		private Expression emissiveColor;

		public void setDiffuseColor(Expression diffuseColor) {
			this.diffuseColor = diffuseColor;
		}

		public void setDiffuseColor(String diffuseColor) {
			setDiffuseColor(filterFactory.literal(diffuseColor));
		}
		
		public void setTextureUrl(Expression TextureURL) {
			this.textureURL = TextureURL;
		}

		public void setTextureUrl(String TextureURL) {
			setTextureUrl(filterFactory.literal(TextureURL));
		}
		
		public void setEmissiveColor(Expression emissiveColor) {
			this.emissiveColor = emissiveColor;
		}

		public void setEmissiveColor(String emissiveColor) {
			setEmissiveColor(filterFactory.literal(emissiveColor));
		}

		/*************************/

		/** Creates a new instance of DefaultFill */
		protected FillImpl3D() {
			this(CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints()));
		}

		public FillImpl3D(FilterFactory factory) {
			filterFactory = factory;
		}

		public void setFilterFactory(FilterFactory factory) {
			filterFactory = factory;
		}

		/**
		 * This parameter gives the solid color that will be used for a Fill.<br>
		 * The color value is RGB-encoded using two hexidecimal digits per
		 * primary-color component, in the order Red, Green, Blue, prefixed with the
		 * hash (#) sign. The hexidecimal digits between A and F may be in either
		 * upper or lower case. For example, full red is encoded as "#ff0000" (with
		 * no quotation marks). The default color is defined to be 50% gray
		 * ("#808080").
		 * 
		 * Note: in CSS this parameter is just called Fill and not Color.
		 * 
		 * @return The color of the Fill encoded as a hexidecimal RGB value.
		 */
		public Expression getColor() {
			return color;
		}

		/**
		 * This parameter gives the solid color that will be used for a Fill.<br>
		 * The color value is RGB-encoded using two hexidecimal digits per
		 * primary-color component, in the order Red, Green, Blue, prefixed with the
		 * hash (#) sign. The hexidecimal digits between A and F may be in either
		 * upper or lower case. For example, full red is encoded as "#ff0000" (with
		 * no quotation marks).
		 * 
		 * Note: in CSS this parameter is just called Fill and not Color.
		 * 
		 * @param rgb
		 *            The color of the Fill encoded as a hexidecimal RGB value.
		 */
		public void setColor(Expression rgb) {
			if (color == rgb)
				return;
			color = rgb;
		}

		public void setColor(String rgb) {
			if (color.toString() == rgb)
				return;

			setColor(filterFactory.literal(rgb));
		}

		/**
		 * This parameter gives the solid color that will be used as a background
		 * for a Fill.<br>
		 * The color value is RGB-encoded using two hexidecimal digits per
		 * primary-color component, in the order Red, Green, Blue, prefixed with the
		 * hash (#) sign. The hexidecimal digits between A and F may be in either
		 * upper or lower case. For example, full red is encoded as "#ff0000" (with
		 * no quotation marks). The default color is defined to be transparent.
		 * 
		 * 
		 * @return The color of the Fill encoded as a hexidecimal RGB value.
		 */
		public Expression getBackgroundColor() {
			return backgroundColor;
		}

		/**
		 * This parameter gives the solid color that will be used as a background
		 * for a Fill.<br>
		 * The color value is RGB-encoded using two hexidecimal digits per
		 * primary-color component, in the order Red, Green, Blue, prefixed with the
		 * hash (#) sign. The hexidecimal digits between A and F may be in either
		 * upper or lower case. For example, full red is encoded as "#ff0000" (with
		 * no quotation marks).
		 * 
		 * 
		 * 
		 * @param rgb
		 *            The color of the Fill encoded as a hexidecimal RGB value.
		 */
		public void setBackgroundColor(Expression rgb) {
			if (this.backgroundColor == rgb)
				return;
			backgroundColor = rgb;
		}

		public void setBackgroundColor(String rgb) {
			LOGGER.fine("setting bg color with " + rgb + " as a string");
			if (backgroundColor.toString() == rgb)
				return;

			setBackgroundColor(filterFactory.literal(rgb));
		}

		/**
		 * This specifies the level of translucency to use when rendering the fill. <br>
		 * The value is encoded as a floating-point value between 0.0 and 1.0 with
		 * 0.0 representing totally transparent and 1.0 representing totally opaque,
		 * with a linear scale of translucency for intermediate values.<br>
		 * For example, "0.65" would represent 65% opacity. The default value is 1.0
		 * (opaque).
		 * 
		 * @return The opacity of the fill, where 0.0 is completely transparent and
		 *         1.0 is completely opaque.
		 */
		public Expression getOpacity() {
			return opacity;
		}

		/**
		 * Setter for property opacity.
		 * 
		 * @param opacity
		 *            New value of property opacity.
		 */
		public void setOpacity(Expression opacity) {
			if (this.opacity == opacity)
				return;

			this.opacity = opacity;
		}

		public void setOpacity(String opacity) {
			if (this.opacity.toString() == opacity)
				return;

			setOpacity(filterFactory.literal(opacity));
		}

		/**
		 * This parameter indicates that a stipple-fill repeated graphic will be
		 * used and specifies the fill graphic to use.
		 * 
		 * @return graphic The graphic to use as a stipple fill. If null then no
		 *         Stipple fill should be used.
		 */
		public org.geotools.styling.Graphic getGraphicFill() {
			return graphicFill;
		}

		/**
		 * Setter for property graphic.
		 * 
		 * @param graphicFill
		 *            New value of property graphic.
		 */
		public void setGraphicFill(org.opengis.style.Graphic graphicFill) {
			if (this.graphicFill == graphicFill)
				return;
			this.graphicFill = null;
		}

		public Object accept(StyleVisitor visitor, Object data) {
			return visitor.visit(this, data);
		}

		public void accept(org.geotools.styling.StyleVisitor visitor) {
			visitor.visit(this);
		}

		/**
		 * Returns a clone of the FillImpl.
		 * 
		 * @see org.geotools.styling.Fill#clone()
		 */
		public Object clone() {
			try {
				FillImpl3D clone = (FillImpl3D) super.clone();
				if (graphicFill != null) {
					clone.graphicFill = (Graphic) ((Cloneable) graphicFill).clone();
				}
				return clone;
			} catch (CloneNotSupportedException e) {
				// This will never happen
				throw new RuntimeException("Failed to clone FillImpl");
			}
		}

		/**
		 * Generates a hashcode for the FillImpl.
		 * 
		 * @return The hashcode.
		 */
		public int hashCode() {
			final int PRIME = 1000003;
			int result = 0;
			if (color != null) {
				result = PRIME * result + color.hashCode();
			}
			if (backgroundColor != null) {
				result = PRIME * result + backgroundColor.hashCode();
			}
			if (opacity != null) {
				result = PRIME * result + opacity.hashCode();
			}
			if (graphicFill != null) {
				result = PRIME * result + graphicFill.hashCode();
			}

			return result;
		}

		/**
		 * Compares a FillImpl with another for equality.
		 * 
		 * <p>
		 * Two FillImpls are equal if they contain the same, color, backgroundcolor,
		 * opacity and graphicFill.
		 * 
		 * @param oth
		 *            The other FillImpl
		 * @return True if this FillImpl is equal to oth.
		 */
		public boolean equals(Object oth) {
			if (this == oth) {
				return true;
			}

			if (oth instanceof FillImpl) {
				FillImpl other = (FillImpl) oth;
				return Utilities.equals(this.color, other.getColor())
						&& Utilities.equals(this.backgroundColor,
								other.getBackgroundColor())
						&& Utilities.equals(this.opacity, other.getOpacity())
						&& Utilities.equals(this.graphicFill, other.getGraphicFill());
			}

			return false;
		}

		static FillImpl3D cast(org.opengis.style.Fill fill) {
			if (fill == null) {
				return null;
			} else if (fill instanceof FillImpl) {
				return (FillImpl3D) fill;
			} else {
				FillImpl3D copy = new FillImpl3D();
				copy.color = fill.getColor();
				copy.graphicFill = null;
				copy.opacity = fill.getOpacity();
				copy.backgroundColor = null; // does not have an equivalent
				if(fill.getClass().getName().equalsIgnoreCase(FillImpl3D.class.getName())) {
					FillImpl3D fill_ = (FillImpl3D) fill;
					copy.diffuseColor = fill_.getDiffuseColor();
					copy.emissiveColor = fill_.getEmissiveColor();
					copy.textureURL = fill_.getTextureUrl();
				}
				return copy;
			}
		}

		/*************************/
		/** PROVISORY SUGESTION **/
		/*************************/

		public Expression getDiffuseColor() {
			return this.diffuseColor;
		}

		public Expression getTextureUrl() {
			return this.textureURL;
		}
		
		public Expression getEmissiveColor() {
			return this.emissiveColor;
		}
	}
