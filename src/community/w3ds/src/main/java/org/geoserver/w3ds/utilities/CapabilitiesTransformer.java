/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.utilities;

import static org.geoserver.ows.util.ResponseUtils.appendQueryString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.w3ds.service.W3DSInfo;
import org.geoserver.w3ds.types.LOD;
import org.geoserver.w3ds.types.LODSet;
import org.geoserver.w3ds.types.TileSet;
import org.geotools.referencing.CRS;
import org.geotools.styling.Description;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.util.InternationalString;
import org.vfny.geoserver.global.FeatureTypeInfoTitleComparator;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.vividsolutions.jts.geom.Envelope;

public class CapabilitiesTransformer extends TransformerBase {

	protected GeoServer geoServer;
	protected Catalog catalog;
	protected HttpServletRequest request;

	protected X3DInfoExtract x3dInfoExtract;

	public CapabilitiesTransformer(GeoServer geoServer, Catalog catalog)
			throws IOException, ParserConfigurationException, SAXException {
		super();
		this.geoServer = geoServer;
		this.catalog = catalog;
		this.x3dInfoExtract = new X3DInfoExtract(this.geoServer);
	}

	@Override
	public Translator createTranslator(ContentHandler handler) {
		return new CapabilitiesTranslator(handler);
	}

	class CapabilitiesTranslator extends TranslatorSupport {

		public CapabilitiesTranslator(ContentHandler handler) {
			super(handler, null, null);
		}

		@Override
		public void encode(Object o) throws IllegalArgumentException {
			request = (HttpServletRequest) o;

			AttributesImpl attributes = new AttributesImpl();
			attributes.addAttribute("", "version", "version", "", "0.4.0");
			attributes.addAttribute("", "xmlns", "xmlns", "",
					"http://www.opengis.net/w3ds/0.4.0");
			attributes.addAttribute("", "xmlns:w3ds", "xmlns:w3ds", "",
					"http://www.opengis.net/w3ds/0.4.0");
			attributes.addAttribute("", "xmlns:ows", "xmlns:ows", "",
					"http://www.opengis.net/ows/1.1");
			attributes.addAttribute("", "xmlns:xlink", "xmlns:xlink", "",
					"http://www.w3.org/1999/xlink");
			// attributes.addAttribute("", "xsi:schemaLocation",
			// "xsi:schemaLocation", "", "w3dsCapabilities.xsd");

			// start("W3DSB_Capabilities", attributes);
			// start("Capabilities");

			/*
			 * handleService(); handleCapability();
			 */

			start("w3ds:Capabilities", attributes);
			handleService();
			handleOperationsMetadata();
			start("w3ds:Contents");
			handleFeatureTypes();
			end("w3ds:Contents");
			end("w3ds:Capabilities");

		}

		private void handleService() {
			start("ows:ServiceIdentification");
			W3DSInfo w3dsInfo = geoServer.getService("w3ds", W3DSInfo.class);
			element("ows:Title", w3dsInfo.getTitle());
			element("ows:Abstract", w3dsInfo.getAbstract());
			element("ows:ServiceType", "OGC W3DS");
			element("ows:ServiceTypeVersion", "0.4.0");
			element("ows:Fees", w3dsInfo.getFees());
			element("ows:AccessConstraints", w3dsInfo.getAccessConstraints());
			end("ows:ServiceIdentification");
		}

		private void handleOperationsMetadata() {
			start("ows:OperationsMetadata");
			handleOperation("GetCapabilities");
			handleOperation("GetScene");
			handleOperation("GetTile");
			handleOperation("GetFeatureInfo");
			end("ows:OperationsMetadata");
		}

		private void handleDCP() {
			String serviceUrl = request.getRequestURL().toString() + "?";
			serviceUrl = appendQueryString(serviceUrl, "");
			AttributesImpl orAtts = new AttributesImpl();
			orAtts.addAttribute("", "xlink:href", "xlink:href", "", serviceUrl);
			start("ows:DCP");
			start("ows:HTTP");
			start("ows:Get", orAtts);
			orAtts = new AttributesImpl();
			orAtts.addAttribute("", "name", "name", "", "GetEncoding");
			start("ows:Constraint", orAtts);
			start("ows:AllowedValues");
			element("ows:Value", "KVP");
			end("ows:AllowedValues");
			end("ows:Constraint");
			end("ows:Get");
			end("ows:HTTP");
			end("ows:DCP");
		}

		private void handleOperation(String name) {
			AttributesImpl operationAtts = new AttributesImpl();
			operationAtts.addAttribute("", "name", "name", "", name);
			start("ows:Operation", operationAtts);
			handleDCP();
			end("ows:Operation");
		}

		private void handleFeatureTypes() {
			List featureTypes = new ArrayList(catalog.getFeatureTypes());
			for (Iterator it = featureTypes.iterator(); it.hasNext();) {
				FeatureTypeInfo ft = (FeatureTypeInfo) it.next();
				if (!ft.enabled())
					it.remove();
			}
			Collections
					.sort(featureTypes, new FeatureTypeInfoTitleComparator());
			for (Iterator it = featureTypes.iterator(); it.hasNext();) {
				FeatureTypeInfo ftype = (FeatureTypeInfo) it.next();
				handleFeatureType(ftype);
			}
		}

		private void handleFeatureType(FeatureTypeInfo info) {
			x3dInfoExtract.setFeatureInfo(info);
			if (x3dInfoExtract.isAX3DLayer()) {
				start("w3ds:Layer");
				element("ows:Title", info.getTitle());
				element("ows:Abstract", info.getAbstract());
				element("ows:Identifier", info.getPrefixedName());
				AttributesImpl bboxAtts = new AttributesImpl();
				bboxAtts.addAttribute("", "crs", "crs", "", info.getSRS());
				start("ows:BoundingBox", bboxAtts);
				Envelope bbox = info.getNativeBoundingBox();
				element("ows:LowerCorner",
						bbox.getMinX() + " " + bbox.getMinY());
				element("ows:UpperCorner",
						bbox.getMaxX() + " " + bbox.getMaxY());
				end("ows:BoundingBox");
				element("ows:OutputFormat", "model/x3d+xml");
				element("ows:OutputFormat", "text/html");
				element("w3ds:DefaultCRS", info.getSRS());
				element("w3ds:Queriable",
						String.valueOf(x3dInfoExtract.isQueryable()));
				element("w3ds:Tiled", String.valueOf(x3dInfoExtract.isTiled()));
				if (x3dInfoExtract.haveLODS()) {
					try {
						LODSet lodSet = x3dInfoExtract.getLODSet();
						if (lodSet != null) {
							handleLODSet(lodSet);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (x3dInfoExtract.isTiled()) {
					try {
						TileSet tileSet = x3dInfoExtract.getTileSet();
						if (tileSet != null) {
							handleTileSet(tileSet);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				handleStyles();
				end("w3ds:Layer");
			}
		}

		private void handleTileSet(TileSet tileSet) {
			start("w3ds:TileSet");
			element("ows:Identifier", tileSet.getIdentifier());
			element("w3ds:CRS", CRS.toSRS(tileSet.getCrs()));
			element("w3ds:TileSizes", tileSet.getTileSizesString());
			element("w3ds:LowerCorner", tileSet.getLowerCornerX() + " "
					+ tileSet.getLowerCornerY());
			end("w3ds:TileSet");
		}

		private void handleStyles() {
			List<StyleInfo> styles = x3dInfoExtract.getStyles();
			for (StyleInfo s : styles) {
				handleStyle(s);
			}
			StyleInfo defaultStyle = x3dInfoExtract.getDefaultStyle();
			if (defaultStyle != null) {
				handleDefaultStyle(defaultStyle);
			}
		}

		private void handleStyle(StyleInfo styleInfo) {
			start("w3ds:Style");
			try {
				Description styleDescription = styleInfo.getStyle()
						.getDescription();
				InternationalString title = styleDescription.getTitle();
				if (title != null) {
					element("ows:Title", title.toString());
				}
				InternationalString abstractTxt = styleDescription
						.getAbstract();
				if (abstractTxt != null) {
					element("ows:Abstract", abstractTxt.toString());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			element("ows:Identifier", styleInfo.getName());
			element("w3ds:IsDefault", "false");
			end("w3ds:Style");
		}

		private void handleDefaultStyle(StyleInfo styleInfo) {
			start("w3ds:Style");
			try {
				Description styleDescription = styleInfo.getStyle()
						.getDescription();
				InternationalString title = styleDescription.getTitle();
				if (title != null) {
					element("ows:Title", title.toString());
				}
				InternationalString abstractTxt = styleDescription
						.getAbstract();
				if (abstractTxt != null) {
					element("ows:Abstract", abstractTxt.toString());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			element("ows:Identifier", styleInfo.getName());
			element("w3ds:IsDefault", "true");
			end("w3ds:Style");
		}

		private void handleLOD(LOD lod) {
			start("w3ds:LOD");
			element("ows:Title", lod.getTitle());
			element("ows:Abstract", lod.getAbstractTxt());
			element("ows:Identifier", lod.getIdentifier());
			element("w3ds:LODValue", "CityGML:" + lod.getLodValue());
			element("w3ds:DefaultRange", String.valueOf(lod.getDefaultRange()));
			end("w3ds:LOD");
		}

		private void handleLODSet(LODSet lodSet) {
			start("w3ds:LODSet");
			List<LOD> lods = lodSet.getLodSet();
			for (LOD l : lods) {
				handleLOD(l);
			}
			end("w3ds:LODSet");
		}
	}

}
