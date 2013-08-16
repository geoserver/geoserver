/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.kvp;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.w3ds.types.GetSceneRequest;
import org.geoserver.w3ds.types.W3DSLayerInfo;
import org.geoserver.w3ds.utilities.Format;
import org.geoserver.w3ds.utilities.Operation;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;

public class GetSceneKvpRequestReader extends KvpRequestReader {

	private GeoServer geoServer;
	private Catalog catalog;

	public GetSceneKvpRequestReader(Class requestBean, Catalog catalog,
			GeoServer geoServer) throws IOException, ParserConfigurationException, SAXException {
		super(requestBean);
		this.catalog = catalog;
		this.geoServer = geoServer;
	}

	public GetSceneRequest read(Object request, Map kvp, Map rawKvp) throws Exception {
		GetSceneRequest gsr = (GetSceneRequest) super.read(request, kvp, rawKvp);
		String aux = (String) rawKvp.get("SERVICE");
		if (aux == null) {
			throw new IllegalArgumentException(
					"Mandatory parameter SERVICE is missing: "
							+ rawKvp.toString());
			
		}
		// TODO: Test service validity.
		String service = "W3DS";
		aux = (String) rawKvp.get("VERSION");
		if (aux == null) {
			throw new IllegalArgumentException(
					"Mandatory parameter VERSION is missing: "
							+ rawKvp.toString());
		}
		// TODO: Test version validity.
		String version = "0.4.0";
		aux = (String) rawKvp.get("REQUEST");
		if (aux == null) {
			throw new IllegalArgumentException(
					"Mandatory parameter REQUEST is missing: "
							+ rawKvp.toString());
		}
		// TODO: Test request validity.
		Operation requestStr = Operation.GETSCENE;
		aux = (String) rawKvp.get("FORMAT");
		if (aux == null) {
			throw new IllegalArgumentException(
					"Mandatory parameter FORMAT is missing: "
							+ rawKvp.toString());
		}
		Format format = KVPUtils.parseFormat(aux, gsr);
		aux = (String) rawKvp.get("CRS");
		if (aux == null) {
			throw new IllegalArgumentException(
					"Mandatory parameter CRS is missing: " + rawKvp.toString());
		}
		CoordinateReferenceSystem crs = KVPUtils.parseCRS(aux);
		aux = (String) rawKvp.get("BOUNDINGBOX");
		if (aux == null) {
			throw new IllegalArgumentException(
					"Mandatory parameter BOUNDINGBOX is missing: "
							+ rawKvp.toString());
		}
		Envelope bbox = KVPUtils.parseBbox(aux, crs, LOGGER);
		aux = (String) rawKvp.get("LAYERS");
		if (aux == null) {
			throw new IllegalArgumentException(
					"Mandatory parameter LAYERS is missing: "
							+ rawKvp.toString());
		}
		List<W3DSLayerInfo> layers = KVPUtils.parseLayers(aux, catalog, LOGGER);
		aux = (String) rawKvp.get("STYLES");
		if (aux != null) {
			KVPUtils.parseStyles(layers, aux, catalog);
		}
		else {
			KVPUtils.setDefaultStyles(layers, catalog);
		}
		// TODO: Handle the baseURL parameter.
		String baseUrl = "geoserver/w3ds?";
		aux = (String) rawKvp.get("OFFSET");
		Coordinate offset = KVPUtils.parseOffset(aux, bbox);
		gsr.setMandatoryParameters(service, requestStr, version, baseUrl, rawKvp, crs, bbox, format, layers);
		gsr.setOffset(offset);
		// Hacking to use WMS to generate KML(provisory)
		gsr.setGeoServer(geoServer);
		return gsr;
	}
	
}
