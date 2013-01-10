/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.w3ds.types.LODSet;
import org.geoserver.w3ds.types.TileSet;
import org.xml.sax.SAXException;

public class X3DInfoExtract {

	private Catalog catalog;
	private GeoServerResourceLoader resourceLoader;
	private Logger LOGGER;
	private TileSetsParser tileSetsParser;
	private LODSetParser LODSetsParser;

	private ResourceInfo resourceInfo;
	private MetadataMap info;

	public X3DInfoExtract(GeoServer geoServer) throws IOException,
			ParserConfigurationException, SAXException {
		init(geoServer.getCatalog(), true);
	}

	public X3DInfoExtract(Catalog catalog) throws IOException,
			ParserConfigurationException, SAXException {
		init(catalog, true);
	}

	public X3DInfoExtract(GeoServer geoServer, boolean searchTL)
			throws IOException, ParserConfigurationException, SAXException {
		init(geoServer.getCatalog(), false);
	}

	public X3DInfoExtract(Catalog catalog, boolean searchTL)
			throws IOException, ParserConfigurationException, SAXException {
		init(catalog, false);
	}

	public X3DInfoExtract() {
		catalog = null;
		resourceLoader = null;
		this.LOGGER = org.geotools.util.logging.Logging
				.getLogger("org.geoserver.ows");
		this.tileSetsParser = new TileSetsParser();
		this.LODSetsParser = new LODSetParser();
		resourceInfo = null;
		info = new MetadataMap();
	}

	private void init(Catalog catalog, boolean searchTL) throws IOException,
			ParserConfigurationException, SAXException {
		this.catalog = catalog;
		this.resourceLoader = this.catalog.getResourceLoader();
		this.LOGGER = org.geotools.util.logging.Logging
				.getLogger("org.geoserver.ows");
		this.resourceInfo = null;
		info = new MetadataMap();
		if (searchTL) {
			File dir = this.resourceLoader.findOrCreateDirectory("tilesets");
			this.tileSetsParser = new TileSetsParser(dir, LOGGER);
			dir = this.resourceLoader.findOrCreateDirectory("lodsets");
			this.LODSetsParser = new LODSetParser(dir, LOGGER);
		} else {
			this.tileSetsParser = new TileSetsParser();
			this.LODSetsParser = new LODSetParser();
		}
	}

	public void setFeatureInfo(FeatureTypeInfo featureInfo) {
		this.resourceInfo = featureInfo;
		this.info = this.resourceInfo.getMetadata();
	}

	public void setResourceInfo(ResourceInfo ressourceInfo) {
		this.resourceInfo = ressourceInfo;
		this.info = this.resourceInfo.getMetadata();
	}

	public void setLayerInfo(LayerInfo layerInfo) {
		setResourceInfo(layerInfo.getResource());
	}

	private boolean getBoolean(String property) {
		if (this.info.containsKey(property)) {
			String value = this.info.get(property).toString();
			if (value != null) {
				return value.equalsIgnoreCase("true");
			}
		}
		return false;
	}

	private List<String> getListString(String property) {
		String strArray = this.info.get(property).toString();
		if (strArray != null) {
			return W3DSUtils.parseStrArray(strArray);
		}
		return new ArrayList<String>();
	}

	public boolean isAX3DLayer() {
		return getBoolean("x3d.activate");
	}

	public boolean isQueryable() {
		return getBoolean("x3d.queryable");
	}

	public boolean isTiled() {
		return getBoolean("x3d.tiled");
	}

	public boolean haveLODS() {
		return getBoolean("x3d.hLOD");
	}

	public TileSet getTileSet() throws ParserConfigurationException,
			SAXException, IOException {
		if (this.info.containsKey("x3d.tileSet")) {
			String identifier = this.info.get("x3d.tileSet").toString();
			if (identifier != null) {
				TileSet tileSet = tileSetsParser.getTileSet(identifier);
				if (tileSet != null) {
					return tileSet;
				}
			}
		}
		return null;
	}

	public LODSet getLODSet() throws ParserConfigurationException,
			SAXException, IOException {
		if (this.info.containsKey("x3d.LODSet")) {
			String identifier = this.info.get("x3d.LODSet").toString();
			if (identifier != null) {
				LODSet LODSet = LODSetsParser.getLODSet(identifier);
				if (LODSet != null) {
					return LODSet;
				}
			}
		}
		return null;
	}

	public List<StyleInfo> getStyles() {
		if (catalog == null)
			return new ArrayList<StyleInfo>();
		List<String> stylesName = getListString("x3d.styles");
		List<StyleInfo> styles = new ArrayList<StyleInfo>();
		for (String styleName : stylesName) {
			StyleInfo style = this.catalog.getStyleByName(styleName);
			if (style != null) {
				styles.add(style);
			}
		}
		return styles;
	}

	public List<String> getStylesNames() {
		return getListString("x3d.styles");
	}

	public boolean containsStyle(String styleName) {
		List<String> lis = getStylesNames();
		boolean b = lis.contains(styleName);
		return getStylesNames().contains(styleName);
	}

	public StyleInfo getDefaultStyle() {
		if (catalog == null)
			return null;
		if (this.info.containsKey("x3d.defaultStyle")) {
			String styleName = this.info.get("x3d.defaultStyle").toString();
			return catalog.getStyleByName(styleName);
		}
		return null;
	}

	public boolean haveObjectID() {
		if (this.info.containsKey("x3d.objectid")) {
			return true;
		}
		return false;
	}

	public String getObjectID() {
		if (this.info.containsKey("x3d.objectid")) {
			return this.info.get("x3d.objectid").toString();
		}
		return "";
	}

	public boolean haveObjectClass() {
		if (this.info.containsKey("x3d.objectclass")) {
			return true;
		}
		return false;
	}

	public List<String> getObjectClass() {
		if (this.info.containsKey("x3d.objectclass")) {
			return getListString("x3d.objectclass");
		}
		return new ArrayList<String>();
	}

}
