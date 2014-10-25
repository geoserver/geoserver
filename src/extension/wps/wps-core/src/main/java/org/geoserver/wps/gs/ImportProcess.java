/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Imports a feature collection into the GeoServer catalog
 * 
 * @author Andrea Aime - GeoSolutions
 * @author Alessio Fabiani - GeoSolutions
 * 
 */
@DescribeProcess(title = "Import to Catalog", description = "Imports a feature collection into the catalog")
public class ImportProcess implements GSProcess {

    static final Logger LOGGER = Logging.getLogger(ImportProcess.class);

    public Catalog catalog;

    public ImportProcess(Catalog catalog) {
        this.catalog = catalog;
    }

    @DescribeResult(name = "layerName", description = "Name of the new featuretype, with workspace")
    public String execute(
            @DescribeParameter(name = "features", min = 0, description = "Input feature collection") SimpleFeatureCollection features,
            @DescribeParameter(name = "coverage", min = 0, description = "Input raster") GridCoverage2D coverage,
            @DescribeParameter(name = "workspace", min = 0, description = "Target workspace (default is the system default)") String workspace,
            @DescribeParameter(name = "store", min = 0, description = "Target store (default is the workspace default)") String store,
            @DescribeParameter(name = "name", min = 0, description = "Name of the new featuretype/coverage (default is the name of the features in the collection)") String name,
            @DescribeParameter(name = "srs", min = 0, description = "Target coordinate reference system (default is based on source when possible)") CoordinateReferenceSystem srs,
            @DescribeParameter(name = "srsHandling", min = 0, description = "Desired SRS handling (default is FORCE_DECLARED, others are REPROJECT_TO_DECLARED or NONE)") ProjectionPolicy srsHandling,
            @DescribeParameter(name = "styleName", min = 0, description = "Name of the style to be associated with the layer (default is a standard geometry-specific style)") String styleName)
            throws ProcessException {

        // first off, decide what is the target store
        WorkspaceInfo ws;
        if (workspace != null) {
            ws = this.catalog.getWorkspaceByName(workspace);
            if (ws == null) {
                throw new ProcessException("Could not find workspace " + workspace);
            }
        } else {
            ws = this.catalog.getDefaultWorkspace();
            if (ws == null) {
                throw new ProcessException(
                        "The catalog is empty, could not find a default workspace");
            }
        }

        // create a builder to help build catalog objects
        CatalogBuilder cb = new CatalogBuilder(this.catalog);
        cb.setWorkspace(ws);

        // ok, find the target store
        StoreInfo storeInfo = null;
        if (store != null) {
            if (features != null) {
                storeInfo = this.catalog.getDataStoreByName(ws.getName(), store);
            } else if (coverage != null) {
                storeInfo = this.catalog.getCoverageStoreByName(ws.getName(), store);
            }
            if (storeInfo == null) {
                throw new ProcessException("Could not find store " + store + " in workspace "
                        + workspace);
                // TODO: support store creation
            }
        } else if (features != null) {
            storeInfo = this.catalog.getDefaultDataStore(ws);
            if (storeInfo == null) {
                throw new ProcessException("Could not find a default store in workspace "
                        + ws.getName());
            }
        } else if (coverage != null) {
            // create a new coverage store
            LOGGER.info("Auto-configuring coverage store: " + (name != null ? name : coverage.getName().toString()));

            storeInfo = cb.buildCoverageStore((name != null ? name : coverage.getName().toString()));
            store = (name != null ? name : coverage.getName().toString());

            if (storeInfo == null) {
                throw new ProcessException("Could not find a default store in workspace " + ws.getName());
            }
        }

        // check the target style if any
        StyleInfo targetStyle = null;
        if (styleName != null) {
            targetStyle = this.catalog.getStyleByName(styleName);
            if (targetStyle == null) {
                throw new ProcessException("Could not find style " + styleName);
            }
        }

        if (features != null) {
            return new FeaturesImporter(this.catalog).execute(features, name, cb, ws, storeInfo, srs, srsHandling, targetStyle);
        } else if (coverage != null) {
            return new CoverageImporter(this.catalog).execute(coverage, name, cb, ws, storeInfo, srs, srsHandling, targetStyle);
        }

        return null;
    }

}
