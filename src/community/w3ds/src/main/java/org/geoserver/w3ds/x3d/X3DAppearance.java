/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.x3d;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.geoserver.w3ds.styles.FillImpl3D;
import org.geoserver.w3ds.styles.PolygonSymbolizerImpl3D;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.IsEqualsToImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.styling.Fill;
import org.geotools.styling.Rule;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;


public class X3DAppearance {

	public X3DNode appearance;
	public X3DAttribute def;
	public X3DNode material;
	public X3DAttribute ambientIntensity;
	public X3DAttribute diffuseColor;
	public X3DAttribute emissiveColor;
	public X3DAttribute shininess;
	public X3DAttribute specularColor;
	public X3DAttribute transparency;
	public X3DAttribute materialMetadata;
	public X3DNode imageTexture;
	public X3DAttribute repeatS;
	public X3DAttribute repeatT;
	public X3DAttribute url;
	public X3DAttribute textureMetadata;

	private String name;
	private Filter filter;
	private Rule rule;

	public X3DAppearance() {
		this.appearance = new X3DNode("Appearance");
		this.def = new X3DAttribute("DEF", "");
		this.def.setValid(false);
		setDefaultMaterial();
		setTextureDefault();
		this.appearance.addX3DNode(imageTexture);
		this.appearance.addX3DNode(material);
		this.name = "";
		this.filter = null;
		this.rule = null;
	}

	public X3DAppearance(String name, Filter filter, Rule rule) {
		this.appearance = new X3DNode("Appearance");
		this.def = new X3DAttribute("DEF", "");
		this.def.setValid(false);
		setTextureDefault();
		setDefaultMaterial();
		this.appearance.addX3DNode(material);
		this.appearance.addX3DNode(imageTexture);
		this.name = name;
		this.filter = filter;
		this.rule = rule;
	}

	public void resetMaterial() {
		this.ambientIntensity.setValue("0.2");
		this.diffuseColor.setValue("0.8 0.8 0.8");
		this.emissiveColor.setValue("0 0 0");
		this.shininess.setValue("0.2");
		this.specularColor.setValue("0 0 0");
		this.transparency.setValue("0.0");
		this.materialMetadata.setValue("");
	}

	public void setDefaultMaterial() {
		this.material = new X3DNode("Material");
		this.ambientIntensity = new X3DAttribute("ambientIntensity", "0.2");
		this.diffuseColor = new X3DAttribute("diffuseColor", "0.8 0.8 0.8");
		this.emissiveColor = new X3DAttribute("emissiveColor", "0 0 0");
		this.shininess = new X3DAttribute("shininess", "0.2");
		this.specularColor = new X3DAttribute("specularColor", "0 0 0");
		this.transparency = new X3DAttribute("transparency", "0.0");
		this.materialMetadata = new X3DAttribute("metadata", "");
		this.materialMetadata.setValid(false);
		this.material.addX3DAttribute(this.ambientIntensity);
		this.material.addX3DAttribute(this.diffuseColor);
		this.material.addX3DAttribute(this.emissiveColor);
		this.material.addX3DAttribute(this.shininess);
		this.material.addX3DAttribute(this.specularColor);
		this.material.addX3DAttribute(this.transparency);
		this.material.addX3DAttribute(this.materialMetadata);
	}

	public void resetTexture() {
		this.repeatS.setValue("true");
		this.repeatT.setValue("true");
		this.url.setValue("");
		this.textureMetadata.setValue("");
	}

	public void setTextureDefault() {
		this.imageTexture = new X3DNode("ImageTexture");
		this.repeatS = new X3DAttribute("repeatS", "true");
		this.repeatT = new X3DAttribute("repeatT", "true");
		this.url = new X3DAttribute("url", "");
		this.textureMetadata = new X3DAttribute("metadata", "");
		this.textureMetadata.setValid(false);
		this.imageTexture.addX3DAttribute(this.repeatS);
		this.imageTexture.addX3DAttribute(this.repeatT);
		this.imageTexture.addX3DAttribute(this.url);
		this.imageTexture.addX3DAttribute(this.textureMetadata);
		this.imageTexture.setValid(false);
	}

	public void setTexture(String url) {
		this.imageTexture.setValid(true);
		this.url.setValue(url);
	}

	public void setDEF(String name) {
		this.def.setValue(name);
	}

	public String toString() {
		return this.appearance.toString();
	}

	public String toStringSpaces() {
		return this.appearance.toStringSpaces("");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Filter getFilter() {
		return filter;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	public Rule getRule() {
		return rule;
	}

	public void setRule(Rule rule) {
		this.rule = rule;
	}

	public boolean match(Feature feature) {
		if (this.filter == null)
			return true;
		if (this.filter.getClass().isAssignableFrom(IsEqualsToImpl.class)) {
			IsEqualsToImpl equal = (IsEqualsToImpl) filter;
			String v1 = this
					.getExpressionValue(equal.getExpression1(), feature);
			v1 = v1.trim();
			String v2 = this
					.getExpressionValue(equal.getExpression2(), feature);
			return v1.equalsIgnoreCase(v2);
		}
		return false;
	}

	public void update(Fill fill_, Feature feature) {

		// Provisory hack to have 3D style (see Styles3D)
		if (fill_.getClass().getName().equalsIgnoreCase(FillImpl3D.class.getName())) {
			FillImpl3D fill = (FillImpl3D) fill_;
			if (fill.getDiffuseColor() != null) {
				String value = getExpressionValue(fill.getDiffuseColor(),
						feature);
				if (value != null) {
					this.diffuseColor.setValue(value);
				}
			}
			if (fill.getTextureUrl() != null) {
				String value = getExpressionValue(fill.getTextureUrl(), feature);
				if (value != null) {
					this.url.setValue(value);
					this.imageTexture.setValid(true);
					this.material.setValid(false);
				}
			}
			if (fill.getEmissiveColor() != null) {
				String value = getExpressionValue(fill.getEmissiveColor(),
						feature);
				if (value != null) {
					this.emissiveColor.setValue(value);
				}
			}
		}
	}

	public X3DNode getX3dNode(Feature feature) {
		resetMaterial();
		resetTexture();
		List<Symbolizer> symbolizers = rule.symbolizers();
		for (Symbolizer s : symbolizers) {
			if (s.getClass().isAssignableFrom(PolygonSymbolizerImpl3D.class)) {
				PolygonSymbolizerImpl3D p = (PolygonSymbolizerImpl3D) s;
				update(p.getFill(), feature);
			}
		}
		return appearance.clone();
	}

	private String getExpressionValue(Expression exp, Feature feature) {
		if (exp.getClass().isAssignableFrom(LiteralExpressionImpl.class)) {
			LiteralExpressionImpl v = (LiteralExpressionImpl) exp;
			byte[] utf8Bytes = null;
			String result = "";
			try {
				utf8Bytes = v.toString().getBytes("UTF8");
				result = new String(utf8Bytes, "UTF8");
			} catch (UnsupportedEncodingException e) {
				return "";
			}
			return result;

		}
		if (exp.getClass().isAssignableFrom(AttributeExpressionImpl.class)) {
			AttributeExpressionImpl v = (AttributeExpressionImpl) exp;
			String property = v.getPropertyName();
			Property p = feature.getProperty(property);
			if (p == null) {
				return "";
			}
			Object o = p.getValue();
			if (o != null) {
				return o.toString();
			}
		}
		return "";
	}

}
