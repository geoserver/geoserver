/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.util.List;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.wfs.xml.v1_0_0.WFSBindingUtils;
import org.geotools.xsd.Node;

/** WFS 1.x specific helper for SqlViewParams handling. */
public class WFS1XSqlViewParamsHelper {

    /** Set the viewParams in the binding class manually */
    public static void viewParams(EObject object, Node node) throws Exception {
        if (node.hasAttribute("viewParams")) {
            String rawViewParams = (String) node.getAttributeValue("viewParams");
            List viewParams =
                    (List) SqlViewParamsExtractor.getWfsSqlViewKvpParser().parse(rawViewParams);
            WFSBindingUtils.set(object, "viewParams", viewParams);
        }
    }
}
