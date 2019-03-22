/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.web.netcdf.layer.NetCDFLayerSettingsContainer;

/**
 * NetCDF output settings. This class stores the global settings that are used to initialise newly
 * published layers. Once layers have been published, their settings are stored in the subclass
 * {@link NetCDFLayerSettingsContainer}.
 */
@SuppressWarnings("serial")
public class NetCDFSettingsContainer implements Serializable {

    public enum Version {
        NETCDF_3,
        NETCDF_4C;
    }

    public static final String NETCDFOUT_KEY = "NetCDFOutput.Key";

    public static final int DEFAULT_COMPRESSION = 0;

    public static final boolean DEFAULT_SHUFFLE = true;

    public static final boolean DEFAULT_COPY_ATTRIBUTES = false;

    public static final boolean DEFAULT_COPY_GLOBAL_ATTRIBUTES = false;

    public static final Version DEFAULT_VERSION = Version.NETCDF_3;

    public static final List<GlobalAttribute> DEFAULT_GLOBAL_ATTRIBUTES =
            new ArrayList<GlobalAttribute>();

    public static final List<VariableAttribute> DEFAULT_VARIABLE_ATTRIBUTES =
            new ArrayList<VariableAttribute>();

    public static final List<ExtraVariable> DEFAULT_EXTRA_VARIABLES =
            new ArrayList<ExtraVariable>();

    private int compressionLevel = DEFAULT_COMPRESSION;

    private boolean shuffle = DEFAULT_SHUFFLE;

    private boolean copyAttributes = DEFAULT_COPY_ATTRIBUTES;

    private boolean copyGlobalAttributes = DEFAULT_COPY_GLOBAL_ATTRIBUTES;

    private DataPacking dataPacking = DataPacking.getDefault();

    private List<GlobalAttribute> globalAttributes = DEFAULT_GLOBAL_ATTRIBUTES;

    private List<VariableAttribute> variableAttributes = DEFAULT_VARIABLE_ATTRIBUTES;

    private List<ExtraVariable> extraVariables = DEFAULT_EXTRA_VARIABLES;

    private MetadataMap metadata = new MetadataMap();

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    public DataPacking getDataPacking() {
        return dataPacking;
    }

    public void setDataPacking(DataPacking dataPacking) {
        this.dataPacking = dataPacking;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    /** Whether to copy attributes from the NetCDF/GRIB source to the main output variable. */
    public boolean isCopyAttributes() {
        return copyAttributes;
    }

    /** Whether to copy global attributes from the NetCDF/GRIB source to the main output. */
    public boolean isCopyGlobalAttributes() {
        return copyGlobalAttributes;
    }

    /** Whether to copy attributes from the NetCDF/GRIB source to the main output variable. */
    public void setCopyAttributes(boolean copyAttributes) {
        this.copyAttributes = copyAttributes;
    }

    /** Whether to copy global attributes from the NetCDF/GRIB source to the main output. */
    public void setCopyGlobalAttributes(boolean copyGlobalAttributes) {
        this.copyGlobalAttributes = copyGlobalAttributes;
    }

    public List<GlobalAttribute> getGlobalAttributes() {
        if (globalAttributes == null) {
            globalAttributes = DEFAULT_GLOBAL_ATTRIBUTES;
        }
        return globalAttributes;
    }

    public void setGlobalAttributes(List<GlobalAttribute> globalAttributes) {
        this.globalAttributes = globalAttributes;
    }

    public List<VariableAttribute> getVariableAttributes() {
        if (variableAttributes == null) {
            variableAttributes = DEFAULT_VARIABLE_ATTRIBUTES;
        }
        return variableAttributes;
    }

    public void setVariableAttributes(List<VariableAttribute> variableAttributes) {
        this.variableAttributes = variableAttributes;
    }

    public List<ExtraVariable> getExtraVariables() {
        if (extraVariables == null) {
            extraVariables = DEFAULT_EXTRA_VARIABLES;
        }
        return extraVariables;
    }

    public void setExtraVariables(List<ExtraVariable> extraVariables) {
        this.extraVariables = extraVariables;
    }

    public MetadataMap getMetadata() {
        if (metadata == null) {
            metadata = new MetadataMap();
        }
        return metadata;
    }

    private abstract static class AbstractAttribute implements Serializable {

        private String key;

        private String value;

        public String getKey() {
            if ((key == null || key.trim().isEmpty())) {
                throw new IllegalArgumentException("Missing attribute key");
            }
            return key.trim();
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return (value == null || value.trim().isEmpty()) ? "" : value.trim();
        }

        public void setValue(String value) {
            this.value = value;
        }

        public AbstractAttribute(String key, String value) {
            this.key = key;
            this.value = value;
        }

        /** @see java.lang.Object#equals(java.lang.Object) */
        @Override
        public boolean equals(Object other) {
            return other instanceof AbstractAttribute
                    && getKey().equals(((AbstractAttribute) other).getKey())
                    && getValue().equals(((AbstractAttribute) other).getValue());
        }

        /** @see java.lang.Object#hashCode() */
        @Override
        public int hashCode() {
            return getKey().hashCode() + getValue().hashCode();
        }
    }

    /** Global attribute to be set in the NetCDF output. */
    public static class GlobalAttribute extends AbstractAttribute {

        public GlobalAttribute(String key, String value) {
            super(key, value);
        }

        /** @see java.lang.Object#equals(java.lang.Object) */
        @Override
        @SuppressWarnings("PMD.OverrideBothEqualsAndHashcode")
        public boolean equals(Object other) {
            return other instanceof GlobalAttribute && super.equals(other);
        }
    }

    /** Attribute to be set on the main variable in the NetCDF output. */
    public static class VariableAttribute extends AbstractAttribute {

        public VariableAttribute(String key, String value) {
            super(key, value);
        }

        /** @see java.lang.Object#equals(java.lang.Object) */
        @Override
        @SuppressWarnings("PMD.OverrideBothEqualsAndHashcode")
        public boolean equals(Object other) {
            return other instanceof VariableAttribute && super.equals(other);
        }
    }

    /** Extra variable that should be copied from NetCDF/GRIB source to output. */
    public static class ExtraVariable implements Serializable {

        /** Name of source variable. */
        private String source;

        /** Name of output variable. */
        private String output;

        /**
         * Whitespace-separated list of output variable dimension names. Empty string to copy a
         * scalar, or a single dimension name like "time" to turn scalar ImageMosaic granules into a
         * vector over that dimensions. More than one dimension not yet supported, but the naming is
         * here as a future extension point.
         */
        private String dimensions;

        /**
         * @param source name of source variable
         * @param output name of output variable
         * @param dimensions whitespace-separated list of output variable dimension names
         */
        public ExtraVariable(String source, String output, String dimensions) {
            this.source = source;
            this.output = output;
            this.dimensions = dimensions;
        }

        public String getSource() {
            if ((source == null || source.trim().isEmpty())
                    && (output == null || output.trim().isEmpty())) {
                throw new IllegalArgumentException(
                        "Neither source nor output supplied for extra variable");
            }
            return (source == null || source.trim().isEmpty()) ? output.trim() : source.trim();
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getOutput() {
            if ((source == null || source.trim().isEmpty())
                    && (output == null || output.trim().isEmpty())) {
                throw new IllegalArgumentException(
                        "Neither source nor output supplied for extra variable");
            }
            return (output == null || output.trim().isEmpty()) ? source.trim() : output.trim();
        }

        public void setOutput(String target) {
            this.output = target;
        }

        public String getDimensions() {
            return dimensions == null ? "" : dimensions.trim();
        }

        public void setDimensions(String dimensions) {
            this.dimensions = dimensions;
        }

        /** @see java.lang.Object#equals(java.lang.Object) */
        @Override
        public boolean equals(Object other) {
            return other instanceof ExtraVariable
                    && getSource().equals(((ExtraVariable) other).getSource())
                    && getOutput().equals(((ExtraVariable) other).getOutput())
                    && getDimensions().equals(((ExtraVariable) other).getDimensions());
        }

        /** @see java.lang.Object#hashCode() */
        @Override
        public int hashCode() {
            return getSource().hashCode() + getOutput().hashCode() + getDimensions().hashCode();
        }
    }
}
