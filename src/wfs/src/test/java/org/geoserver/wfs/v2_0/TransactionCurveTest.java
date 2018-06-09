/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import java.util.Map;
import org.geoserver.wfs.AbstractTransactionCurveTest;

/**
 * This class is going to use WFS 2.0 xml test fixtures located in this package
 *
 * @author Andrea Aime - GeoSolutions
 */
public class TransactionCurveTest extends AbstractTransactionCurveTest {

    @Override
    protected void setUpNamespaces(Map<String, String> namespaces) {
        // force wfs 2.0 namespaces, the base class uses wfs 1.1 ones
        namespaces.put("wfs", "http://www.opengis.net/wfs/2.0");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("fes", "http://www.opengis.net/fes/2.0");
        namespaces.put("gml", "http://www.opengis.net/gml/3.2");
    }
}
