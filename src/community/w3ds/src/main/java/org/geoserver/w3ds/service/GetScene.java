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

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.w3ds.types.GetSceneRequest;
import org.geoserver.w3ds.types.Scene;
import org.geoserver.w3ds.types.W3DSLayerInfo;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.filter.IllegalFilterException;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.geometry.jts.LiteCoordinateSequenceFactory;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GetScene {

	private GeoServer geoServer;
	private Catalog catalog;
	private GetSceneRequest request;
	private Scene scene;

	public GetScene(GeoServer geoServer, Catalog catalog,
			GetSceneRequest request, Scene scene) {
		this.geoServer = geoServer;
		this.catalog = catalog;
		this.request = request;
		this.scene = scene;
	}

	public GetScene(GeoServer geoServer, Catalog catalog,
			GetSceneRequest request) {
		this.geoServer = geoServer;
		this.catalog = catalog;
		this.request = request;
		this.scene = new Scene(request.getCrs());
	}

	public void run() throws IOException {
		Iterator<W3DSLayerInfo> layersIterator = request.getLayers().iterator();
		while(layersIterator.hasNext()) {
			W3DSLayerInfo layerInfo = layersIterator.next();
			FeatureTypeInfo feature = catalog.getFeatureTypeByName(layerInfo.getLayerInfo().getResource().getName());
			Query query = createQuery((SimpleFeatureSource)feature.getFeatureSource(null, null));
			scene.add(layerInfo, feature.getFeatureSource(null, null).getFeatures(query), catalog);
		}
	}

	public Query createQuery(SimpleFeatureSource source) {
		FilterFactory filterFactory = (FilterFactory) CommonFactoryFinder
				.getFilterFactory(null);
		Filter filter = Filter.INCLUDE;
		CoordinateReferenceSystem crs = source.getSchema()
				.getCoordinateReferenceSystem();
		CoordinateReferenceSystem declaredCRS = this.request.getCrs();
		Filter transformedFilter = filter;
		if (declaredCRS != null) {
			ReprojectingFilterVisitor visitor = new ReprojectingFilterVisitor(
					CommonFactoryFinder.getFilterFactory2(GeoTools
							.getDefaultHints()), source.getSchema());
			transformedFilter = (Filter) filter.accept(visitor, null);
		}
		Filter bboxFilter = createBBoxFilter(source.getSchema(),
				request.getBbox(), filterFactory);
		filter = filterFactory.and(transformedFilter, bboxFilter);
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

	private static Filter createBBoxFilter(SimpleFeatureType schema,
			Envelope bbox, FilterFactory filterFactory)
			throws IllegalFilterException {
		List<Filter> filters = new ArrayList<Filter>();
		ReferenceIdentifier id = (ReferenceIdentifier)bbox.getCoordinateReferenceSystem().getIdentifiers().toArray()[0];
		for (int j = 0; j < schema.getAttributeCount(); j++) {
			AttributeDescriptor attType = schema.getDescriptor(j);
			if (attType instanceof GeometryDescriptor) {
				Filter gfilter = filterFactory.bbox(attType.getLocalName(),
						bbox.getMinimum(0), bbox.getMinimum(1),
						bbox.getMaximum(0), bbox.getMaximum(1), id.toString());
				filters.add(gfilter);
			}
		}

		if (filters.size() == 0)
			return Filter.INCLUDE;
		else if (filters.size() == 1)
			return (Filter) filters.get(0);
		else
			return filterFactory.or(filters);
	}
	
	public Scene getScene() {
		return scene;
	}
	 
}
