/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.x3d;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.geoserver.platform.ServiceException;
import org.geoserver.w3ds.types.W3DSLayer;
import org.geoserver.w3ds.types.W3DSLayerInfo;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.resources.CRSUtilities;
import org.geotools.styling.Style;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class X3DBuilder {

	private BufferedWriter writer;

	private X3DNode scene;
	private X3DStyles styles;
	private X3DAttribute geoSystem;

	private X3DNode activeLayer;
	private X3DNode activeObject;
	private X3DPoints activePoints;
	private X3DLines activeLines;
	private X3DPolygons activePolygons;

	private BoundingBox activeLayerBounds;
	private int coordinatesType;

	private StringBuilder title;

	private List<X3DInlineModel> inlineModels;
	private X3DNode activeInlineModels;

	public X3DBuilder(OutputStream output) {
		writer = new BufferedWriter(new OutputStreamWriter(output));
		scene = new X3DNode("Scene");

		/*
		 * X3DNode background = new X3DNode("Background");
		 * background.addX3DAttribute("skyColor", "1 1 1");
		 * scene.addX3DNode(background); X3DNode light = new
		 * X3DNode("PointLight"); light.addX3DAttribute("global", "true");
		 * light.addX3DAttribute("ambientIntensity", "0.3");
		 * light.addX3DAttribute("attenuation", "0 0 0");
		 * light.addX3DAttribute("color", "1 1 1");
		 * light.addX3DAttribute("intensity", "0.9");
		 * light.addX3DAttribute("location", "0 0 0");
		 * light.addX3DAttribute("on", "true"); light.addX3DAttribute("radius",
		 * "100"); scene.addX3DNode(light);
		 */

		coordinatesType = X3DDefinitions.GEOGRAPHIC_METRIC.getCode();
		this.styles = new X3DStyles();
		this.title = new StringBuilder();
		inlineModels = new ArrayList<X3DInlineModel>();
	}

	public void createGeoOrigin(CoordinateReferenceSystem crs, double[] origin) {
		String type = "";
		Unit<?> unit_str = CRSUtilities.getUnit(crs.getCoordinateSystem());
		if (unit_str.getStandardUnit().isCompatible(SI.METER)) {
			coordinatesType = X3DDefinitions.GEOGRAPHIC_METRIC.getCode();
			return;
		} else {
			type = "\"GD\" \"WE\" \"longitude_first\"";
			coordinatesType = X3DDefinitions.GEOGRAPHIC_DEGREES.getCode();
		}

		geoSystem = new X3DAttribute("geoSystem", type);

		X3DNode geoOrigin = new X3DNode("geoOrigin");
		geoOrigin.addX3DAttribute(geoSystem);
		geoOrigin.addX3DAttribute("geoCoords", String.valueOf(origin[0]) + " "
				+ String.valueOf(origin[1]) + " " + String.valueOf(origin[2]));
		geoOrigin.addX3DAttribute("DEF",
				X3DDefinitions.GEO_ORIGIN.getDefinition());
		X3DNode geoOriginGroup = new X3DNode("Group");
		geoOriginGroup.addX3DNode(geoOrigin);
		scene.addX3DNode(geoOriginGroup);
	}

	public void createGeoOrigin(CoordinateReferenceSystem crs,
			BoundingBox bounds) {
		double[] offset = new double[3];
		ReferencedEnvelope bbox;
		if (bounds != null) {
			bbox = new ReferencedEnvelope(bounds);
		} else {
			bbox = new ReferencedEnvelope(CRS.getEnvelope(crs));
		}
		Coordinate centre = bbox.centre();
		if (centre.x == 0 && centre.y == 0) {
			offset[0] = bbox.getMinX();
			offset[1] = bbox.getMaxX();
		} else {
			offset[0] = centre.x;
			offset[1] = centre.y;
		}
		offset[2] = 0;
		createGeoOrigin(crs, offset);
	}

	public void cleanGeometries() {
		activePoints = new X3DPoints(X3DGeometryType.POINTS, geoSystem,
				coordinatesType);
		activeLines = new X3DLines(X3DGeometryType.LINES, geoSystem,
				coordinatesType);
		activePolygons = new X3DPolygons(X3DGeometryType.POLYGONS, geoSystem,
				coordinatesType);
		activeInlineModels = new X3DNode("Group");
	}

	public void newObject() {
		activeObject = new X3DNode("Group");
		activeLayer.addX3DNode(activeObject);
		cleanGeometries();
	}

	public boolean setObjectAttribute(String name, String value) {
		if (!value.isEmpty()) {
			activeObject.addX3DAttribute(new X3DAttribute(name, value));
			return true;
		}
		return false;
	}

	public boolean setObjectID(String id) {
		return setObjectAttribute("id", id);
	}

	public boolean setObjectClass(String clazz) {
		return setObjectAttribute("class", clazz);
	}

	public void newObject(String id, String clazz) {
		newObject();
		setObjectID(id);
		setObjectClass(clazz);
	}

	public void newLayer() {
		activeLayer = new X3DNode("Group");
		scene.addX3DNode(activeLayer);
	}

	public boolean setLayerAttribute(String name, String value) {
		if (!value.isEmpty()) {
			activeLayer.addX3DAttribute(new X3DAttribute(name, value));
			return true;
		}
		return false;
	}

	public void newLayer(W3DSLayerInfo layerInfo, List<Style> styles) {
		newLayer();
		this.styles = new X3DStyles();
		this.styles.addStyles(styles);
		setLayerAttribute("id", layerInfo.getRequestName());
		for (Style style : styles) {
			inlineModels.addAll(X3DUtils.getInlineModels(style));
		}
	}

	private static String getObjectID(Boolean hashID, String cn, Feature feature) {
		if (hashID.booleanValue() == Boolean.FALSE.booleanValue()) {
			return "uknow";
		}
		Object o = feature.getProperty(cn).getValue();
		if (o != null) {
			return feature.getProperty(cn).getValue().toString();
		}
		return "uknow";
	}

	private static String getObjectClass(Boolean hashClass, List<String> ncs,
			Feature feature) {
		if (hashClass.booleanValue() == Boolean.FALSE.booleanValue()) {
			return "";
		}
		StringBuilder strb = new StringBuilder();
		for (String cn : ncs) {
			Object o = feature.getProperty(cn).getValue();
			if (o != null) {
				strb.append(feature.getProperty(cn).getValue().toString() + " ");
			}
		}
		return strb.toString();
	}

	public void addW3DSLayer(W3DSLayer layer) {
		newLayer(layer.getLayerInfo(), layer.getStyles());
		FeatureCollection<?, ?> collection = layer.getFeatures();
		title.append(layer.getLayerInfo().getRequestName());
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
						if (value instanceof Geometry) {
							addGeometry(
									(Geometry) value,
									layer.getLayerInfo().getRequestName()
											+ ":"
											+ getObjectID(
													layer.getHasObjectID(),
													layer.getObjectID(),
													feature),
									getObjectClass(layer.getHasObjectClass(),
											layer.getObjectClass(), feature),
									feature);
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

	private void setPolygonStyle(Feature feature) {
		X3DNode appearance = this.styles.getAppearance(feature);
		this.activePolygons.setAppearance(appearance);
	}

	private void setLineStyle(Feature feature) {
		X3DNode appearance = this.styles.getAppearance(feature);
		this.activeLines.setAppearance(appearance);
	}

	public void addGeometry(Geometry geometry, String id, String clazz,
			Feature feature) throws IOException {
		newObject(id, clazz);
		setPolygonStyle(feature);
		setLineStyle(feature);
		addGeometry(geometry, feature);
		if (activePolygons.haveGeometries()) {
			X3DNode polygons = activePolygons.getX3D();
			// polygons.addX3DNode(getStyle(feature));
			activeObject.addX3DNode(polygons);
		}
		if (activeLines.haveGeometries()) {
			X3DNode lines = activeLines.getX3D();
			activeObject.addX3DNode(lines);
		}
		if (activeInlineModels.haveChilds()) {
			activeObject.addX3DNode(activeInlineModels);
		}
	}

	private static int getGeometryType(Geometry geometry) {
		if (geometry instanceof Point) {
			return GeometryType.POINT.getCode();
		} else if (geometry instanceof LineString) {
			return GeometryType.LINESTRING.getCode();
		} else if (geometry instanceof Polygon) {
			return GeometryType.POLYGON.getCode();
		} else if (geometry instanceof MultiPoint) {
			return GeometryType.MULTIPOINT.getCode();
		} else if (geometry instanceof MultiLineString) {
			return GeometryType.MULTILINESTRING.getCode();
		} else if (geometry instanceof MultiPolygon) {
			return GeometryType.MULTIPOLYGON.getCode();
		} else if (geometry instanceof GeometryCollection) {
			return GeometryType.MULTIGEOMETRY.getCode();
		} else {
			throw new IllegalArgumentException(
					"Unable to determine geometry type " + geometry.getClass());
		}
	}

	// DEBUG // TO LIMIT THE WRITED GEOMETRIES
	int geometries = 0;

	public void addGeometry(Geometry geometry, Feature feature)
			throws IOException {
		final int geometryType = getGeometryType(geometry);
		if (geometries > 6620)
			return;
		if (geometryType != GeometryType.MULTIGEOMETRY.getCode()) {
			if (geometryType == GeometryType.POLYGON.getCode()) {
				this.activePolygons.addPolygon(((Polygon) geometry));
				// geometries++;
			} else if (geometryType == GeometryType.MULTIPOLYGON.getCode()) {
				for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
					addGeometry(geometry.getGeometryN(i), feature);
				}
			} else if (geometryType == GeometryType.LINESTRING.getCode()) {
				for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
					this.activeLines.addLineString((LineString) geometry);
				}
			} else if (geometryType == GeometryType.POINT.getCode()) {
				for (X3DInlineModel inlineModel : inlineModels) {
					if (inlineModel.acceptFeature(feature)) {
						activeInlineModels.addX3DNode(inlineModel
								.getInlineModel(feature, (Point) geometry));
						break;
					}
				}
			}
		} else {
			int n_geometries = geometry.getNumGeometries();
			for (int i = 0; i < n_geometries; i++) {
				addGeometry(geometry.getGeometryN(i), feature);
			}
		}
	}

	public void writeX3D() throws IOException {
		X3DNode x3d = new X3DNode("X3D");
		x3d.addX3DNode(scene);
		writer.write(x3d.toStringSpaces(""));
	}

	public void writeHTML() throws IOException {
		X3DNode html = new X3DNode("html");
		X3DNode doc = new X3DNode();
		// doc.setText("<!DOCTYPE html>");
		html.addX3DNode(doc);
		X3DNode head = new X3DNode("head");
		X3DNode meta = new X3DNode("meta");
		meta.addX3DAttribute(new X3DAttribute("http-equiv", "Content-Type"));
		meta.addX3DAttribute(new X3DAttribute("content",
				"text/html; charset=utf-8"));
		X3DNode title = new X3DNode("title");
		title.setText(this.title.toString());
		X3DNode link = new X3DNode("link");
		link.addX3DAttribute(new X3DAttribute("rel", "stylesheet"));
		link.addX3DAttribute(new X3DAttribute("href",
				"http://3dwebgis.di.uminho.pt/geoserver3D/x3dom/x3dom.css"));
		// http://3dwebgis.di.uminho.pt/geoserver3D/canvasSize.css
		// link.addX3DAttribute(new X3DAttribute("href",
		// "http://localhost/x3dom.css"));
		X3DNode script = new X3DNode("script");
		script.setExpand(true);
		script.addX3DAttribute(new X3DAttribute("type", "text/javascript"));
		script.addX3DAttribute(new X3DAttribute("src",
				"http://3dwebgis.di.uminho.pt/geoserver3D/x3dom/x3dom.js"));
		// script.addX3DAttribute(new X3DAttribute("src",
		// "http://localhost/x3dom.js"));
		head.addX3DNode(meta);
		head.addX3DNode(title);
		head.addX3DNode(link);
		head.addX3DNode(script);
		html.addX3DNode(head);
		X3DNode body = new X3DNode("body");
		body.addX3DAttribute(new X3DAttribute("onload", "init()"));
		X3DNode p = new X3DNode("p");
		X3DNode l1 = new X3DNode("a");
		l1.addX3DAttribute(new X3DAttribute("href", "#"));
		l1.addX3DAttribute(new X3DAttribute("onClick",
				"$element.runtime.showAll();return false;"));
		l1.setText("Show All");
		X3DNode l2 = new X3DNode("a");
		l2.addX3DAttribute(new X3DAttribute("href", "#"));
		l2.addX3DAttribute(new X3DAttribute("onClick",
				"$element.runtime.examine();return false;"));
		l2.setText("Examine");
		X3DNode l3 = new X3DNode("a");
		l3.addX3DAttribute(new X3DAttribute("href", "#"));
		l3.addX3DAttribute(new X3DAttribute("onClick",
				"$element.runtime.lookAt();return false;"));
		l3.setText("LookAt");
		p.addX3DNode(l1);
		p.addX3DNode(l2);
		p.addX3DNode(l3);
		body.addX3DNode(p);
		X3DNode x3d = new X3DNode("x3d");
		x3d.addX3DAttribute(new X3DAttribute("id", "element"));
		// x3d.addX3DAttribute(new X3DAttribute("width", "1200px"));
		// x3d.addX3DAttribute(new X3DAttribute("height", "900px"));
		// x3d.addX3DAttribute("showLog", "true");
		x3d.addX3DNode(scene);
		body.addX3DNode(x3d);
		X3DNode init = new X3DNode();
		init.setText("<script type=\"text/javascript\">function init() {$element = document.getElementById('element');}</script>");
		body.addX3DNode(init);
		html.addX3DNode(body);
		writer.write(html.toStringSpaces(""));
	}

	public void close() throws IOException {
		writer.flush();
		writer.close();
	}

}
