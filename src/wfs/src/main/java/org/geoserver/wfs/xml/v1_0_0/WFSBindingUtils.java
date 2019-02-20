/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import java.math.BigInteger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.geotools.xsd.Node;

/**
 * Utility class to be used by bindings.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class WFSBindingUtils {
    /**
     * Sets the service feature of the object passed in.
     *
     * <p>The service value is retreived as an attribute from the node, if <code>null</code>, the
     * default "WFS" is used.
     *
     * @param object An object which contains a feature named "service"
     * @param node The parse node.
     */
    public static void service(EObject object, Node node) {
        String service = (String) node.getAttributeValue("service");

        if (service == null) {
            service = "WFS";
        }

        set(object, "service", service);
    }

    /**
     * Sets the version feature of the object passed in.
     *
     * <p>The version value is retreived as an attribute from the node, if <code>null</code>, the
     * default "1.0.0" is used.
     *
     * @param object An object which contains a feature named "version"
     * @param node The parse node.
     */
    public static void version(EObject object, Node node) {
        String version = (String) node.getAttributeValue("version");

        if (version == null) {
            version = "1.0.0";
        }

        set(object, "version", version);
    }

    /**
     * Sets the outputFormat feature of the object passed in.
     *
     * <p>The outputFormat value is retreived as an attribute from the node, if <code>null</code>,
     * the default <code>default</code> is used.
     *
     * @param object An object which contains a feature named "version"
     * @param node The parse node.
     */
    public static void outputFormat(EObject object, Node node, String defalt) {
        String outputFormat = (String) node.getAttributeValue("outputFormat");

        if (outputFormat == null) {
            outputFormat = defalt;
        }

        set(object, "outputFormat", outputFormat);
    }

    public static void set(EObject object, String featureName, Object value) {
        EStructuralFeature feature = object.eClass().getEStructuralFeature(featureName);

        if (feature != null) {
            object.eSet(feature, value);
        }
    }

    /**
     * @param number A number
     * @return The number as a {@link BigInteger}.
     */
    public static BigInteger asBigInteger(Number number) {
        if (number == null) {
            return null;
        }

        if (number instanceof BigInteger) {
            return (BigInteger) number;
        }

        return BigInteger.valueOf(number.longValue());
    }
}
