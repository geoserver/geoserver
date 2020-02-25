/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapCallback;
import org.geoserver.wms.GetMapCallbackAdapter;
import org.geoserver.wms.WMSMapContent;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.gce.imagemosaic.MergeBehavior;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.Layer;
import org.geotools.util.logging.Logging;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.ReferenceIdentifier;

/**
 * Convenience base class for writing {@link GetMapCallback} that are only interested in a small
 * subset of the supported events.
 *
 * @author Andrea Aime - GeoSolutions
 * @author Simone Giannecchini, GeoSolutions
 */
public class EOGetMapCallback extends GetMapCallbackAdapter implements GetMapCallback {

    private static final Logger LOGGER = Logging.getLogger(EOGetMapCallback.class);

    private final Catalog catalog;

    public EOGetMapCallback(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Layer beforeLayer(WMSMapContent content, Layer layer) {

        // === check in catalog if it's an EO BAND Layer. If not return untouched
        final CoverageInfo cinfo = catalogChecks(layer);
        if (cinfo == null) {
            throw new IllegalStateException(
                    "Layer " + layer.getTitle() + " does nto resolve to a coverage");
        }
        // extract incoming reader
        if (!(layer instanceof GridReaderLayer)) {
            throw new IllegalStateException(
                    "Layer " + layer.getTitle() + " does nto resolve to a coverage");
        }
        final GridReaderLayer gridReaderLayer = (GridReaderLayer) layer;
        final GridCoverage2DReader reader = gridReaderLayer.getReader();

        // === assuming now it is an EO BAND layer we must have either 1 or 3 values for the
        // additional domain

        // enforce one dimension
        Set<ParameterDescriptor<List>> dimensions;
        try {
            dimensions = reader.getDynamicParameters();

            if (dimensions.size() != 1) {
                throw new IllegalStateException(
                        "Coverage "
                                + cinfo.getName()
                                + " has a number of dimensions different than 1");
            }
            // extract curent values and enforce 1 or 3 values
            final GeneralParameterValue[] params = gridReaderLayer.getParams();
            enforceParamCardinality(params, dimensions);

            // enforce stacking order
            enforceStackingOrder(params);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        return layer;
    }

    /** */
    private void enforceStackingOrder(GeneralParameterValue[] params) {

        // look for it
        for (GeneralParameterValue p : params) {
            if (p.getDescriptor().getName().equals(ImageMosaicFormat.MERGE_BEHAVIOR.getName())) {
                // found it, enfoce cardinality
                ((ParameterValue) p).setValue(MergeBehavior.STACK.name());
                break;
            }
        }
    }

    /** */
    private void enforceParamCardinality(
            GeneralParameterValue[] params, Set<ParameterDescriptor<List>> dimensions) {

        // get parameter name
        final ParameterDescriptor<List> param = dimensions.iterator().next();
        final ReferenceIdentifier paramName = param.getName();

        // look for it
        for (GeneralParameterValue p : params) {
            if (p.getDescriptor().getName().equals(paramName)) {
                // found it, enfoce cardinality
                List value = (List) ((ParameterValue) p).getValue();
                if (value.size() != 1 && value.size() != 3) {
                    throw new IllegalStateException();
                }
                break;
            }
        }
    }

    /** @param layer */
    private CoverageInfo catalogChecks(Layer layer) {
        return catalog.getCoverageByName(layer.getTitle());
    }
}
