/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Nuno Oliveira - PTInovacao
 */

package org.geoserver.w3ds.responses;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.w3ds.kml.KmlBuilder;
import org.geoserver.w3ds.types.GetSceneRequest;
import org.geoserver.w3ds.types.GetTileRequest;
import org.geoserver.w3ds.types.Scene;
import org.geoserver.w3ds.types.W3DSLayer;
import org.geoserver.w3ds.x3d.X3DBuilder;
import org.opengis.geometry.BoundingBox;

import de.micromata.opengis.kml.v_2_2_0.Kml;

public class KmlResponse extends Response {
	public KmlResponse() {
		super(Scene.class);
	}

	public boolean canHandle(Operation operation) {
		Object o = operation.getParameters()[0];
		if (o instanceof GetSceneRequest) {
			GetSceneRequest gs = (GetSceneRequest) operation.getParameters()[0];
			return "GetScene".equalsIgnoreCase(operation.getId())
					&& operation.getService().getId().equals("w3ds")
					&& gs.getFormat()
							.getMimeType()
							.equalsIgnoreCase(
									org.geoserver.w3ds.utilities.Format.KML
											.getMimeType());
		}
		return false;
	}

	public String getMimeType(Object value, Operation operation) {
		return org.geoserver.w3ds.utilities.Format.KML.getMimeType();
	}

	public String getAttachmentFileName(Object value, Operation operation) {
		return "kml_model.kml";
	}

	public void write(Object o, OutputStream output, Operation operation)
			throws IOException {
		Object request = operation.getParameters()[0];
		GetSceneRequest gs = (GetSceneRequest) request;
		writeGetScene((Scene) o, output, gs);
	}

	private void writeGetScene(Scene scene, OutputStream output,
			GetSceneRequest gs) throws IOException {
		KmlBuilder kmlBuilder = new KmlBuilder();
		for (W3DSLayer layer : scene.getLayers()) {
			kmlBuilder.addW3DSLayer(layer);
		}
		Marshaller marshaller;
		try {
			marshaller = JAXBContext.newInstance((Kml.class))
					.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(kmlBuilder.getKml(), output);
		} catch (JAXBException exception) {
			throw new ServiceException(exception);
		}
	}
}
