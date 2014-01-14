/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.responses;

import java.io.IOException;
import java.io.OutputStream;

import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.w3ds.types.GetSceneRequest;
import org.geoserver.w3ds.types.GetTileRequest;
import org.geoserver.w3ds.types.Scene;
import org.geoserver.w3ds.types.W3DSLayer;
import org.geoserver.w3ds.x3d.X3DBuilder;
import org.opengis.geometry.BoundingBox;

public class X3DResponse extends Response {
	public X3DResponse() {
		super(Scene.class);
	}

	public boolean canHandle(Operation operation) {
		Object o = operation.getParameters()[0];
		if (o instanceof GetTileRequest) {
			GetTileRequest gs = (GetTileRequest) operation.getParameters()[0];
			return "GetTile".equalsIgnoreCase(operation.getId())
					&& operation.getService().getId().equals("w3ds")
					&& gs.getFormat()
							.getMimeType()
							.equalsIgnoreCase(
									org.geoserver.w3ds.utilities.Format.X3D
											.getMimeType());
		}
		if (o instanceof GetSceneRequest) {
			GetSceneRequest gs = (GetSceneRequest) operation.getParameters()[0];
			return "GetScene".equalsIgnoreCase(operation.getId())
					&& operation.getService().getId().equals("w3ds")
					&& gs.getFormat()
							.getMimeType()
							.equalsIgnoreCase(
									org.geoserver.w3ds.utilities.Format.X3D
											.getMimeType());
		}
		return false;
	}

	public String getMimeType(Object value, Operation operation) {
		return "model/x3d+xml";
	}

	public String getAttachmentFileName(Object value, Operation operation) {
		return "x3d_model.x3d";
	}

	private void writeGetScene(Scene scene, OutputStream output,
			GetSceneRequest gs) throws IOException {
		X3DBuilder x3d = new X3DBuilder(output);
		double[] origin = { gs.getOffset().x, gs.getOffset().y,
				gs.getOffset().z };
		x3d.createGeoOrigin(gs.getCrs(), origin);
		for (W3DSLayer layer : scene.getLayers()) {
			x3d.addW3DSLayer(layer);
		}
		x3d.writeX3D();
		x3d.close();
	}

	private void writeGetTile(Scene scene, OutputStream output,
			GetTileRequest gt) throws IOException {
		X3DBuilder x3d = new X3DBuilder(output);
		for (W3DSLayer layer : scene.getLayers()) {
			BoundingBox bbox = null;
			try {
				bbox = layer.getLayerInfo().getLayerInfo().getResource()
						.boundingBox();
			} catch (Exception e) {
				e.printStackTrace();
			}
			x3d.createGeoOrigin(gt.getCrs(), bbox);
			x3d.addW3DSLayer(layer);
		}
		x3d.writeX3D();
		x3d.close();
	}

	public void write(Object o, OutputStream output, Operation operation)
			throws IOException {
		Object request = operation.getParameters()[0];
		if (request instanceof GetTileRequest) {
			GetTileRequest gt = (GetTileRequest) request;
			writeGetTile((Scene) o, output, gt);
		} else if (request instanceof GetSceneRequest) {
			GetSceneRequest gs = (GetSceneRequest) request;
			writeGetScene((Scene) o, output, gs);
		} else {
			throw new ServiceException("Don't reconize the request");
		}
	}

}
