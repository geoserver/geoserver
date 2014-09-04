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
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
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

import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.AbstractVectorLayer;
import es.unex.sextante.dataObjects.IFeatureIterator;
import es.unex.sextante.dataObjects.IVectorLayer;

public class GTShpLayer extends AbstractVectorLayer {
	private String m_sFilename;
	private String m_sName;
	private int m_iCount;
	private CoordinateReferenceSystem m_CRS;
	private Query m_Query;
	private FeatureWriter<SimpleFeatureType,SimpleFeature> m_featWriter;
	private DataStore m_dataStore;
	
	public GTShpLayer() {
		System.out.println("Hello!");
	}

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
	public static GTShpLayer createLayer(DataStore source, Query query)
			throws IOException {
		if (!Arrays.asList(source.getTypeNames()).contains(query.getTypeName())) {
			throw new IllegalArgumentException(
					query.getTypeName()
							+ " is not one of the FeatureTypes contained by the Datasource.  Options are: "
							+ Arrays.asList(source.getTypeNames()));
		}
		FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = source.getFeatureSource(query
				.getTypeName());
		GTShpLayer layer = new GTShpLayer();
		layer.create(source, featureSource, query);
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
	public static GTShpLayer createLayer(DataStore source, String typename,
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
	public static GTShpLayer createLayer(DataStore source, String typename,
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
	public static GTShpLayer createLayer(DataStore source, String typename)
			throws IOException {
		return createLayer(source, typename, Filter.INCLUDE, null);
	}
	
	public void create(DataStore store, FeatureSource<SimpleFeatureType, SimpleFeature> featureSource, Query query) throws IOException {
		this.m_dataStore = store;
		
		try {
			this.m_Query = query;
			m_BaseDataObject = featureSource;
			m_iCount = featureSource.getFeatures(query).size();
			m_CRS = featureSource.getSchema().getCoordinateReferenceSystem();
			m_sName = query.getTypeName();
		} catch (Exception e) {
			throw (IOException) new IOException(e.getMessage()).initCause(e);
		}
	}
	
	public void open() {
		try {
			postProcess(); // close and dispose the feature writer, if existing
		} catch (IOException e) {
			e.printStackTrace();
			Sextante.addErrorToLog(e);
		}
	}

	public void close() {
		if (m_featWriter!=null) {
			try {
				m_featWriter.close();
				m_featWriter=null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Sextante.addErrorToLog(e);
			}
		}
		if (this.getFeatureSource() != null) {
			this.getFeatureSource().getDataStore().dispose();
		}
	}

	public void addFeature(Geometry g, Object[] values) {
		if (m_featWriter==null) {
			SimpleFeatureType ft = ((FeatureSource<SimpleFeatureType, SimpleFeature>)m_BaseDataObject).getSchema();
			try {
				m_featWriter = m_dataStore.getFeatureWriterAppend(ft.getTypeName(), DefaultTransaction.AUTO_COMMIT);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
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
				List<Object> attributes = new ArrayList<Object>();
				attributes.add(geom);
				attributes.addAll(Arrays.asList(values));
				SimpleFeature feature = m_featWriter.next();
				feature.setAttributes(attributes);
				m_featWriter.write();
				m_iCount++;
			} catch (Exception e) {
				// FIXME: improve exception handling
				throw new RuntimeException(e);
			}
		}
		else {
			throw new RuntimeException("Incorrect feature source");
		}


	}

	public IFeatureIterator iterator() {
		if (m_featWriter!=null) {
			throw new IllegalArgumentException("Method open() [and postproces() if an editing session is active] must be called before reading the table contents.");
		}
		if (m_BaseDataObject != null) {
			try {
				FeatureCollection<SimpleFeatureType, SimpleFeature> features = getFeatureSource().getFeatures(
						m_Query);
				return new GTFeatureIterator(features);
			} catch (Exception e) {
				throw new RuntimeException(e);
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
				Sextante.addErrorToLog(e);
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
				Sextante.addErrorToLog(e);
				return IVectorLayer.SHAPE_TYPE_POLYGON;
			}
		}

		return IVectorLayer.SHAPE_TYPE_POLYGON;

	}

	public String getName() {

		return m_sName;

	}

	public void postProcess() throws IOException {
		if (m_featWriter!=null) {
			m_featWriter.close();
			m_featWriter = null;
		}
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
				Sextante.addErrorToLog(e);
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

}
