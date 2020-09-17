/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.impl;

import java.io.IOException;
import java.util.*;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;

/** The root of the builders' tree. It triggers the evaluation process */
public class RootBuilder implements TemplateBuilder {

    private List<TemplateBuilder> children;

    private Map<String, String> vendorOptions;

    protected List<String> supportedOptions = new ArrayList<>();

    /** Enum listing available vendor options */
    public enum VendorOption {
        FLAT_OUTPUT("flat_output"),
        SEPARATOR("separator");

        private String vendorOptionName;

        VendorOption(String vendorOptionName) {
            this.vendorOptionName = vendorOptionName;
        }

        public String getVendorOptionName() {
            return vendorOptionName;
        }
    }

    public RootBuilder() {
        super();
        this.children = new ArrayList<TemplateBuilder>(2);
        this.vendorOptions = new HashMap<>();
    }

    public void addChild(TemplateBuilder builder) {
        this.children.add(builder);
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        for (TemplateBuilder jb : children) {
            jb.evaluate(writer, context);
        }
    }

    @Override
    public List<TemplateBuilder> getChildren() {
        return children;
    }

    /**
     * Get the vendor option by name
     *
     * @param optionName the vendor option name
     * @return
     */
    public String getVendorOption(String optionName) {
        return vendorOptions.get(optionName);
    }

    /**
     * Set the vendor option
     *
     * @param vendorOption a string array containing vendor option name and value
     */
    public void setVendorOptions(String[] vendorOption) {
        if (supportVendorOption(vendorOption[0])) {
            vendorOptions.put(vendorOption[0], vendorOption[1]);
        }
    }

    /**
     * Checks if vendor option is supported
     *
     * @param vendorOptionName the name of the vendor option
     * @return
     */
    protected boolean supportVendorOption(String vendorOptionName) {
        if (supportedOptions.contains(vendorOptionName)) return true;
        else return false;
    }
}
