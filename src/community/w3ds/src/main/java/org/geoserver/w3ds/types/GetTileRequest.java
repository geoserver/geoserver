/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.types;

import java.util.List;
import java.util.Map;

import org.geoserver.config.GeoServer;
import org.geoserver.w3ds.utilities.Format;
import org.geoserver.w3ds.utilities.Operation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GetTileRequest extends W3DSRequest {

	// Mandatory Parameters
	private CoordinateReferenceSystem crs;
	private W3DSLayerInfo layer;
	private Format format;
	private int TileLevel;
	private int TileRow;
	private int TileCol;

	// Maybe unuseful
	private GeoServer geoServer;

	public GetTileRequest(String service, Operation request, String version,
			String baseUrl, Map<String, String> kpvPrs,
			List<Format> acceptedFormats, CoordinateReferenceSystem crs,
			W3DSLayerInfo layer, Format format, int tileLevel, int tileRow,
			int tileCol, GeoServer geoServer) {
		super(service, request, version, baseUrl, kpvPrs, acceptedFormats);
		this.crs = crs;
		this.layer = layer;
		this.format = format;
		TileLevel = tileLevel;
		TileRow = tileRow;
		TileCol = tileCol;
		this.geoServer = geoServer;
		this.addAcceptedFormat(Format.X3D);
		this.addAcceptedFormat(Format.HTML);
	}

	public GetTileRequest() {
		super(Operation.GETILE);
		this.crs = null;
		this.layer = null;
		this.format = null;
		TileLevel = -1;
		TileRow = -1;
		TileCol = -1;
		this.geoServer = geoServer;
		this.addAcceptedFormat(Format.X3D);
		this.addAcceptedFormat(Format.HTML);
	}

	public CoordinateReferenceSystem getCrs() {
		return crs;
	}

	public void setCrs(CoordinateReferenceSystem crs) {
		this.crs = crs;
	}

	public W3DSLayerInfo getLayer() {
		return layer;
	}

	public void setLayer(W3DSLayerInfo layer) {
		this.layer = layer;
	}

	public Format getFormat() {
		return format;
	}

	public void setFormat(Format format) {
		this.format = format;
	}

	public int getTileLevel() {
		return TileLevel;
	}

	public void setTileLevel(int tileLevel) {
		TileLevel = tileLevel;
	}

	public int getTileRow() {
		return TileRow;
	}

	public void setTileRow(int tileRow) {
		TileRow = tileRow;
	}

	public int getTileCol() {
		return TileCol;
	}

	public void setTileCol(int tileCol) {
		TileCol = tileCol;
	}

	public GeoServer getGeoServer() {
		return geoServer;
	}

	public void setGeoServer(GeoServer geoServer) {
		this.geoServer = geoServer;
	}

	public void setMandatoryParameters(CoordinateReferenceSystem crs,
			W3DSLayerInfo layer, Format format, int tileLevel, int tileRow,
			int tileCol) {
		this.crs = crs;
		this.layer = layer;
		this.format = format;
		TileLevel = tileLevel;
		TileRow = tileRow;
		TileCol = tileCol;
	}
}
