/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.web.netcdf.layer.NetCDFLayerSettingsContainer;

/**
 * NetCDF output settings. This class stores the global settings that are used to initialise newly published layers.
 * Once layers have been published, their settings are stored in the subclass {@link NetCDFLayerSettingsContainer}.
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

    public static final List<GlobalAttribute> DEFAULT_GLOBAL_ATTRIBUTES = new ArrayList<>();

    public static final List<VariableAttribute> DEFAULT_VARIABLE_ATTRIBUTES = new ArrayList<>();

    public static final List<ExtraVariable> DEFAULT_EXTRA_VARIABLES = new ArrayList<>();

    public static final List<BandSetting> DEFAULT_BAND_SETTINGS = new ArrayList<>();

    private int compressionLevel = DEFAULT_COMPRESSION;

    private boolean shuffle = DEFAULT_SHUFFLE;

    private boolean copyAttributes = DEFAULT_COPY_ATTRIBUTES;

    private boolean copyGlobalAttributes = DEFAULT_COPY_GLOBAL_ATTRIBUTES;

    private DataPacking dataPacking = DataPacking.getDefault();

    private List<GlobalAttribute> globalAttributes = DEFAULT_GLOBAL_ATTRIBUTES;

    private List<VariableAttribute> variableAttributes = DEFAULT_VARIABLE_ATTRIBUTES;

    private List<ExtraVariable> extraVariables = DEFAULT_EXTRA_VARIABLES;

    private List<BandSetting> bandSettings = DEFAULT_BAND_SETTINGS;

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

    /**
     * Per-band output settings. When the source coverage has more than one sample dimension and this list is non-empty,
     * the encoder writes one output variable per band, using the entry at index {@code i} to override the
     * band-{@code i} variable's name, unit of measure, and per-variable attributes (the global
     * {@link #variableAttributes} list still applies to every output variable; per-band attributes are additive and
     * take precedence on key collisions).
     *
     * <p>When this list is empty AND the source coverage is multi-band, the encoder falls back to one output variable
     * per band named after the band's {@link org.geotools.coverage.GridSampleDimension#getDescription()} — which is
     * what a {@code COVERAGE_VIEW} {@code BAND_SELECT} {@code <definition>} field sets — preserving fully
     * backward-compatible single-variable behavior on single-band coverages.
     */
    public List<BandSetting> getBandSettings() {
        if (bandSettings == null) {
            bandSettings = DEFAULT_BAND_SETTINGS;
        }
        return bandSettings;
    }

    public void setBandSettings(List<BandSetting> bandSettings) {
        this.bandSettings = bandSettings;
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
            if (key == null || key.trim().isEmpty()) {
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
            return other instanceof AbstractAttribute aa
                    && getKey().equals(aa.getKey())
                    && getValue().equals(aa.getValue());
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

    /**
     * Per-band output settings. Each entry maps to the source coverage's sample dimension at the same index. Used by
     * the encoder when the source coverage has more than one sample dimension to write a separate output variable per
     * band — see {@link NetCDFSettingsContainer#getBandSettings()} for the activation rules and defaults.
     */
    public static class BandSetting implements Serializable {

        /**
         * Output variable name for this band. When {@code null} or empty, the encoder uses the source band's
         * {@link org.geotools.coverage.GridSampleDimension#getDescription() sample dimension description} — which
         * equals the {@code <definition>} value when the source coverage is a {@code COVERAGE_VIEW} with
         * {@code BAND_SELECT} entries.
         */
        private String name;

        /** Output unit of measure for this band. When {@code null} or empty, the band's source unit is used as-is. */
        private String uom;

        /**
         * Attributes to add to this band's output variable (in addition to the container-level
         * {@link NetCDFSettingsContainer#getVariableAttributes() variableAttributes}). Per-band entries take precedence
         * on key collisions.
         */
        private List<VariableAttribute> variableAttributes;

        public BandSetting() {}

        public BandSetting(String name, String uom, List<VariableAttribute> variableAttributes) {
            this.name = name;
            this.uom = uom;
            this.variableAttributes = variableAttributes;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUom() {
            return uom;
        }

        public void setUom(String uom) {
            this.uom = uom;
        }

        public List<VariableAttribute> getVariableAttributes() {
            if (variableAttributes == null) {
                variableAttributes = new ArrayList<>();
            }
            return variableAttributes;
        }

        public void setVariableAttributes(List<VariableAttribute> variableAttributes) {
            this.variableAttributes = variableAttributes;
        }

        /** @see java.lang.Object#equals(java.lang.Object) */
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof BandSetting bs)) {
                return false;
            }
            return Objects.equals(name, bs.name)
                    && Objects.equals(uom, bs.uom)
                    && getVariableAttributes().equals(bs.getVariableAttributes());
        }

        /** @see java.lang.Object#hashCode() */
        @Override
        public int hashCode() {
            return Objects.hash(name, uom, getVariableAttributes());
        }
    }

    /** Extra variable that should be copied from NetCDF/GRIB source to output. */
    public static class ExtraVariable implements Serializable {

        /** Name of source variable. */
        private String source;

        /** Name of output variable. */
        private String output;

        /**
         * Whitespace-separated list of output variable dimension names. Empty string to copy a scalar, or a single
         * dimension name like "time" to turn scalar ImageMosaic granules into a vector over that dimensions. More than
         * one dimension not yet supported, but the naming is here as a future extension point.
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
                throw new IllegalArgumentException("Neither source nor output supplied for extra variable");
            }
            return (source == null || source.trim().isEmpty()) ? output.trim() : source.trim();
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getOutput() {
            if ((source == null || source.trim().isEmpty())
                    && (output == null || output.trim().isEmpty())) {
                throw new IllegalArgumentException("Neither source nor output supplied for extra variable");
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
            return other instanceof ExtraVariable ev
                    && getSource().equals(ev.getSource())
                    && getOutput().equals(ev.getOutput())
                    && getDimensions().equals(ev.getDimensions());
        }

        /** @see java.lang.Object#hashCode() */
        @Override
        public int hashCode() {
            return getSource().hashCode()
                    + getOutput().hashCode()
                    + getDimensions().hashCode();
        }
    }
}
