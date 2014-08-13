/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */
package org.geoserver.w3ds.x3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;

public abstract class X3DGeometry {

	private X3DGeometryType type;
	private StringBuilder points;
	private StringBuilder indexis;
	private Map<Integer, Integer> pointsIndexis;
	private int index;

	protected X3DNode appearance;
	protected int coordinatesType;
	protected X3DAttribute geoSystem;

	protected List<Coordinate> coordinates;

	public X3DGeometry(X3DGeometryType type) {
		this.type = type;
		this.points = new StringBuilder();
		this.indexis = new StringBuilder();
		this.pointsIndexis = new HashMap<Integer, Integer>();
		this.index = -1;
		this.coordinatesType = X3DDefinitions.GEOGRAPHIC_METRIC.getCode();
		this.geoSystem = null;
		this.appearance = new X3DNode();
		this.appearance.setValid(false);
		this.coordinates = new ArrayList<Coordinate>();
	}

	public X3DGeometry(X3DGeometryType type, X3DAttribute geoSystem) {
		this.type = type;
		this.points = new StringBuilder();
		this.indexis = new StringBuilder();
		this.pointsIndexis = new HashMap<Integer, Integer>();
		this.index = -1;
		this.coordinatesType = X3DDefinitions.GEOGRAPHIC_DEGREES.getCode();
		this.geoSystem = geoSystem;
		this.appearance = new X3DNode();
		this.appearance.setValid(false);
		this.coordinates = new ArrayList<Coordinate>();
	}

	public X3DGeometry(X3DGeometryType type, X3DAttribute geoSystem,
			int coordinatesType) {
		this.type = type;
		this.points = new StringBuilder();
		this.indexis = new StringBuilder();
		this.pointsIndexis = new HashMap<Integer, Integer>();
		this.index = -1;
		this.coordinatesType = coordinatesType;
		this.geoSystem = geoSystem;
		this.appearance = new X3DNode();
		this.appearance.setValid(false);
		this.coordinates = new ArrayList<Coordinate>();
	}

	public int getCoordinatesType() {
		return coordinatesType;
	} 

	public void setCoordinatesType(int coordinatesType) {
		this.coordinatesType = coordinatesType;
	}

	private static Coordinate[] removeDuplicate(Coordinate[] coordinates) {
		/*Set<Coordinate> set = new HashSet<Coordinate>();
		ArrayList<Coordinate> no_duplicates = new ArrayList<Coordinate>();
		for (int i = 0; i < coordinates.length; i++) {
			if (set.add(coordinates[i])) {
				no_duplicates.add(coordinates[i]);
			}
		}
		return no_duplicates.toArray(new Coordinate[no_duplicates.size()]);*/
		
		ArrayList<Coordinate> no_duplicates = new ArrayList<Coordinate>();
		for (int i = 0; i < coordinates.length - 2; i++) {
			no_duplicates.add(coordinates[i]);
		}
		return no_duplicates.toArray(new Coordinate[no_duplicates.size()]);
	}

	public void add(Coordinate[] coordinates) {
		//coordinates = removeDuplicate(coordinates); 
		for (Coordinate coordinate : coordinates) {
			if (coordinate.z == Double.NaN) {
				coordinate.z = 0;
			}
			if (this.pointsIndexis.containsKey(this.getHashCode(coordinate))) {
				this.indexis.append(pointsIndexis.get(this
						.getHashCode(coordinate)) + " ");
				// this.index++;
			} else {
				this.coordinates.add(coordinate);
				String x = String.valueOf(coordinate.x);
				String y = String.valueOf(coordinate.y);
				String z = "0.0";
				Double zValue = new Double(coordinate.z);
				if(!(zValue.equals(Double.NaN))) {
					z = String.valueOf(coordinate.z);
				}
				if (coordinatesType == X3DDefinitions.GEOGRAPHIC_DEGREES
						.getCode()) {
					//this.points.append(String.valueOf(coordinate.x) + " ");
					//this.points.append(String.valueOf(coordinate.y) + " ");
					//this.points.append(String.valueOf(coordinate.z) + " ");
					this.points.append(x + " ");
					this.points.append(y + " ");
					this.points.append(z + " ");
				} else {
					this.points.append(y + " ");
					this.points.append(z + " ");
					this.points.append(x + " ");
				}
				this.index++;
				this.pointsIndexis
						.put(this.getHashCode(coordinate), this.index);
				this.indexis.append(this.index + " ");
			}
		}
		this.indexis.append("-1 ");
	}

	public void addG(Coordinate[] coordinates) {
		for (Coordinate coordinate : coordinates) {
			if (coordinate.z == Double.NaN) {
				coordinate.z = 0;
			}
			if (this.pointsIndexis.containsKey(coordinate)) {
				this.indexis.append(pointsIndexis.get(coordinate) + " ");
				this.index++;
			} else {
				this.coordinates.add(coordinate);
				this.points.append(String.valueOf(coordinate.x) + " ");
				this.points.append(String.valueOf(coordinate.y) + " ");
				this.points.append(String.valueOf(coordinate.z) + " ");
				this.index++;
				this.pointsIndexis
						.put(this.getHashCode(coordinate), this.index);
				this.indexis.append(this.index + " ");
			}
		}
		this.indexis.append("-1 ");
	}

	public String getPoints() {
		return this.points.toString();
	}

	public String getIndexis() {
		return this.indexis.toString();
	}

	public boolean haveGeometries() {
		return index != -1;
	}

	private int getHashCode(Coordinate coordinate) {
		int result = 17;
		result = 37 * result + hashCode(coordinate.x);
		result = 37 * result + hashCode(coordinate.y);
		result = 37 * result + hashCode(coordinate.z);
		// System.out.println(coordinate + " " + result);
		return result;
	}

	private int hashCode(double x) {
		long f = Double.doubleToLongBits(x);
		return (int) (f ^ (f >>> 32));
	}

	public X3DGeometryType getType() {
		return type;
	}

	public void setType(X3DGeometryType type) {
		this.type = type;
	}

	public void setAppearance(X3DNode appearance) {
		this.appearance = appearance;
	}

	public abstract X3DNode getX3D();

	protected static double[] calculateBbox(List<Coordinate> coordinates) {
		double bbox[] = new double[4];
		bbox[0] = Double.MAX_VALUE;
		bbox[1] = Double.MAX_VALUE;
		bbox[2] = -1 * Double.MAX_VALUE;
		bbox[3] = -1 * Double.MAX_VALUE;
		for (Coordinate c : coordinates) {
			if (c.x < bbox[0])
				bbox[0] = c.x;
			if (c.x > bbox[2])
				bbox[2] = c.x;
			if (c.y < bbox[1])
				bbox[1] = c.y;
			if (c.y > bbox[3])
				bbox[3] = c.y;
		}
		return bbox;
	}

}
