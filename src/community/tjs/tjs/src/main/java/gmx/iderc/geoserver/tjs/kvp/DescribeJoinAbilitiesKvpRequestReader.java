/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.kvp;


import net.opengis.tjs10.RequestBaseType;

import java.util.Map;


/**
 * @author root
 */
public class DescribeJoinAbilitiesKvpRequestReader extends TJSKvpRequestReader {

    public DescribeJoinAbilitiesKvpRequestReader() {
        super(RequestBaseType.class);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        request = super.read(request, kvp, rawKvp);
        return request;
    }


}
