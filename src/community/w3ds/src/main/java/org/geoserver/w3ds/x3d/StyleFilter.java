/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.x3d;

import java.io.UnsupportedEncodingException;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.IsEqualsToImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;

public class StyleFilter {
	
	private String name;
	private Filter filter;

	public StyleFilter(String name, Filter filter) {
		this.name = name;
		this.filter = filter;
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

	public boolean match(Feature feature) {
		if(this.filter == null) return true;
		if (filter.getClass().isAssignableFrom(IsEqualsToImpl.class)) {
			IsEqualsToImpl equal = (IsEqualsToImpl) filter;
			String v1 = this.getExpressionValue(equal.getExpression1(),
					feature);
			v1 = v1.trim();
			String v2 = this.getExpressionValue(equal.getExpression2(),
					feature);
			return v1.equalsIgnoreCase(v2);
		}
		return false;
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
			Object o = feature.getProperty(property).getValue();
			if (o != null) {
				return o.toString();
			}
		}
		return "";
	}
}
