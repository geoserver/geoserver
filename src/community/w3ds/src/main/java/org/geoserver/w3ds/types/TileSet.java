/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TileSet {
	
	private String identifier;
	private CoordinateReferenceSystem crs;
	private List<Float> tileSizes;
	private double lowerCornerX;
	private double lowerCornerY;
	
	public TileSet() {
		this.tileSizes = new ArrayList<Float>();
	}
	
	public TileSet(String identifier, CoordinateReferenceSystem crs, double lowerCornerX, double lowerCornerY) {
		this.identifier = identifier;
		this.crs = crs;
		this.lowerCornerX = lowerCornerX;
		this.lowerCornerY = lowerCornerY;
		this.tileSizes = new ArrayList<Float>();
	}
	
	public TileSet(String identifier, CoordinateReferenceSystem crs,
			List<Float> tileSizes, double lowerCornerX, double lowerCornerY) {
		this.identifier = identifier;
		this.crs = crs;
		this.tileSizes = tileSizes;
		this.lowerCornerX = lowerCornerX;
		this.lowerCornerY = lowerCornerY;
	}
	
	public void addTileSize(float size) {
		tileSizes.add(size);
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public CoordinateReferenceSystem getCrs() {
		return crs;
	}

	public void setCrs(CoordinateReferenceSystem crs) {
		this.crs = crs;
	}

	public List<Float> getTileSizes() {
		return tileSizes;
	}

	public void setTileSizes(List<Float> tileSizes) {
		this.tileSizes = tileSizes;
	}

	public double getLowerCornerX() {
		return lowerCornerX;
	}

	public void setLowerCornerX(double lowerCornerX) {
		this.lowerCornerX = lowerCornerX;
	}

	public double getLowerCornerY() {
		return lowerCornerY;
	}

	public void setLowerCornerY(double lowerCornerY) {
		this.lowerCornerY = lowerCornerY;
	}
	
	public String getTileSizesString() {
		StringBuilder stb = new StringBuilder();
		Collections.sort(tileSizes, Collections.reverseOrder());
		Iterator iterator = tileSizes.iterator(); 
		while(iterator.hasNext()) {
			stb.append(String.valueOf((Float)iterator.next()));
			if(iterator.hasNext()) {
				stb.append(" ");
			}
		}
		return stb.toString();
	}
	
}
