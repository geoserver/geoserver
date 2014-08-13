/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.x3d;

import java.util.ArrayList;
import java.util.List;

import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;

public class X3DStyles {
	
	private List<X3DAppearance> styles;
	private X3DNode defaultStyle;
	
	public X3DStyles() {
		this.styles = new ArrayList<X3DAppearance>();
		X3DAppearance defaultAppearance = new X3DAppearance();
		this.defaultStyle = defaultAppearance.appearance; 
	}

	public List<X3DAppearance> getStyles() {
		return styles;
	}

	public void setStyles(List<X3DAppearance> styles) {
		this.styles = styles;
	}

	public X3DNode getDefaultStyle() {
		return defaultStyle;
	}

	public void setDefaultStyle(X3DNode defaultStyle) {
		this.defaultStyle = defaultStyle;
	}

	public void addStyle(Style style) {
		List<FeatureTypeStyle> fStyles = style.featureTypeStyles();
		for (FeatureTypeStyle fs : fStyles) {
			List<Rule> rules = fs.rules();
			for (Rule r : rules) {
				X3DAppearance appearance = new X3DAppearance();
				String name = r.getName();
				Filter filter = r.getFilter();
				this.styles.add(new X3DAppearance(name, filter, r));
			}
		}
	}
	
	public void addStyles(List<Style> styles) {
		for(Style s : styles) {
			this.addStyle(s);
		}
	}
	
	public X3DNode getAppearance(Feature feature) { 
		for(X3DAppearance a : this.styles) {
			if(a.match(feature)) {
				return a.getX3dNode(feature);
			}
		}
		return this.defaultStyle;
	}
	
}
