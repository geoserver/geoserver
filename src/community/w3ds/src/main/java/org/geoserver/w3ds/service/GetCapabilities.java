/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.service;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.w3ds.utilities.CapabilitiesTransformer;
import org.xml.sax.SAXException;

public class GetCapabilities {

	protected GeoServer geoServer;
	protected Catalog catalog;

	public GetCapabilities(GeoServer geoServer, Catalog catalog) {
		this.geoServer = geoServer;
		this.catalog = catalog;
	}

	public CapabilitiesTransformer run() throws IOException, ParserConfigurationException, SAXException {
		CapabilitiesTransformer capsTransformer = new CapabilitiesTransformer(
				geoServer, catalog);
		return capsTransformer;
	}

}
