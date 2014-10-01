/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.kvp;

import gmx.iderc.geoserver.tjs.TJSException;
import net.opengis.tjs10.DescribeDatasetsType;

import java.util.Map;

/**
 * @author root
 */
public class DescribeDatasetsKvpRequestReader extends TJSKvpRequestReader {

    public DescribeDatasetsKvpRequestReader() {
        super(DescribeDatasetsType.class);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        request = super.read(request, kvp, rawKvp);

        DescribeDatasetsType describeDataRequest = (DescribeDatasetsType) request;

        if (kvp.containsKey("frameworkURI")) {
            describeDataRequest.setFrameworkURI(kvp.get("frameworkURI").toString());
        } else {
            throw new TJSException("The query must contain a FrameworkURI value");
        }
        if (kvp.containsKey("datasetURI")) {
            describeDataRequest.setDatasetURI(kvp.get("datasetURI").toString());
        }
        return request;
    }


}
