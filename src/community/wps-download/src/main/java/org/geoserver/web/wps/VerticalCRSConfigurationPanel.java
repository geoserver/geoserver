/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wps;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.web.data.resource.ResourceConfigurationPanel;
import org.geoserver.web.wicket.CRSPanel;
import org.geoserver.web.wicket.SRSToCRSModel;
import org.opengis.coverage.SampleDimensionType;

/**
 * A CRS Configuration panel being plugged on Layer's Data config panel when WPS-Download plugin is
 * installed and the underlying gridCoverage could potentially represent vertical data.
 */
public class VerticalCRSConfigurationPanel extends ResourceConfigurationPanel {

    private static final Set<SampleDimensionType> GOOD_CANDIDATES_FOR_VERTICAL_CRS;

    static {
        GOOD_CANDIDATES_FOR_VERTICAL_CRS = new HashSet<SampleDimensionType>();
        GOOD_CANDIDATES_FOR_VERTICAL_CRS.add(SampleDimensionType.REAL_32BITS);
        GOOD_CANDIDATES_FOR_VERTICAL_CRS.add(SampleDimensionType.REAL_64BITS);
        GOOD_CANDIDATES_FOR_VERTICAL_CRS.add(SampleDimensionType.SIGNED_16BITS);
        GOOD_CANDIDATES_FOR_VERTICAL_CRS.add(SampleDimensionType.SIGNED_32BITS);
        GOOD_CANDIDATES_FOR_VERTICAL_CRS.add(SampleDimensionType.UNSIGNED_16BITS);
        GOOD_CANDIDATES_FOR_VERTICAL_CRS.add(SampleDimensionType.UNSIGNED_32BITS);
        // Can DataType <= 8 bit represent valid vertical data?
        // I would say no but if any objections, just add the related
        // SampleDimensionTypes to the above set
    }

    public static final String VERTICAL_CRS_KEY = "VerticalCRS";

    private String verticalCRS;

    public VerticalCRSConfigurationPanel(String panelId, IModel model) {
        super(panelId, model);

        CoverageInfo ci = (CoverageInfo) getResourceInfo();
        List<CoverageDimensionInfo> dimensions = ci.getDimensions();
        boolean isGoodCandidateForVerticalCRS = false;
        if (dimensions != null && dimensions.size() == 1) {
            // We assume that only single band coverage with proper
            // datatype is a good candidate to represent vertical data
            CoverageDimensionInfo dimension = dimensions.get(0);
            SampleDimensionType type = dimension.getDimensionType();
            isGoodCandidateForVerticalCRS = GOOD_CANDIDATES_FOR_VERTICAL_CRS.contains(type);
        }
        final PropertyModel<MetadataMap> metadata =
                new PropertyModel<MetadataMap>(model, "metadata");
        MetadataMap objectMetadata = metadata.getObject();
        if (objectMetadata != null) {
            Serializable verticalCRSvalue = objectMetadata.get(VERTICAL_CRS_KEY);
            if (verticalCRSvalue != null) {
                verticalCRS = verticalCRSvalue.toString();
            }
        }

        CRSPanel verticalCRSPanel =
                new CRSPanel(
                        "verticalCRS", new SRSToCRSModel(new PropertyModel(this, "verticalCRS"))) {
                    protected void onSRSUpdated(String srs, AjaxRequestTarget target) {
                        verticalCRS = srs;
                        if (verticalCRS != null) {

                            final PropertyModel<MetadataMap> metadata =
                                    new PropertyModel<MetadataMap>(model, "metadata");
                            if (metadata.getObject() == null) {
                                metadata.setObject(new MetadataMap());
                            }
                            metadata.getObject().put(VERTICAL_CRS_KEY, verticalCRS);
                        }
                    }
                };
        add(verticalCRSPanel);
        this.setVisible(isGoodCandidateForVerticalCRS);
    }
}
