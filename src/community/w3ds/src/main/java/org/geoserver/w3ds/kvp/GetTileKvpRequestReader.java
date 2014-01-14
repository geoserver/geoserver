/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.kvp;

import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.w3ds.types.GetTileRequest;
import org.geoserver.w3ds.types.W3DSLayerInfo;
import org.geoserver.w3ds.utilities.Format;
import org.geoserver.w3ds.utilities.Operation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GetTileKvpRequestReader extends KvpRequestReader {

	static Catalog catalog;
	GeoServer geoServer;

	public GetTileKvpRequestReader(Class requestBean, Catalog catalog,
			GeoServer geoServer) {
		super(requestBean);
		this.catalog = catalog;
		this.geoServer = geoServer;
	}

	public GetTileRequest read(Object request, Map kvp, Map rawKvp) throws Exception {
		GetTileRequest gtr = (GetTileRequest) super.read(request, kvp, rawKvp);
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
		Operation requestStr = Operation.GETILE;
		aux = (String) rawKvp.get("FORMAT");
		if (aux == null) {
			throw new IllegalArgumentException(
					"Mandatory parameter FORMAT is missing: "
							+ rawKvp.toString());
		}
		Format format = KVPUtils.parseFormat(aux, gtr);
		aux = (String) rawKvp.get("CRS");
		if (aux == null) {
			throw new IllegalArgumentException(
					"Mandatory parameter CRS is missing: " + rawKvp.toString());
		}
		CoordinateReferenceSystem crs = KVPUtils.parseCRS(aux);
		aux = (String) rawKvp.get("LAYER");
		if (aux == null) {
			throw new IllegalArgumentException(
					"Mandatory parameter LAYER is missing: "
							+ rawKvp.toString());
		}
		W3DSLayerInfo layer = KVPUtils.parseTiledLayer(aux, catalog, LOGGER);
		aux = (String) rawKvp.get("STYLE");
		if (aux != null) {
			KVPUtils.parseStyle(layer, aux, catalog);
		}
		else {
			KVPUtils.setDefaultStyle(layer, catalog);
		}
		aux = (String) rawKvp.get("TILELEVEL");
		if (aux == null) {
			throw new IllegalArgumentException(
					"Mandatory parameter TILELEVEL is missing: "
							+ rawKvp.toString());
		}
		int tileLevel = Integer.parseInt(aux);
		aux = (String) rawKvp.get("TILEROW");
		if (aux == null) {
			throw new IllegalArgumentException(
					"Mandatory parameter TILEROW is missing: "
							+ rawKvp.toString());
		}
		int tileRow = Integer.parseInt(aux);
		aux = (String) rawKvp.get("TILECOL");
		if (aux == null) {
			throw new IllegalArgumentException(
					"Mandatory parameter TILECOL is missing: "
							+ rawKvp.toString());
		}
		int tileCol = Integer.parseInt(aux);
		// TODO: Handle the baseURL parameter.
		String baseUrl = "geoserver/w3ds?";
		gtr.setMandatoryParameters(crs, layer, format, tileLevel, tileRow, tileCol);
		// Hacking to use WMS to generate KML(provisory)
		gtr.setGeoServer(geoServer);
		return gtr;
	}
}
