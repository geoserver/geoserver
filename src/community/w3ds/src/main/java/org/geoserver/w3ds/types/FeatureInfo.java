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
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public class FeatureInfo {
	private List<W3DSLayer> layers;
	
	public FeatureInfo(List<W3DSLayer> layers) {
		this.layers = layers;
	}
	public FeatureInfo() {
		this.layers = new ArrayList();
	}
	
	public List<W3DSLayer> getLayers() {
		return layers;
	}
	
	public void setLayers(List<W3DSLayer> layers) {
		this.layers = layers;
	}
	
	public void add(W3DSLayerInfo layerInfo,
			FeatureCollection<? extends FeatureType, ? extends Feature> features, Catalog catalog) throws IOException {
		this.layers.add(new W3DSLayer(layerInfo, features, catalog));
	}
	
}
