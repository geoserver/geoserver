/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.impl;

import static org.geoserver.featurestemplating.builders.impl.RootBuilder.VendorOption.FLAT_OUTPUT;

import java.io.IOException;
import java.util.*;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.flat.FlatBuilder;
import org.geoserver.featurestemplating.expressions.TemplateCQLManager;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;

/** The root of the builders' tree. It triggers the evaluation process */
public class RootBuilder implements TemplateBuilder {

    private List<TemplateBuilder> children;

    private Map<String, String> vendorOptions;

    private Map<String, Object> encodingHints;

    protected List<String> supportedOptions = new ArrayList<>();

    private boolean semanticValidation;

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

    @Override
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
        String optionValue = vendorOptions.get(optionName);
        if (optionValue != null && optionValue.startsWith("$${")) {
            TemplateCQLManager cqlManager = new TemplateCQLManager(optionValue, null);
            return cqlManager.getExpressionFromString().evaluate(null).toString();
        }
        return optionValue;
    }

    /**
     * Set the vendor option
     *
     * @param vendorOption a string array containing vendor option name and value
     */
    public void setVendorOptions(String[] vendorOption) {
        vendorOptions.put(vendorOption[0], vendorOption[1]);
    }

    public void addVendorOption(String name, String value) {
        vendorOptions.put(name, value);
    }

    public void addVendorOptions(Map<String, String> vendorOptions) {
        vendorOptions.putAll(vendorOptions);
    }

    public boolean needsReload() {
        TemplateBuilder aChild = getChildren().get(0);
        boolean isCachedFlattened = aChild instanceof FlatBuilder;
        String strFlat = getVendorOption(FLAT_OUTPUT.getVendorOptionName());
        boolean isFlatOutput = strFlat != null ? Boolean.valueOf(strFlat) : false;
        if (isCachedFlattened && !isFlatOutput) return true;
        else if (!isCachedFlattened && isFlatOutput) return true;
        else return false;
    }

    @Override
    public void addEncodingHint(String key, Object value) {
        if (encodingHints == null) this.encodingHints = new HashMap<>();
        encodingHints.put(key, value);
    }

    @Override
    public Map<String, Object> getEncodingHints() {
        if (encodingHints == null) encodingHints = new HashMap<>();
        return encodingHints;
    }

    public boolean isSemanticValidation() {
        return semanticValidation;
    }

    public void setSemanticValidation(boolean semanticValidation) {
        this.semanticValidation = semanticValidation;
    }
}
