/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.util.List;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.kvp.ViewParamsKvpParser;
import org.geoserver.wfs.xml.v1_0_0.WFSBindingUtils;
import org.geotools.xsd.Node;

/** Static methods for accessing the ViewParams KVP parser. */
public class SqlViewParamsExtractor {

    /** Fully setup KVP parser for viewParams. Injected by spring at runtime */
    private static ViewParamsKvpParser wfsSqlViewKvpParser = null;

    public static ViewParamsKvpParser getWfsSqlViewKvpParser() {
        return wfsSqlViewKvpParser;
    }

    public static void setWfsSqlViewKvpParser(ViewParamsKvpParser wfsSqlViewKvpParser) {
        SqlViewParamsExtractor.wfsSqlViewKvpParser = wfsSqlViewKvpParser;
    }

    /**
     * Fix the node object to store a parsed list of viewParams instead of a raw string. This
     * prevents the parse() method choking later on...
     */
    public static void fixNodeObject(Node node) throws Exception {
        List viewParams = null;
        if (node.hasAttribute("viewParams")) {
            Node viewParamsAttribute = node.getAttribute("viewParams");
            viewParams = (List) wfsSqlViewKvpParser.parse((String) viewParamsAttribute.getValue());

            EList viewParamsList = new org.eclipse.emf.common.util.BasicEList();
            viewParamsList.addAll(viewParams);

            viewParamsAttribute.setValue(viewParamsList);
        }
    }

    /** Set the viewParams in the binding class manually */
    public static void viewParams(EObject object, Node node) throws Exception {
        if (node.hasAttribute("viewParams")) {
            String rawViewParams = (String) node.getAttributeValue("viewParams");
            List viewParams = (List) wfsSqlViewKvpParser.parse(rawViewParams);
            WFSBindingUtils.set(object, "viewParams", viewParams);
        }
    }
}
