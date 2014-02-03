/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.kvp;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.w3ds.types.W3DSLayer;
import org.geoserver.w3ds.types.W3DSLayerInfo;
import org.geoserver.w3ds.types.W3DSRequest;
import org.geoserver.w3ds.utilities.Format;
import org.geoserver.w3ds.utilities.W3DSUtils;
import org.geoserver.w3ds.utilities.X3DInfoExtract;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.MetaData;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;

public class KVPUtils {

	public static Envelope parseBbox(String bboxstr,
			CoordinateReferenceSystem crs, Logger LOGGER) throws Exception {
		List unparsed = KvpUtils.readFlat(bboxstr, ",");
		if (unparsed.size() < 4) {
			throw new IllegalArgumentException(
					"Error parsing the parameter BOUNDINGBOX (invalid format): "
							+ bboxstr);
		}
		double[] bbox = new double[4];
		for (int i = 0; i < 4; i++) {
			try {
				bbox[i] = Double.parseDouble((String) unparsed.get(i));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(
						"Error parsing the parameter BOUNDINGBOX (coordinate "
								+ unparsed.get(i) + " is not parsable: "
								+ bboxstr);
			}
		}
		double minx = bbox[0];
		double miny = bbox[1];
		double maxx = bbox[2];
		double maxy = bbox[3];
		if (minx > maxx) {
			throw new IllegalArgumentException(
					"Invalid parameter BOUNDINGBOX (MIN_X [" + minx + "] is "
							+ "greater than MAX_X [" + maxx + "]): " + bboxstr);
		}
		if (miny > maxy) {
			throw new IllegalArgumentException(
					"Invalid parameter BOUNDINGBOX (MIN_Y [" + miny + "] is "
							+ "greater than MAX_Y [" + maxy + "]): " + bboxstr);
		}
		ReferencedEnvelope bbox_e = new ReferencedEnvelope(minx, maxx, miny,
				maxy, crs);
		Envelope crsEnvelope = CRS.getEnvelope(crs);
		if (crsEnvelope != null) {
			ReferencedEnvelope crs_e = new ReferencedEnvelope(crsEnvelope);
			if (!crs_e.covers(bbox_e)) {
				// The specification says: If the Bounding Box values are
				// not defined for the given CRS (e.g., latitudes greater than 90
				// degrees in CRS:84), the
				// server should return empty content for areas outside the valid
				// range of the CRS.
				LOGGER.warning("CRS [" + crs.getName().getCodeSpace() + ":"
						+ crs.getName().getCode()
						+ "] envelope don't covers the boundingbox [" + bboxstr
						+ "] envelope");
				// throw new
				// IllegalArgumentException("Invalid mandatory parameter BOUNDINGBOX (crs envelope don't covers the boundingbox envelope): "
				// + bboxstr);
			}
		}
		// Isto só tá assim porque tinha um erro tem que se corrigir
		// return new ReferencedEnvelope(crs_e.intersection(bbox_e), crs);
		return bbox_e;
	}

	public static List<W3DSLayerInfo> parseLayers(String layersstr,
			Catalog catalog, Logger LOGGER) throws IOException,
			ParserConfigurationException, SAXException {
		List<W3DSLayerInfo> layers = new ArrayList<W3DSLayerInfo>();
		List unparsed = KvpUtils.readFlat(layersstr, ",");
		if (!(unparsed.size() > 0)) {
			throw new IllegalArgumentException(
					"Error parsing the parameter LAYERS (no layers given): "
							+ layersstr);
		}
		Iterator it = unparsed.iterator();
		X3DInfoExtract x3dInfoExtract = new X3DInfoExtract(catalog, false);
		while (it.hasNext()) {
			// The method getLayerByName handle this patter "prefix:resource".
			String n = (String) it.next();
			LayerInfo li = catalog.getLayerByName(n);
			if (li == null) {
				// If the layer cannot be found we just put a warning, is this
				// the right thing to do ?
				LOGGER.warning("The layer [" + n + "] cannot be found");
			} else {
				x3dInfoExtract.setLayerInfo(li);
				if (x3dInfoExtract.isAX3DLayer()) {
					if (x3dInfoExtract.isQueryable()) {
						if (!x3dInfoExtract.isTiled()) {
							layers.add(new W3DSLayerInfo(li, n));
						} else {
							LOGGER.warning("The layer [" + n + "] is tiled.");
						}
					} else {
						LOGGER.warning("The layer [" + n
								+ "] is not queriable.");
					}
				} else {
					LOGGER.warning("The layer [" + n
							+ "] is not a X3D enable layer.");
				}
			}
		}
		if (!(layers.size() > 0)) {
			throw new IllegalArgumentException(
					"Error parsing the parameter LAYERS (cannot found any valid layer): "
							+ layersstr);
		}
		return layers;
	}

	public static W3DSLayerInfo parseTiledLayer(String layerstr,
			Catalog catalog, Logger LOGGER) throws IOException, ParserConfigurationException, SAXException {
		LayerInfo li = catalog.getLayerByName(layerstr);
		if (li == null) {
			// If the layer cannot be found we just put a warning, is this
			// the right thing to do ?
			LOGGER.warning("The layer [" + layerstr + "] cannot be found");
		}
		// Test if the layer is tiled or not
		X3DInfoExtract x3dInfoExtract = new X3DInfoExtract(catalog, false);
		x3dInfoExtract.setLayerInfo(li);
		if (!x3dInfoExtract.isTiled()) {
			throw new IllegalArgumentException("The layer '" + layerstr
					+ "' is not tiled, so the GetTile operation is unvailable");
		}
		return new W3DSLayerInfo(li, layerstr);
	}

	public static Coordinate parseOffset(String offsetstr, Envelope envelope) {
		double[] offset = new double[3];
		ReferencedEnvelope bbox = new ReferencedEnvelope(envelope);
		List unparsed = KvpUtils.readFlat(offsetstr, ",");
		if (unparsed.size() == 3) {
			offset[0] = Double.valueOf((String) unparsed.get(0));
			offset[1] = Double.valueOf((String) unparsed.get(1));
			offset[2] = Double.valueOf((String) unparsed.get(2));
		} else {
			Coordinate centre = bbox.centre();
			offset[0] = centre.x;
			offset[1] = centre.y;
			offset[2] = centre.z;
			if (offset[2] != Double.NaN) {
				offset[2] = 0;
			}
		}
		return new Coordinate(offset[0], offset[1], offset[2]);
	}

	public static Coordinate parseCoordinate(String coordinatestr,
			CoordinateReferenceSystem crs) {
		double[] coordinate = new double[3];
		List unparsed = KvpUtils.readFlat(coordinatestr, ",");
		if (unparsed.size() == 3) {
			coordinate[0] = Double.valueOf((String) unparsed.get(0));
			coordinate[1] = Double.valueOf((String) unparsed.get(1));
			coordinate[2] = Double.valueOf((String) unparsed.get(2));
		} else {
			throw new IllegalArgumentException(
					"Error parsing the parameter COORDINATE (must have three axis): "
							+ coordinatestr);
		}
		Coordinate cord = new Coordinate(coordinate[0], coordinate[1],
				coordinate[2]);
		ReferencedEnvelope crs_e = new ReferencedEnvelope(CRS.getEnvelope(crs));
		if (!crs_e.contains(cord)) {
			throw new IllegalArgumentException("Error coordinate ("
					+ coordinatestr + ") not contained by the crs envelope");
		}
		return cord;
	}

	public static CoordinateReferenceSystem parseCRS(String crsstr) {
		CoordinateReferenceSystem crs = null;
		try {
			crs = CRS.decode(crsstr);
		} catch (NoSuchAuthorityCodeException e) {
			throw new IllegalArgumentException(
					"Invalid mandatory parameter CRS (unrecognized authority code): "
							+ crsstr);
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Error parsing the mandatory parameter CRS: " + crsstr);
		}
		return crs;
	}

	public static Format parseFormat(String formatstr, W3DSRequest request) {
		Format format = request.getValidFormat(formatstr);
		if (format == null) {
			throw new IllegalArgumentException(
					"Invalid parameter FORMAT (invalid format mime-type): "
							+ formatstr);
		}
		return format;
	}

	public static void parseStyles(List<W3DSLayerInfo> layers,
			String stylesstr, Catalog catalog) throws IOException,
			ParserConfigurationException, SAXException {
		X3DInfoExtract x3dInfoExtract = new X3DInfoExtract(catalog, false);
		String[] stylesNames = W3DSUtils.parseStrArray(stylesstr, "\\s*,\\s*");
		if (stylesNames.length != layers.size()) {
			throw new IllegalArgumentException(
					"Styles list length don't correspond to layers list lenght.");
		}
		int i = 0;
		for (W3DSLayerInfo wl : layers) {
			x3dInfoExtract.setLayerInfo(wl.getLayerInfo());
			String styleName = stylesNames[i];
			if (!styleName.isEmpty()) {
				if (x3dInfoExtract.containsStyle(styleName)) {
					StyleInfo styleInfo = catalog.getStyleByName(styleName);
					if (styleInfo != null) {
						wl.setRequestStyle(styleInfo);
						i++;
						continue;
					}
				}
			}
			StyleInfo styleInfo = x3dInfoExtract.getDefaultStyle();
			if (styleInfo != null) {
				wl.setRequestStyle(styleInfo);
			}
			i++;
		}
	}

	public static void parseStyle(W3DSLayerInfo layer, String styleName,
			Catalog catalog) throws IOException, ParserConfigurationException,
			SAXException {
		X3DInfoExtract x3dInfoExtract = new X3DInfoExtract(catalog, false);
		x3dInfoExtract.setLayerInfo(layer.getLayerInfo());
		if (!styleName.isEmpty()) {
			if (x3dInfoExtract.containsStyle(styleName)) {
				StyleInfo styleInfo = catalog.getStyleByName(styleName);
				if (styleInfo != null) {
					layer.setRequestStyle(styleInfo);
					return;
				}
			}
		}
		StyleInfo styleInfo = x3dInfoExtract.getDefaultStyle();
		if (styleInfo != null) {
			layer.setRequestStyle(styleInfo);
		}
	}

	public static void setDefaultStyle(W3DSLayerInfo layer, Catalog catalog)
			throws IOException, ParserConfigurationException, SAXException {
		X3DInfoExtract x3dInfoExtract = new X3DInfoExtract(catalog, false);
		x3dInfoExtract.setLayerInfo(layer.getLayerInfo());
		StyleInfo styleInfo = x3dInfoExtract.getDefaultStyle();
		if (styleInfo != null) {
			layer.setRequestStyle(styleInfo);
		}
	}

	public static void setDefaultStyles(List<W3DSLayerInfo> layers,
			Catalog catalog) throws IOException, ParserConfigurationException,
			SAXException {
		for (W3DSLayerInfo wl : layers) {
			setDefaultStyle(wl, catalog);
		}
	}
}
