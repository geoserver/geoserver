/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.kvp;

import java.util.List;
import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.w3ds.types.GetFeatureInfoRequest;
import org.geoserver.w3ds.types.W3DSLayerInfo;
import org.geoserver.w3ds.utilities.Format;
import org.geoserver.w3ds.utilities.Operation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

public class GetFeatureInfoKvpRequestReader extends KvpRequestReader {

	static Catalog catalog;
	GeoServer geoServer;

	public GetFeatureInfoKvpRequestReader(Class requestBean, Catalog catalog,
			GeoServer geoServer) {
		super(requestBean);
		this.catalog = catalog;
		this.geoServer = geoServer;
	}

	public GetFeatureInfoRequest read(Object request, Map kvp, Map rawKvp) throws Exception {
		GetFeatureInfoRequest gfi = (GetFeatureInfoRequest) super.read(request, kvp, rawKvp);
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
		Operation requestStr = Operation.GETFEATUREINFO;
		aux = (String) rawKvp.get("FORMAT");
		if (aux == null) {
			throw new IllegalArgumentException(
					"Mandatory parameter FORMAT is missing: "
							+ rawKvp.toString());
		}
		Format format = KVPUtils.parseFormat(aux, gfi);
		aux = (String) rawKvp.get("CRS");
		if (aux == null) {
			throw new IllegalArgumentException(
					"Mandatory parameter CRS is missing: " + rawKvp.toString());
		}
		CoordinateReferenceSystem crs = KVPUtils.parseCRS(aux);
		aux = (String) rawKvp.get("LAYERS");
		if (aux == null) {
			throw new IllegalArgumentException(
					"Mandatory parameter LAYERS is missing: "
							+ rawKvp.toString());
		}
		List<W3DSLayerInfo> layers = KVPUtils.parseLayers(aux, catalog, LOGGER);
		// TODO: Handle the baseURL parameter.
		String baseUrl = "geoserver/w3ds?";
		aux = (String) rawKvp.get("COORDINATE");
		Coordinate coordinate = KVPUtils.parseCoordinate(aux, crs);
		gfi.setMandatoryParameters(service, requestStr, version, baseUrl, rawKvp, crs, format, layers, coordinate);
		// Hacking to use WMS to generate KML(provisory)
		gfi.setGeoServer(geoServer);
		return gfi;
	}
	
}
