/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.types;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Style;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class Scene {

	private List<W3DSLayer> layers;
	private CoordinateReferenceSystem crs;

	public Scene(CoordinateReferenceSystem crs) {
		this.layers = new ArrayList<W3DSLayer>();
		this.crs = crs;
	}

	public Scene(List<W3DSLayer> layers, CoordinateReferenceSystem crs) {
		this.layers = layers;
		this.crs = crs;
	}

	public Scene(List<W3DSLayer> layers) {
		this.layers = layers;
		this.crs = DefaultGeographicCRS.WGS84;
	}

	public Scene() {
		this.layers = new ArrayList<W3DSLayer>();
		this.crs = DefaultGeographicCRS.WGS84;
	}

	public List<W3DSLayer> getLayers() {
		return layers;
	}

	public void setLayers(List<W3DSLayer> layers) {
		this.layers = layers;
	}

	public void add(W3DSLayerInfo layerInfo,
			FeatureCollection<? extends FeatureType, ? extends Feature> features, Catalog catalog)
			throws IOException {
		this.layers.add(new W3DSLayer(layerInfo, features, catalog));
	}

	public void add(
			W3DSLayerInfo layerInfo,
			FeatureCollection<? extends FeatureType, ? extends Feature> features,
			List<Style> styles, Catalog catalog) throws IOException {
		this.layers.add(new W3DSLayer(layerInfo, features, catalog));
	}

	public CoordinateReferenceSystem getCrs() {
		return crs;
	}

	public void setCrs(CoordinateReferenceSystem crs) {
		this.crs = crs;
	}

}
