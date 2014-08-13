/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.kvp;

import net.opengis.tjs10.DescribeFrameworksType;

import java.util.Map;

/**
 * @author root
 */
public class DescribeFrameworksKvpRequestReader extends TJSKvpRequestReader {

    public DescribeFrameworksKvpRequestReader() {
        super(DescribeFrameworksType.class);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        request = super.read(request, kvp, rawKvp);

        DescribeFrameworksType describeFrameworksRequest = (DescribeFrameworksType) request;

        if (kvp.containsKey("frameworkURI")) {
            describeFrameworksRequest.setFrameworkURI(kvp.get("frameworkURI").toString());
        }

        return request;
    }


}
