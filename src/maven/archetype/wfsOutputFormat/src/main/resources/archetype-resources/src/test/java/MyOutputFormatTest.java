/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package ${groupId};

import java.io.InputStream;

import org.geoserver.wfs.WFSTestSupport;


public class MyOutputFormatTest extends WFSTestSupport {

    public void testOutputFormat() throws Exception {
        //execute a mock request using the output format
        InputStream in = get( "wfs?request=GetFeature&typeName=cite:Buildings" + 
           "&outputFormat=myOutputFormat");
        print( in ); 

        //make assertions here
    }
}
