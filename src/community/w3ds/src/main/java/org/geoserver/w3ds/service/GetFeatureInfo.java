/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.w3ds.types.FeatureInfo;
import org.geoserver.w3ds.types.GetFeatureInfoRequest;
import org.geoserver.w3ds.types.W3DSLayerInfo;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.filter.IllegalFilterException;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.geometry.GeometryBuilder;
import org.geotools.geometry.jts.LiteCoordinateSequenceFactory;
import org.geotools.referencing.CRS;
import org.geotools.resources.CRSUtilities;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

public class GetFeatureInfo {
	private GeoServer geoServer;
	private Catalog catalog;
	private GetFeatureInfoRequest request;
	private FeatureInfo featureInfo;

	public GetFeatureInfo(GeoServer geoServer, Catalog catalog,
			GetFeatureInfoRequest request, FeatureInfo featureInfo) {
		this.geoServer = geoServer;
		this.catalog = catalog;
		this.request = request;
		this.featureInfo = featureInfo;
	}

	public GetFeatureInfo(GeoServer geoServer, Catalog catalog,
			GetFeatureInfoRequest request) {
		this.geoServer = geoServer;
		this.catalog = catalog;
		this.request = request;
		this.featureInfo = new FeatureInfo();
	}

	public void run() throws IOException, IllegalFilterException, FactoryException {
		Iterator<W3DSLayerInfo> layersIterator = request.getLayers().iterator();
		while(layersIterator.hasNext()) {
			W3DSLayerInfo layerInfo = layersIterator.next();
			FeatureTypeInfo feature = catalog.getFeatureTypeByName(layerInfo.getLayerInfo().getName());
			Query query = createQuery((SimpleFeatureSource)feature.getFeatureSource(null, null));
			featureInfo.add(layerInfo, feature.getFeatureSource(null, null).getFeatures(query), catalog);
		}
		System.out.print("TESTE");
	}
	
	public Query createQuery(SimpleFeatureSource source) throws IllegalFilterException, FactoryException {
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		Filter filter = Filter.INCLUDE;
		CoordinateReferenceSystem crs = source.getSchema()
				.getCoordinateReferenceSystem();
		CoordinateReferenceSystem declaredCRS = this.request.getCrs();
		Filter transformedFilter = filter;
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
		Filter distanceFilter = createDistanceFilter(source.getSchema(),
				ff, request.getCoordinate(), request.getCrs());
		filter = ff.and(transformedFilter,distanceFilter);
		Query query = new Query(source.getName().getLocalPart(),
				filter);
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

	private static Filter createDistanceFilter(SimpleFeatureType schema,
			FilterFactory2 ff, Coordinate coordinate, CoordinateReferenceSystem crs)
			throws IllegalFilterException, FactoryException {
		List<Filter> filters = new ArrayList<Filter>();
		for (int j = 0; j < schema.getAttributeCount(); j++) {
			AttributeDescriptor attType = schema.getDescriptor(j);
			if (attType instanceof GeometryDescriptor) {
				Filter gfilter = getDistanceFilter(attType.getLocalName(), coordinate, crs, ff);
				filters.add(gfilter);
			}
		}

		if (filters.size() == 0)
			return Filter.INCLUDE;
		else if (filters.size() == 1)
			return (Filter) filters.get(0);
		else
			return ff.or(filters);
	}
	
	private static Filter getDistanceFilter(String propertyName, Coordinate coordinate, CoordinateReferenceSystem crs, FilterFactory2 ff) throws FactoryException {
		GeometryBuilder builder = new GeometryBuilder(crs);
		org.opengis.geometry.primitive.Point point = builder.createPoint(coordinate.x, coordinate.y, coordinate.z);
		Unit<?> unit_str = CRSUtilities.getUnit(crs.getCoordinateSystem());
		double unit = 0.00001;
		if(unit_str.getStandardUnit().isCompatible(SI.METER)) {
			unit = 1.11;
		}
		return ff.dwithin(propertyName, point, 1 * unit, unit_str.toString());
    }

	public FeatureInfo getFeatureInfo() {
		return this.featureInfo;
	}
	 
}
