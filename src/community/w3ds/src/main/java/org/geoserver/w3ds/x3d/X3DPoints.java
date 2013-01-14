/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.x3d;

public class X3DPoints extends X3DGeometry {

	public X3DPoints(X3DGeometryType type, X3DAttribute geoSystem) {
		super(type, geoSystem);
	}
	
	public X3DPoints(X3DGeometryType type, X3DAttribute geoSystem, int coordinatesType) {
		super(type, geoSystem, coordinatesType);
	}

	@Override
	public X3DNode getX3D() {
		// TODO Auto-generated method stub
		return null;
	}

}
