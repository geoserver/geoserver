/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Nuno Oliveira
 * @author Juha Hyvärinen / Cyberlightning Ltd
 */

package org.geoserver.w3ds.responses;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.w3ds.types.GetSceneRequest;
import org.geoserver.w3ds.types.GetTileRequest;
import org.geoserver.w3ds.types.Scene;
import org.geoserver.w3ds.types.W3DSLayer;
import org.geoserver.w3ds.types.W3DSLayerInfo;
import org.geoserver.w3ds.utilities.Format;
import org.geoserver.w3ds.xml3d.XML3DBuilder;
import org.geotools.util.logging.Logging;

public class XML3DResponse extends Response {

private final static Logger LOGGER = Logging.getLogger(XML3DResponse.class);
        
public XML3DResponse() {
    super(Scene.class);
}

@Override
public boolean canHandle(Operation operation) {
    Object requestObject = operation.getParameters()[0];

    // Check if request format is supported by this implementation
    if (requestObject instanceof GetTileRequest) {
        return false;
    } else if (requestObject instanceof GetSceneRequest) {
        GetSceneRequest getSceneRequest = (GetSceneRequest) requestObject;
        if (getSceneRequest.getFormat() == Format.XML3D) {
            return true;
        } else if (getSceneRequest.getFormat() == Format.OCTET_STREAM) {
            return true;
        } else {
            return false;
        }
    }
    return false;
}

@Override
public String getMimeType(Object value, Operation operation) {
    Object op = operation.getParameters()[0];

    if (op instanceof GetSceneRequest) {
        return ((GetSceneRequest) op).getFormat().getMimeType();
    } else if (op instanceof GetTileRequest) {
        return ((GetTileRequest) op).getFormat().getMimeType();
    } else {
        return Format.XML3D.getMimeType();
    }
}

@Override
public String getAttachmentFileName(Object value, Operation operation) {
    StringBuilder fileName = new StringBuilder();
    Object requestObject = operation.getParameters()[0];
    if (requestObject instanceof GetSceneRequest) {
        GetSceneRequest getSceneRequest = (GetSceneRequest) requestObject;
        for (W3DSLayerInfo w3dsLayerInfo : getSceneRequest.getLayers()) {
            fileName.append(w3dsLayerInfo.getLayerInfo().getName());
        }
    }
    // if (requestObject instanceof GetTileRequest) {
    // GetTileRequest getTileRequest = (GetTileRequest) requestObject;
    // fileName.append(getTileRequest.getLayer().getLayerInfo().getName());
    // }
    fileName.append(".xml3d");
    return fileName.toString();
}

@Override
public void write(Object content, OutputStream outputStream, Operation operation)
        throws IOException {
    Object requestObject = operation.getParameters()[0];

    if (requestObject instanceof GetSceneRequest) {
        GetSceneRequest getSceneRequest = (GetSceneRequest) requestObject;
        writeGetScene((Scene) content, outputStream, getSceneRequest);
    } else {
        throw new ServiceException("The request is not recognised!");
    }
}

private void writeGetScene(Scene scene, OutputStream outputStream, GetSceneRequest getSceneRequest)
        throws IOException {
    XML3DBuilder xml3dBuilder = new XML3DBuilder(getSceneRequest.getBbox(), outputStream,
            getSceneRequest.getFormat());
    
    // Set LOD if it is requested
    if (getSceneRequest.getKpvPrs().containsKey("LOD")) {
        int LOD = Integer.parseInt(getSceneRequest.getKpvPrs().get("LOD"));
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("LOD Request with LOD value: " + LOD);
        }
        xml3dBuilder.setLOD(LOD);
    }

    // Add layers
    for (W3DSLayer layer : scene.getLayers()) {
        xml3dBuilder.addW3DSLayer(layer);
    }

    xml3dBuilder.writeOutput();
    xml3dBuilder.close();
}

private void writeGetTile(Scene scene, OutputStream outputStream, GetTileRequest getTileRequest)
        throws IOException {
    // TODO
}
}
