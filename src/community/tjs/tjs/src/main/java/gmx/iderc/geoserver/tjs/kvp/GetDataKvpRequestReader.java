/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.kvp;

import net.opengis.tjs10.GetDataType;

import java.util.Map;

/**
 * @author root
 */
public class GetDataKvpRequestReader extends TJSKvpRequestReader {

    public GetDataKvpRequestReader() {
        super(GetDataType.class);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        request = super.read(request, kvp, rawKvp);

        GetDataType describeDataRequest = (GetDataType) request;

        if (kvp.containsKey("frameworkURI")) {
            describeDataRequest.setFrameworkURI(kvp.get("frameworkURI").toString());
        }
        if (kvp.containsKey("datasetURI")) {
            describeDataRequest.setDatasetURI(kvp.get("datasetURI").toString());
        }
        if (kvp.containsKey("attributes")) {
            describeDataRequest.setAttributes(kvp.get("attributes").toString());
        }

        return request;
    }


}
