/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.measure.unit.Unit;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.w3ds.types.GetTileRequest;
import org.geoserver.w3ds.types.Scene;
import org.geoserver.w3ds.types.W3DSLayerInfo;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.IllegalFilterException;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.geometry.jts.LiteCoordinateSequenceFactory;
import org.geotools.referencing.CRS;
import org.geotools.resources.CRSUtilities;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GetTile {

	private GeoServer geoServer;
	private Catalog catalog;
	private GetTileRequest request;
	private Scene scene;

	protected static Logger LOGGER = org.geotools.util.logging.Logging
			.getLogger("org.geoserver.ows");

	public GetTile(GeoServer geoServer, Catalog catalog,
			GetTileRequest request, Scene scene) {
		this.geoServer = geoServer;
		this.catalog = catalog;
		this.request = request;
		this.scene = scene;
	}

	public GetTile(GeoServer geoServer, Catalog catalog, GetTileRequest request) {
		this.geoServer = geoServer;
		this.catalog = catalog;
		this.request = request;
		this.scene = new Scene();
	}

	public void run() throws IOException {
		W3DSLayerInfo layerInfo = request.getLayer();
		List<Style> styles = new ArrayList<Style>();
		for (StyleInfo si : layerInfo.getLayerInfo().getStyles()) {
			styles.add(si.getStyle());
		}
		String name = layerInfo.getLayerInfo().getResource().getName();
		FeatureTypeInfo feature = catalog.getFeatureTypeByName(layerInfo
				.getLayerInfo().getResource().getName());
		Query query = createQuery((SimpleFeatureSource) feature
				.getFeatureSource(null, null));
		scene.add(layerInfo,
				feature.getFeatureSource(null, null).getFeatures(query), styles, catalog);
	}

	public Query createQuery(SimpleFeatureSource source) {
		FilterFactory filterFactory = (FilterFactory) CommonFactoryFinder
				.getFilterFactory(null);
		Filter filter = Filter.INCLUDE;
		CoordinateReferenceSystem crs = source.getSchema()
				.getCoordinateReferenceSystem();
		CoordinateReferenceSystem declaredCRS = this.request.getCrs();
		Filter transformedFilter = filter;
		Unit<?> unit = CRSUtilities.getUnit(crs.getCoordinateSystem());
		if (declaredCRS != null) {
			DefaultCRSFilterVisitor defaultVisitor = new DefaultCRSFilterVisitor(
					CommonFactoryFinder.getFilterFactory2(GeoTools
							.getDefaultHints()), declaredCRS);
			Filter defaulted = (Filter) filter.accept(defaultVisitor, null);
			ReprojectingFilterVisitor visitor = new ReprojectingFilterVisitor(
					CommonFactoryFinder.getFilterFactory2(GeoTools
							.getDefaultHints()), source.getSchema());
			transformedFilter = (Filter) filter.accept(visitor, null);
		}
		Filter gridFilter = createGridFilter(source.getSchema(), filterFactory, request);
		filter = filterFactory.and(transformedFilter, gridFilter);
		Query query = new Query(source.getName().getLocalPart(), filter);
		CoordinateReferenceSystem target = declaredCRS;
		if (target != null && declaredCRS != null
				&& !CRS.equalsIgnoreMetadata(crs, target)) {
			query.setCoordinateSystemReproject(target);
		}
		final Hints hints = new Hints();
		hints.put(Hints.JTS_COORDINATE_SEQUENCE_FACTORY,
				new LiteCoordinateSequenceFactory());
		hints.put(Query.INCLUDE_MANDATORY_PROPS, true);
		hints.put(Hints.FEATURE_2D, Boolean.FALSE);
		query.setHints(hints);
		return query;
	}

	private static Filter createGridFilter(SimpleFeatureType schema, FilterFactory filterFactory, GetTileRequest request)
			throws IllegalFilterException {
		PropertyName tilerow = new AttributeExpressionImpl("tilerow");
		Literal tilerow_value = filterFactory.literal(request.getTileRow());
		Filter tilerow_filter = filterFactory.equals(tilerow, tilerow_value);
		PropertyName tilecol = new AttributeExpressionImpl("tilecol");
		Literal tilecol_value = filterFactory.literal(request.getTileCol());
		Filter tilecol_filter = filterFactory.equals(tilecol, tilecol_value);
		PropertyName tilelevel = new AttributeExpressionImpl("tilelevel");
		Literal tilelevel_value = filterFactory.literal(request.getTileLevel());
		Filter tilelevel_filter = filterFactory.equals(tilelevel, tilelevel_value);
		Filter filter = filterFactory.and(tilerow_filter, tilecol_filter);
		return filterFactory.and(filter, tilelevel_filter);
	}

	public Scene getScene() {
		return scene;
	}

}
