/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.sextante;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.data.DataStore;
import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import es.unex.sextante.dataObjects.AbstractVectorLayer;
import es.unex.sextante.dataObjects.IFeatureIterator;
import es.unex.sextante.dataObjects.IVectorLayer;

public class GTVectorLayer extends AbstractVectorLayer {

	// Factory methods:
	/**
	 * Creates a vector layer from the FeatureCollection resulting from applying
	 * the query to the Datastore.
	 * <p>
	 * Note throws an exception if the query's typename is not one of the
	 * datastores featuretypes
	 * </p>
	 *
	 * @param source
	 *            the DataStore to query
	 * @param query
	 *            the query to use for obtaining the data
	 * @throws IOException
	 */
	public static GTVectorLayer createLayer(DataStore source, Query query)
			throws IOException {
		if (!Arrays.asList(source.getTypeNames()).contains(query.getTypeName())) {
			throw new IllegalArgumentException(
					query.getTypeName()
							+ " is not one of the FeatureTypes contained by the Datasource.  Options are: "
							+ Arrays.asList(source.getTypeNames()));
		}
		FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = source.getFeatureSource(query
				.getTypeName());
		GTVectorLayer layer = new GTVectorLayer();
		layer.create(featureSource, query);
		return layer;
	}
	
	/**
	 * Creates a vector layer from the FeatureCollection resulting from applying
	 * the query to the Datastore.
	 * <p>
	 * Note throws an exception if the query's typename is not one of the
	 * datastores featuretypes
	 * </p>
	 *
	 * @param source
	 *            the DataStore to query
	 * @param query
	 *            the query to use for obtaining the data
	 * @throws IOException
	 */
	public static GTVectorLayer createLayer(FeatureSource featureSource, Query query) {
		GTVectorLayer layer = new GTVectorLayer();
		layer.create(featureSource, query);
		return layer;
	}


	/**
	 * Creates a vector layer from the datasource.
	 *
	 * @param source
	 *            the source to query
	 * @param typename
	 *            the typename (must be one of the datasources typenames) to use
	 *            for the layer
	 * @param filter
	 *            a filter to apply to the features that are used in the layer
	 * @param crs
	 *            the crs to reproject the features to (if necessary). If null
	 *            no reprojection will be applied
	 */
	public static GTVectorLayer createLayer(DataStore source, String typename,
			Filter filter, CoordinateReferenceSystem crs) throws IOException {
		DefaultQuery query = new DefaultQuery(typename, filter);
		if (crs != null) {
			query.setCoordinateSystemReproject(crs);
		}
		return createLayer(source, query);
	}

	/**
	 * Creates a vector layer from the datasource.
	 *
	 * @param source
	 *            the source to query
	 * @param typename
	 *            the typename (must be one of the datasources typenames) to use
	 *            for the layer
	 * @param crs
	 *            the crs to reproject the features to (if necessary). If null
	 *            no reprojection will be applied
	 */
	public static GTVectorLayer createLayer(DataStore source, String typename,
			CoordinateReferenceSystem crs) throws IOException {
		return createLayer(source, typename, Filter.INCLUDE, crs);
	}

	/**
	 * Creates a vector layer from the datasource.
	 *
	 * @param source
	 *            the source to query
	 * @param typename
	 *            the typename (must be one of the datasources typenames) to use
	 *            for the layer
	 */
	public static GTVectorLayer createLayer(DataStore source, String typename)
			throws IOException {
		return createLayer(source, typename, Filter.INCLUDE, null);
	}

	private String m_sFilename;
	private String m_sName;
	private int m_iCount;
	private CoordinateReferenceSystem m_CRS;
	private Query m_Query;

	public void create(FeatureSource<SimpleFeatureType, SimpleFeature> featureSource, Query query) {

		try {
			this.m_Query = query;
			m_BaseDataObject = featureSource;
			m_iCount = featureSource.getFeatures(query).size();
			m_CRS = featureSource.getSchema().getCoordinateReferenceSystem();
			m_sName = query.getTypeName();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void open() {
	}

	public void close() {
		if (this.getFeatureSource() != null) {
			this.getFeatureSource().getDataStore().dispose();
		}
	}

	public void addFeature(Geometry g, Object[] values) {

		if (getFeatureSource() instanceof FeatureStore) {
			Geometry geom;
			GeometryFactory gf = new GeometryFactory();
			if (g instanceof Polygon) {
				geom = gf.createMultiPolygon(new Polygon[] { (Polygon) g });
			} else if (g instanceof LineString) {
				geom = gf.createMultiLineString(new LineString[] { (LineString) g });
			} else {
				geom = g;
			}

			try {
				FeatureStore<SimpleFeatureType, SimpleFeature> store = ((FeatureStore<SimpleFeatureType, SimpleFeature>) getFeatureSource());
				List<Object> attributes = new ArrayList<Object>();
				attributes.add(geom);
				attributes.addAll(Arrays.asList(values));
				SimpleFeatureType ft = store.getSchema();
				FeatureCollection<SimpleFeatureType, SimpleFeature> collection = FeatureCollections
						.newCollection();

				SimpleFeature feature = SimpleFeatureBuilder.build(ft, attributes, SimpleFeatureBuilder.createDefaultFeatureId());
				collection.add(feature);
				store.addFeatures(collection);
				m_iCount++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public IFeatureIterator iterator() {

		if (m_BaseDataObject != null) {
			try {
				FeatureCollection<SimpleFeatureType, SimpleFeature> features = getFeatureSource().getFeatures(
						m_Query);
				return new GTFeatureIterator(features);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}

	}

	public String getFieldName(int i) {

		if (m_BaseDataObject != null) {
			try {

				SimpleFeatureType ft = getFeatureSource().getSchema();
				AttributeType at = ft.getType(i + 1);
				return at.getName().getLocalPart();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		return null;

	}

	public Class<?> getFieldType(int i) {

		if (m_BaseDataObject != null) {
			try {
				SimpleFeatureType ft = getFeatureSource().getSchema();
				AttributeType at = ft.getType(i + 1);
				return at.getBinding();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		return null;

	}

	public int getFieldCount() {

		if (m_BaseDataObject != null) {
			try {
				SimpleFeatureType ft = getFeatureSource().getSchema();
				return ft.getAttributeCount() - 1;
			} catch (Exception e) {
				e.printStackTrace();
				return 0;
			}
		}

		return 0;

	}

	public int getShapesCount() {

		return m_iCount;

	}

	public int getShapeType() {

		if (m_BaseDataObject != null) {
			try {

				SimpleFeatureType ft = getFeatureSource().getSchema();
				Class<?> type = ft.getGeometryDescriptor().getType().getBinding();
				if (type.isAssignableFrom(Polygon.class)
						|| type.isAssignableFrom(MultiPolygon.class)) {
					return IVectorLayer.SHAPE_TYPE_POLYGON;
				} else if (type.isAssignableFrom(LineString.class)
						|| type.isAssignableFrom(MultiLineString.class)) {
					return IVectorLayer.SHAPE_TYPE_LINE;
				} else {
					return IVectorLayer.SHAPE_TYPE_POINT;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return IVectorLayer.SHAPE_TYPE_POLYGON;
			}
		}

		return IVectorLayer.SHAPE_TYPE_POLYGON;

	}

	public String getName() {

		return m_sName;

	}

	public Rectangle2D getFullExtent() {

		if (m_BaseDataObject != null) {
			try {
				ReferencedEnvelope bounds = getFeatureSource().getFeatures(
						m_Query).getBounds();
				return new Rectangle2D.Double(bounds.getMinX(), bounds
						.getMinY(), bounds.getWidth(), bounds.getHeight());
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}

	}

	public String getFilename() {

		return m_sFilename;

	}

	public Object getCRS() {

		return m_CRS;

	}

	public void setName(String name) {

		m_sName = name;

	}

	@SuppressWarnings("unchecked")
	private FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource() {
		return (FeatureSource<SimpleFeatureType, SimpleFeature>) m_BaseDataObject;
	}

	public void postProcess() throws Exception {
		// TODO Auto-generated method stub
		
	}


}
