/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.responses;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geotools.xml.transform.TransformerBase;

public class GetCapabilitiesResponse extends Response {
	
    public GetCapabilitiesResponse() {
        super(TransformerBase.class);
    }
    
    public boolean canHandle(Operation operation) {
        return "GetCapabilities".equalsIgnoreCase(operation.getId()) && 
                operation.getService().getId().equals("w3ds");
    }

    public String getMimeType(Object value, Operation operation) {
        return "application/xml";
    }

    public void write(Object value, OutputStream output, Operation operation)
        throws IOException {
        TransformerBase tx = (TransformerBase) value;
        try {
            tx.transform(operation.getParameters()[0], output);
        } catch (TransformerException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }
}
