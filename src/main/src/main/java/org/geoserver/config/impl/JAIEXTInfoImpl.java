/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import it.geosolutions.jaiext.ConcurrentOperationRegistry.OperationItem;
import it.geosolutions.jaiext.JAIExt;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.geoserver.config.JAIEXTInfo;
import org.geotools.image.ImageWorker;

public class JAIEXTInfoImpl implements JAIEXTInfo {

    /** Available JAI operations */
    public static final Set<String> JAI_OPS = new TreeSet<String>();

    private Set<String> jaiOperations = JAI_OPS;

    /** Available JAIEXT operations */
    public static final TreeSet<String> JAIEXT_OPS = new TreeSet<String>();

    private Set<String> jaiExtOperations = JAIEXT_OPS;

    static {
        JAIExt.initJAIEXT(ImageWorker.isJaiExtEnabled());
        populateOperations(JAIEXT_OPS);
    }

    public JAIEXTInfoImpl() {
        if (jaiOperations == null) {
            jaiOperations = JAI_OPS;
        }
        if (jaiExtOperations == null) {
            jaiExtOperations = JAIEXT_OPS;
        }
        if (ImageWorker.isJaiExtEnabled()) {
            populateOperations(jaiExtOperations);
        }
    }

    @Override
    public Set<String> getJAIOperations() {
        if (jaiOperations == null) {
            jaiOperations = JAI_OPS;
        }
        return jaiOperations;
    }

    @Override
    public void setJAIOperations(Set<String> operations) {
        this.jaiOperations = new TreeSet<String>(operations);
    }

    @Override
    public Set<String> getJAIEXTOperations() {
        if (jaiExtOperations == null) {
            jaiExtOperations = JAIEXT_OPS;
        }
        return jaiExtOperations;
    }

    @Override
    public void setJAIEXTOperations(Set<String> operations) {
        this.jaiExtOperations = new TreeSet<String>(operations);
    }

    private static void populateOperations(Set<String> jaiExtOp) {
        List<OperationItem> jaiextOps =
                ImageWorker.isJaiExtEnabled()
                        ? JAIExt.getJAIEXTOperations()
                        : JAIExt.getJAIOperations();
        for (OperationItem item : jaiextOps) {
            String name = item.getName();
            if (name.equalsIgnoreCase("algebric")
                    || name.equalsIgnoreCase("operationConst")
                    || name.equalsIgnoreCase("Stats")
                    || JAIExt.isJAIAPI(name)) {
                jaiExtOp.add(name);
            }
        }
    }
}
