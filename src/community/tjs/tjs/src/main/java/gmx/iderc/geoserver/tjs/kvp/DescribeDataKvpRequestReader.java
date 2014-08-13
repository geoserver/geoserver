/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.kvp;

import gmx.iderc.geoserver.tjs.TJSException;
import net.opengis.tjs10.DescribeDataType;

import java.util.Map;

/**
 * @author root
 */
public class DescribeDataKvpRequestReader extends TJSKvpRequestReader {

    public DescribeDataKvpRequestReader() {
        super(DescribeDataType.class);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        request = super.read(request, kvp, rawKvp);

        DescribeDataType describeDataRequest = (DescribeDataType) request;

        if (kvp.containsKey("frameworkURI")) {
            describeDataRequest.setFrameworkURI(kvp.get("frameworkURI").toString());
        } else {
            throw new TJSException("Query must define a FrameworkUri parameter.");
        }
        if (kvp.containsKey("datasetURI")) {
            describeDataRequest.setDatasetURI(kvp.get("datasetURI").toString());
        } else {
            throw new TJSException("Query must define a DatasetUri parameter.");
        }
        if (kvp.containsKey("attributes")) {
            describeDataRequest.setAttributes(kvp.get("attributes").toString());
        }

        return request;
    }


}
