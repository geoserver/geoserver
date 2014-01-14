package org.geoserver.w3ds.styles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.config.GeoServerDataDirectory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;

/**************************************************************************************************************
 * 
 * /!\ Provisory hack (very inefficient) to have 3D styles based on geotools styles without editing
 * geotools. In a near future a decent styles3d implementation will be made. /!\
 * 
 **************************************************************************************************************/

public class Styles3D {

	static StyleFactory styleFactory = CommonFactoryFinder
			.getStyleFactory(null);
	private GeoServerDataDirectory dataDir;

	public Styles3D(Catalog catalog) {
		this.dataDir = new GeoServerDataDirectory(catalog.getResourceLoader());
	}

	public Style getStyle(StyleInfo info) throws IOException {
		File styleFile = dataDir.findStyleSldFile(info);
		SLD3DParser p = parser(styleFile);
		StyledLayerDescriptor sld = p.parseSLD();
		if (sld.getStyledLayers().length == 0) {
			Style[] style = p.readDOM();
			if (style.length > 0) {
				NamedLayer l = styleFactory.createNamedLayer();
				l.addStyle(style[0]);
				sld.addStyledLayer(l);
			}
		}
		return Styles.style(sld);
	}

	private SLD3DParser parser(Object input) throws IOException {
		if (input instanceof File) {
			return new SLD3DParser(styleFactory, (File) input);
		} else {
			return new SLD3DParser(styleFactory, toReader(input));
		}
	}

	static Reader toReader(Object input) throws IOException {
		if (input instanceof Reader) {
			return (Reader) input;
		}

		if (input instanceof InputStream) {
			return new InputStreamReader((InputStream) input);
		}

		if (input instanceof File) {
			FileReader fr = new FileReader((File)input); 
			BufferedReader br = new BufferedReader(fr);
			String s;
			while((s = br.readLine()) != null) {
			System.out.println(s);
			}
			fr.close(); 
			return new FileReader((File) input);
		}

		throw new IllegalArgumentException("Unable to turn " + input
				+ " into reader");
	}
}
