/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Nuno Oliveira - PTInovacao
 */

package org.geoserver.w3ds.kml;

import java.io.OutputStream;
import java.util.List;

import org.geoserver.platform.ServiceException;
import org.geoserver.w3ds.types.W3DSLayer;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import com.vividsolutions.jts.geom.Point;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Model;
import de.micromata.opengis.kml.v_2_2_0.Placemark;

public class KmlBuilder {

	private Kml kml;
	private Document document;

	private List<KmlModel> kmlModels;

	public KmlBuilder() {
		kml = new Kml();
		document = kml.createAndSetDocument().withName("");
	}

	public void addW3DSLayer(W3DSLayer layer) {
		List<KmlModel> kmlModels = KmlUtils.getKmlModels(layer.getStyles());
		FeatureCollection<?, ?> collection = layer.getFeatures();
		document.setName((document.getName() + layer.getLayerInfo()
				.getRequestName()));
		try {
			FeatureIterator<?> iterator = collection.features();
			SimpleFeature feature;
			SimpleFeatureType fType;
			List<AttributeDescriptor> types;
			while (iterator.hasNext()) {
				feature = (SimpleFeature) iterator.next();
				fType = feature.getFeatureType();
				types = fType.getAttributeDescriptors();
				for (int j = 0; j < types.size(); j++) {
					Object value = feature.getAttribute(j);
					if (value != null) {
						if (value instanceof Point) {
							addPoint(feature, (Point) value, kmlModels);
						}
					}
				}
			}
			iterator.close();
		} catch (Exception exception) {
			ServiceException serviceException = new ServiceException("Error: "
					+ exception.getMessage());
			serviceException.initCause(exception);
			throw serviceException;
		}
	}

	public Kml getKml() {
		return kml;
	}

	private void addPoint(Feature feature, Point point, List<KmlModel> kmlModels) {
		Model kmlModel = getKmlModel(feature, point, kmlModels);
		if (kmlModel != null) {
			Placemark placemark = new Placemark().withName(feature.getName()
					.toString());
			placemark.createAndSetLookAt()
					.withLongitude(point.getCoordinate().x)
					.withLatitude(point.getCoordinate().y).withRange(440.8)
					.withTilt(8.3).withHeading(2.7);
			placemark.withGeometry(kmlModel);
			document.addToFeature(placemark);
		}
	}

	private Model getKmlModel(Feature feature, Point point,
			List<KmlModel> kmlModels) {
		for (KmlModel kmlModel : kmlModels) {
			if (kmlModel.acceptFeature(feature)) {
				return kmlModel.getKmlModel(feature, point);
			}
		}
		return null;
	}

}
