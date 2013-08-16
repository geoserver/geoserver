/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Nuno Oliveira - PTInovacao
 */

package org.geoserver.w3ds.utilities;

import java.io.UnsupportedEncodingException;

import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.IsEqualsToImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;

public class StyleFilter {
	private Filter filter;

	public StyleFilter(Filter filter) {
		this.filter = filter;
	}

	public Filter getFilter() {
		return filter;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	public boolean match(Feature feature) {
		if (this.filter == null)
			return true;
		if (filter.getClass().isAssignableFrom(IsEqualsToImpl.class)) {
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

	public String getExpressionValue(Expression expression, Feature feature) {
		if ((expression == null) || (feature == null)) {
			return null;
		}
		if (expression.getClass().isAssignableFrom(LiteralExpressionImpl.class)) {
			LiteralExpressionImpl v = (LiteralExpressionImpl) expression;
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
		if (expression.getClass().isAssignableFrom(
				AttributeExpressionImpl.class)) {
			AttributeExpressionImpl v = (AttributeExpressionImpl) expression;
			String property = v.getPropertyName();
			Object o = feature.getProperty(property).getValue();
			if (o != null) {
				return o.toString();
			}
		}
		return "";
	}
}
