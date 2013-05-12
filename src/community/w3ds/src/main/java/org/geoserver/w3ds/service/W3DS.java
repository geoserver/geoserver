/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.w3ds.types.FeatureInfo;
import org.geoserver.w3ds.types.GetFeatureInfoRequest;
import org.geoserver.w3ds.types.GetSceneRequest;
import org.geoserver.w3ds.types.GetTileRequest;
import org.geoserver.w3ds.types.Scene;
import org.geotools.filter.IllegalFilterException;
import org.geotools.xml.transform.TransformerBase;
import org.opengis.referencing.FactoryException;
import org.xml.sax.SAXException;

public class W3DS {

	protected GeoServer geoServer;
	protected Catalog catalog;
	
	public W3DS(GeoServer gs) {
		this.geoServer = gs;
		this.catalog = gs.getCatalog();
	}

	public W3DS() {
		this.geoServer = null; 
		this.catalog = null;
	}
	
	public ServiceInfo getServiceInfo() {
		return geoServer.getService("w3ds", ServiceInfo.class);
    }

	public void SayHello(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.getOutputStream().write(
				"Hello, welcome to the 3D World !".getBytes());
	}

	public Scene GetScene(GetSceneRequest request)
			throws ServletException, IOException {
		GetScene gs = new GetScene(geoServer, catalog, request);
		gs.run();
		return gs.getScene();
	}
	
	public FeatureInfo GetFeatureInfo(GetFeatureInfoRequest request)
			throws ServletException, IOException, IllegalFilterException, FactoryException {
		GetFeatureInfo gfi = new GetFeatureInfo(geoServer, catalog, request);
		gfi.run();
		return gfi.getFeatureInfo();
	}
	
	public Scene GetTile(GetTileRequest request)
			throws ServletException, IOException {
		GetTile gt = new GetTile(geoServer, catalog, request);
		gt.run();
		return gt.getScene();
	}
	
	public TransformerBase GetCapabilities(HttpServletRequest request)
			throws ServletException, IOException, ParserConfigurationException, SAXException {
		GetCapabilities gc_op = new GetCapabilities(geoServer, catalog);
		return gc_op.run();
	}
}
