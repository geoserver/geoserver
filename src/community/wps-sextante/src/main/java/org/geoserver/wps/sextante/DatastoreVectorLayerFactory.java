/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.sextante;

import java.io.IOException;

import org.geotools.data.DataStore;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import es.unex.sextante.dataObjects.IVectorLayer;

public abstract class DatastoreVectorLayerFactory implements GTVectorLayerFactory {
	public DatastoreVectorLayerFactory() {
		super();
	}

	public IVectorLayer create(String sName, int iShapeType, Class<?>[] fields,
			String[] sFields, String filename, Object crs) {
		try {

			if (!(crs instanceof CoordinateReferenceSystem)) {
				crs = DefaultGeographicCRS.WGS84;
			}
			SimpleFeatureType featureType = buildFeatureType(sName, iShapeType, fields, sFields, (CoordinateReferenceSystem) crs);
			DataStore mds = createDatastore(filename, featureType);
			mds.createSchema(featureType);
			return createLayer(mds, sName, crs);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected IVectorLayer createLayer(DataStore dataStore, String sName, Object crs) throws IOException {
		GTVectorLayer layer = GTVectorLayer.createLayer(dataStore, sName,
				(CoordinateReferenceSystem) crs);
		layer.setName(sName);
		return layer;
	}

	protected abstract DataStore createDatastore(String m_sFilename, SimpleFeatureType m_FeatureType) throws IOException;

	private static GeometryDescriptor toGeometryAttribute(int shapeType,
			CoordinateReferenceSystem crs, AttributeTypeBuilder builder) {

		Class<?> s[] = { Point.class, MultiLineString.class, MultiPolygon.class };
		GeometryType buildGeometryType = builder.crs(crs).binding(s[shapeType]).buildGeometryType();
		return builder.buildDescriptor("geom", buildGeometryType);
	}

	public static SimpleFeatureType buildFeatureType(String sName, int iShapeType, Class<?>[] fields,
			String[] sFields, CoordinateReferenceSystem crs) {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName(sName);

		AttributeTypeBuilder attBuilder = new AttributeTypeBuilder();
		builder.add(toGeometryAttribute(iShapeType, crs,attBuilder));
		builder.setDefaultGeometry("geom");
		for (int i = 0; i < sFields.length; i++) {
			AttributeType type = attBuilder.binding(fields[i]).buildType();
			AttributeDescriptor descriptor = attBuilder.buildDescriptor(sFields[i], type);
			builder.add(descriptor);
		}
		return builder.buildFeatureType();
		
	}

}
