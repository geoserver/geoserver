package org.geotools.renderer.lite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Arrays;

import org.geotools.feature.FeatureCollection;
import org.geotools.map.Layer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.junit.Test;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.expression.Expression;

public class VectorMapRenderUtilsTest {

	@Test
	public void testTransformIfNotNecessary() throws IOException {
		Layer layer = mock(Layer.class);
		Style style = mock(Style.class);
		when(layer.getStyle()).thenReturn(style);
		Rectangle paintArea = mock(Rectangle.class);

		double mapScale = 5.0;
		FeatureType schema = mock(FeatureType.class);
		FeatureCollection<?, ?> features = mock(FeatureCollection.class);
		FeatureCollection<?, ?> transformedFeatures = VectorMapRenderUtils.transformIfNecessary(layer, paintArea,
				mapScale, schema, features);
		assertEquals(transformedFeatures, features);
	}

	@Test
	public void testTransformIfNecessary() throws IOException {
		Layer layer = mock(Layer.class);
		Style style = mock(Style.class);
		when(layer.getStyle()).thenReturn(style);

		FeatureTypeStyle featureTypeStyle = mock(FeatureTypeStyle.class);
		Expression expression = mock(Expression.class);
		when(featureTypeStyle.getTransformation()).thenReturn(expression);
		when(style.featureTypeStyles()).thenReturn(Arrays.asList(featureTypeStyle));
		Rule rule = mock(Rule.class);
		when(rule.getMaxScaleDenominator()).thenReturn(10.0);
		when(featureTypeStyle.rules()).thenReturn(Arrays.asList(rule));

		Rectangle paintArea = mock(Rectangle.class);

		double mapScale = 5.0;
		FeatureType schema = mock(FeatureType.class);
		FeatureCollection<?, ?> features = mock(FeatureCollection.class);
		FeatureCollection<?, ?> transformedFeatures = VectorMapRenderUtils.transformIfNecessary(layer, paintArea,
				mapScale, schema, features);
		verify(expression).evaluate(eq(features));
		assertNull(transformedFeatures);
	}

}
