/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.util.Utilities;

/**
 * Class containing main definition of a Coverage View, such as, originating coverageStore and
 * composing coverageNames/bands.
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class CoverageView implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 5504720319141832424L;

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder("CoverageView name=").append(name);
        output.append("\n\tBands");
        for (CoverageBand band : coverageBands) {
            output.append("\t").append(band.toString());
        }
        return output.toString();
    }

    public static final String BAND_SEPARATOR = "@";

    /** Type of Envelope Composition, used to expose the bounding box of the CoverageView */
    public static enum EnvelopeCompositionType {
        UNION,
        INTERSECTION;
    }

    /** Which Resolution to be used in composition */
    public static enum SelectedResolution {
        BEST,
        WORST;
    }

    /**
     * Composition Type, used to specify how output bands should be composed.
     *
     * <p>BAND_SELECT: The output band is simply a band selected from the input bands. FORMULA: The
     * output band is computed by applying a formula on the input selected band(s). As an instance,
     * the output band could be defined like this: Speed = SQRT(SELECTED_BAND_1^2 +
     * SELECTED_BAND_2^2)
     */
    public static enum CompositionType {

        // Some pieces here have been commented. They should be uncommented once
        // we enable complex output bands definition using formula.
        BAND_SELECT {
            @Override
            public String displayValue() {
                return BAND_SELECTION_STRING;
            }
        } /*,

          FORMULA {
              @Override
              public String displayValue() {
                  return FORMULA_STRING;
              }

          }
          */;

        public static CompositionType getDefault() {
            return BAND_SELECT;
        }

        public abstract String displayValue();

        public String toValue() {
            return this.toString();
        }

        static final String BAND_SELECTION_STRING = "Band Selection";
        /* final static String FORMULA_STRING = "Formula"; */
    }

    /**
     * Definition of Input Coverage Bands composing a single {@link CoverageBand} A {@link
     * CoverageBand} may be composed of different {@link InputCoverageBand}s.
     *
     * <p>Current implementation only deal with {@link CoverageBand}s made of a single {@link
     * InputCoverageBand}. Once we allows for Scripts and Math on bands compositions (like
     * WindSpeedBand = SQRT(UBand^2 + VBand^2)) we will have a {@link CoverageBand} built on top of
     * multiple {@link InputCoverageBand}s
     */
    public static class InputCoverageBand implements Serializable {

        public InputCoverageBand() {}

        /** serialVersionUID */
        private static final long serialVersionUID = -2200641260788001394L;

        @Override
        public String toString() {
            return "InputCoverageBand [coverageName=" + coverageName + ", band=" + band + "]";
        }

        public InputCoverageBand(String coverageName, String band) {
            this.coverageName = coverageName;
            this.band = band;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((band == null) ? 0 : band.hashCode());
            result = prime * result + ((coverageName == null) ? 0 : coverageName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            InputCoverageBand other = (InputCoverageBand) obj;
            if (band == null) {
                if (other.band != null) return false;
            } else if (!band.equals(other.band)) return false;
            if (coverageName == null) {
                if (other.coverageName != null) return false;
            } else if (!coverageName.equals(other.coverageName)) return false;
            return true;
        }

        public String getCoverageName() {
            return coverageName;
        }

        public void setCoverageName(String coverageName) {
            this.coverageName = coverageName;
        }

        public String getBand() {
            return band;
        }

        public void setBand(String band) {
            this.band = band;
        }

        /** The name of the input coverage from which this band has been extracted. */
        private String coverageName;

        /** Currently, we store here the index of the band in the input coverage. */
        private String band;
    }

    /**
     * Definition of a {@link CoverageView}'s Band composing the {@link CoverageView}. A {@link
     * CoverageBand} is made of
     *
     * <ul>
     *   <li>a list of {@link InputCoverageBand}s defining which coverages and which bands have been
     *       used to compose this band
     *   <li>the type of composition used to configure this band (Currently, only BAND_SELECT is
     *       supported)
     *   <li>the definition of this band (It may contain the script, or the RULE to compose that
     *       band)
     *   <li>the index in the output coverage (Wondering if this can be removed)
     * </ul>
     */
    public static class CoverageBand implements Serializable {

        /** serialVersionUID */
        private static final long serialVersionUID = -7223081117287911988L;

        public CoverageBand() {}

        public CoverageBand(
                List<InputCoverageBand> inputCoverageBands,
                String definition,
                int index,
                CompositionType compositionType) {
            this.inputCoverageBands = inputCoverageBands;
            this.definition = definition;
            this.index = index;
            this.compositionType = compositionType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((compositionType == null) ? 0 : compositionType.hashCode());
            result = prime * result + ((definition == null) ? 0 : definition.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            CoverageBand other = (CoverageBand) obj;
            if (compositionType != other.compositionType) return false;
            if (definition == null) {
                if (other.definition != null) return false;
            } else if (!definition.equals(other.definition)) return false;
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("OutputBand\n   inputCoverageBands=");
            for (InputCoverageBand inputBand : inputCoverageBands) {
                sb.append("[").append(inputBand).append("]");
            }
            sb.append(", definition=")
                    .append(definition)
                    .append(", index=")
                    .append(index)
                    .append(", compositionType=")
                    .append(compositionType);
            return sb.toString();
        }

        /** The InputCoverageBands composing this band */
        private List<InputCoverageBand> inputCoverageBands;

        /**
         * The definition of this coverage band. Currently it simply contains the name of the input
         * band. Once we support different compositions, it will contain the maths... something
         * like, as an instance, speed = sqrt(u_component_of_the_wind@0 ^ 2 +
         * v_component_of_the_wind@0^2).
         */
        private String definition;

        /** The index of the band in the output. (Is it really needed?) */
        private int index;

        /**
         * Type of composition used to define this band. Currently, only {@link
         * CompositionType#BAND_SELECT} is supported.
         */
        private CompositionType compositionType;

        public String getDefinition() {
            return definition;
        }

        public void setDefinition(String definition) {
            this.definition = definition;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public CompositionType getCompositionType() {
            return compositionType;
        }

        public void setCompositionType(CompositionType compositionType) {
            this.compositionType = compositionType;
        }

        public List<InputCoverageBand> getInputCoverageBands() {
            return inputCoverageBands;
        }

        public void setInputCoverageBands(List<InputCoverageBand> inputCoverageBands) {
            this.inputCoverageBands = inputCoverageBands;
        }
    }

    /** A key to be assigned to the {@link CoverageView} object into metadata map */
    public static String COVERAGE_VIEW = "COVERAGE_VIEW";

    public CoverageView() {}

    public CoverageView(String name, List<CoverageBand> coverageBands) {
        this(name, coverageBands, EnvelopeCompositionType.UNION, SelectedResolution.BEST);
    }

    public CoverageView(
            String name,
            List<CoverageBand> coverageBands,
            EnvelopeCompositionType envelopeCompositionType,
            SelectedResolution selectedResolution) {
        this.name = name;
        this.coverageBands = coverageBands;
        this.envelopeCompositionType = envelopeCompositionType;
        this.selectedResolution = selectedResolution;
    }

    /** The list of {@link CoverageBand}s composing this {@link CoverageView} */
    private List<CoverageBand> coverageBands;

    /** The name assigned to the {@link CoverageView} */
    private String name;

    /** Type of composition of the envelope. */
    private EnvelopeCompositionType envelopeCompositionType;

    /** Requested resolution type (worst vs best vs imposed vs index) */
    private SelectedResolution selectedResolution;

    /** This will be != -1 when {@link SelectedResolution} is INDEX */
    private int selectedResolutionIndex = -1;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EnvelopeCompositionType getEnvelopeCompositionType() {
        // for backwards compatibility
        return envelopeCompositionType == null
                ? EnvelopeCompositionType.INTERSECTION
                : envelopeCompositionType;
    }

    public void setEnvelopeCompositionType(EnvelopeCompositionType envelopeCompositionType) {
        this.envelopeCompositionType = envelopeCompositionType;
    }

    public SelectedResolution getSelectedResolution() {
        // for backwards compatibility
        return selectedResolution == null ? SelectedResolution.BEST : selectedResolution;
    }

    public void setSelectedResolution(SelectedResolution selectedResolution) {
        this.selectedResolution = selectedResolution;
    }

    public int getSelectedResolutionIndex() {
        return selectedResolutionIndex;
    }

    public void setSelectedResolutionIndex(int selectedResolutionIndex) {
        this.selectedResolutionIndex = selectedResolutionIndex;
    }

    public List<CoverageBand> getCoverageBands() {
        return coverageBands;
    }

    public void setCoverageBands(List<CoverageBand> coverageBands) {
        this.coverageBands = coverageBands;
    }

    /** Create a {@link CoverageInfo} */
    private CoverageInfo buildCoverageInfo(
            CatalogBuilder builder, CoverageStoreInfo storeInfo, CoverageInfo cinfo, String name)
            throws Exception {
        Catalog catalog = storeInfo.getCatalog();

        // Get a reader from the pool for this Sample CoverageInfo
        // (we have to pass it down a CoverageView definition)
        cinfo.setStore(storeInfo);
        cinfo.getMetadata().put(CoverageView.COVERAGE_VIEW, this);
        cinfo.setName(name);
        cinfo.setNativeCoverageName(name);

        GridCoverage2DReader reader =
                (GridCoverage2DReader)
                        catalog.getResourcePool().getGridCoverageReader(cinfo, name, null);
        builder.setStore(storeInfo);
        return builder.buildCoverage(reader, name, null);
    }

    /** Create a new {@link CoverageInfo} for this {@link CoverageView} */
    public CoverageInfo createCoverageInfo(
            String name, CoverageStoreInfo storeInfo, CatalogBuilder builder) throws Exception {
        Catalog catalog = storeInfo.getCatalog();

        CoverageInfo coverageInfo = catalog.getFactory().createCoverage();
        CoverageInfo info = buildCoverageInfo(builder, storeInfo, coverageInfo, name);

        info.getMetadata().put(CoverageView.COVERAGE_VIEW, this);
        info.setName(name);
        info.setNativeCoverageName(name);
        return info;
    }

    /**
     * Update the specified {@link CoverageInfo} with the updated {@link CoverageView} stored within
     * its metadata
     */
    public void updateCoverageInfo(
            String name,
            CoverageStoreInfo storeInfo,
            CatalogBuilder builder,
            CoverageInfo coverageInfo)
            throws Exception {
        Utilities.ensureNonNull("coverageInfo", coverageInfo);

        // clean up coverage dimensions for the update
        coverageInfo.getDimensions().clear();
        CoverageInfo info = buildCoverageInfo(builder, storeInfo, coverageInfo, name);
        coverageInfo.getMetadata().put(CoverageView.COVERAGE_VIEW, this);
        coverageInfo.getDimensions().addAll(info.getDimensions());
    }

    /** Get the i-th {@link CoverageBand} */
    public CoverageBand getBand(final int index) {
        return coverageBands.get(index);
    }

    /** Get the {@link CoverageBand}s related to the specified coverageName */
    public List<CoverageBand> getBands(final String coverageName) {
        List<CoverageBand> bands = new ArrayList<CoverageBand>();
        for (CoverageBand coverageBand : coverageBands) {
            for (InputCoverageBand inputBand : coverageBand.getInputCoverageBands()) {
                if (inputBand.getCoverageName().equalsIgnoreCase(coverageName)) {
                    bands.add(coverageBand);
                }
            }
        }
        return bands;
    }

    /** Return the number of {@link CoverageBand}s composing the {@link CoverageView} */
    public int getSize() {
        return coverageBands != null ? coverageBands.size() : 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((coverageBands == null) ? 0 : coverageBands.hashCode());
        result =
                prime * result
                        + ((envelopeCompositionType == null)
                                ? 0
                                : envelopeCompositionType.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result =
                prime * result + ((selectedResolution == null) ? 0 : selectedResolution.hashCode());
        result = prime * result + selectedResolutionIndex;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        CoverageView other = (CoverageView) obj;
        if (coverageBands == null) {
            if (other.coverageBands != null) return false;
        } else if (!coverageBands.equals(other.coverageBands)) return false;
        if (envelopeCompositionType != other.envelopeCompositionType) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (selectedResolution != other.selectedResolution) return false;
        if (selectedResolutionIndex != other.selectedResolutionIndex) return false;
        return true;
    }
}
