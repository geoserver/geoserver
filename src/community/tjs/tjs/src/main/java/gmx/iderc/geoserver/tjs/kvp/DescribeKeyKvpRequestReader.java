/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.kvp;

import net.opengis.tjs10.DescribeKeyType;

import java.util.Map;

/**
 * @author root
 */
public class DescribeKeyKvpRequestReader extends TJSKvpRequestReader {

    public DescribeKeyKvpRequestReader() {
        super(DescribeKeyType.class);
    }

    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        request = super.read(request, kvp, rawKvp);

        DescribeKeyType describeKeyRequest = (DescribeKeyType) request;

        if (kvp.containsKey("frameworkURI")) {
            describeKeyRequest.setFrameworkURI(kvp.get("frameworkURI").toString());
        }
        //aqui se arreglan los parametros que pudieran venir incompletos en la solicitud

        return describeKeyRequest;
    }


}
