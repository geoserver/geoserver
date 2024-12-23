/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rat.rest;

import static java.lang.String.format;

import it.geosolutions.imageio.pam.PAMDataset;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.rat.CoverageRATs;
import org.geoserver.rat.RasterAttributeTable;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.catalog.AbstractCatalogController;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.style.Style;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.PAMResourceInfo;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@ControllerAdvice
@RequestMapping(
        path = {
            RestBaseController.ROOT_PATH + "/layers",
            RestBaseController.ROOT_PATH
                    + "/workspaces/{workspaceName}/coveragestores/{storeName}/coverages/{coverageName}/pam"
        })
public class PAMController extends AbstractCatalogController {

    static final Logger LOGGER = Logging.getLogger(PAMController.class);

    public PAMController(Catalog catalog) {
        super(catalog);
    }

    @GetMapping(produces = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    public String getRasterAttributeTable(
            @PathVariable String workspaceName, @PathVariable String storeName, @PathVariable String coverageName)
            throws Exception {
        CoverageRATs ratSupport = getRATSupport(workspaceName, storeName, coverageName);

        return ratSupport.toXML();
    }

    private CoverageRATs getRATSupport(String workspaceName, String storeName, String coverageName) {
        CoverageInfo coverage = getCoverageInfo(workspaceName, storeName, coverageName);

        CoverageRATs rats = new CoverageRATs(catalog, coverage);
        if (rats.getPAMDataset() == null) {
            throw new ResourceNotFoundException(
                    format("No PAMDataset found for coverage: '%s:%s'", workspaceName, coverageName));
        }
        return rats;
    }

    private CoverageInfo getCoverageInfo(String workspaceName, String storeName, String coverageName) {
        WorkspaceInfo wsInfo = catalog.getWorkspaceByName(workspaceName);
        if (wsInfo == null) {
            // could not find the namespace associated with the desired workspace
            throw new ResourceNotFoundException(format("Workspace not found: '%s'.", workspaceName));
        }
        CoverageStoreInfo storeInfo = catalog.getCoverageStoreByName(workspaceName, storeName);
        if (storeInfo == null) {
            throw new ResourceNotFoundException(format("No such coverage store: '%s:%s'", workspaceName, storeName));
        }
        CoverageInfo coverage = catalog.getCoverageByName(workspaceName, coverageName);
        if (coverage == null) {
            throw new ResourceNotFoundException(format("No such coverage: '%s:%s'", workspaceName, coverageName));
        }
        if (!storeInfo.equals(coverage.getStore())) {
            throw new ResourceNotFoundException(
                    format("No such coverage: '%s' in store '%s'", coverageName, storeName));
        }
        return coverage;
    }

    @PostMapping
    public ResponseEntity create(
            @PathVariable String workspaceName,
            @PathVariable String storeName,
            @PathVariable String coverageName,
            @RequestParam int band,
            @RequestParam String classification,
            @RequestParam(required = false) String styleName,
            UriComponentsBuilder uris)
            throws IOException {
        CoverageRATs ratSupport = getRATSupport(workspaceName, storeName, coverageName);
        List<PAMDataset.PAMRasterBand> bands = ratSupport.getPAMDataset().getPAMRasterBand();
        if (band < 0 || band >= bands.size()) {
            throw new RestException(
                    format("Band index %d out of range for coverage '%s:%s'", band, workspaceName, coverageName),
                    HttpStatus.BAD_REQUEST);
        }
        RasterAttributeTable rat = ratSupport.getRasterAttributeTable(band);
        if (rat == null) {
            throw new RestException(
                    format(
                            "No Raster Attribute Table found for coverage '%s:%s' on band %d",
                            workspaceName, coverageName, band),
                    HttpStatus.BAD_REQUEST);
        }
        Set<String> classifications = rat.getClassifications();
        if (!classifications.contains(classification)) {
            throw new RestException(
                    format("Raster attribute table found, but has no classification field named: '%s'", classification),
                    HttpStatus.BAD_REQUEST);
        }

        Style style = rat.classify(classification);

        if (styleName == null) {
            styleName = ratSupport.getDefaultStyleName(band, classification);
        }
        boolean exists = ratSupport.getCoverageStyle(styleName) != null;
        StyleInfo si = ratSupport.saveStyle(style, styleName);

        LayerInfo layer = catalog.getLayerByName(workspaceName + ":" + coverageName);
        Set<StyleInfo> styles = layer.getStyles();
        if (!styles.contains(si)) {
            layer.getStyles().add(si);
            catalog.save(layer);
            LOGGER.info("Created style " + si.prefixedName());
        }

        // prepare the response
        UriComponents uriComponents =
                uris.path("/workspaces/{workspaceName}/styles/{styleName}").buildAndExpand(workspaceName, styleName);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>("", headers, exists ? HttpStatus.SEE_OTHER : HttpStatus.CREATED);
    }

    @PostMapping(path = "/reload")
    public void reload(
            @PathVariable String workspaceName, @PathVariable String storeName, @PathVariable String coverageName)
            throws IOException {
        CoverageInfo ci = getCoverageInfo(workspaceName, storeName, coverageName);
        GridCoverage2DReader reader = (GridCoverage2DReader) ci.getStore().getGridCoverageReader(null, null);
        ResourceInfo resourceInfo = reader.getInfo(ci.getNativeCoverageName());
        if (!(resourceInfo instanceof PAMResourceInfo)) {
            throw new RestException(
                    format("No Raster Attribute Table found for coverage '%s:%s'", workspaceName, coverageName),
                    HttpStatus.BAD_REQUEST);
        }
        PAMResourceInfo pamInfo = (PAMResourceInfo) resourceInfo;
        // if the reload does not work, a clear should help (e.g. with GeoTIFF)
        // as the PAM is normally cached in memory. The mosaic is the odd case that
        // needs the full reload, as it caches the summary PAM from all its sources
        if (!pamInfo.reloadPAMDataset()) {
            catalog.getResourcePool().clear(ci);
        }
    }
}
