/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.changeset;

import static org.geoserver.api.changeset.ChangesetIndexProvider.INITIAL_STATE;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.geoserver.api.APIBBoxParser;
import org.geoserver.api.APIDispatcher;
import org.geoserver.api.APIException;
import org.geoserver.api.APIService;
import org.geoserver.api.InvalidParameterValueException;
import org.geoserver.api.ResourceNotFoundException;
import org.geoserver.api.tiles.TilesService;
import org.geoserver.api.tiles.TilesServiceInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.wms.capabilities.CapabilityUtil;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.NumberRange;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** Extension to the Tiles service allowing to retrieve changesets */
@APIService(
    service = "Tiles",
    version = "1.0",
    landingPage = "ogc/tiles",
    serviceClass = TilesServiceInfo.class
)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/tiles")
public class ChangesetTilesService {

    public static final String CHANGESET_MIME = "application/changeset+json";
    public static final String ZIP_MIME = "application/x-zip-compressed";
    public static final String GET_RENDERED_COLLECTION_TILES = "getRenderedCollectionTiles";
    private final ChangesetIndexProvider indexProvider;
    private final Catalog catalog;
    private final GWC gwc;

    public enum ChangeSetType {
        summary("summary"),
        pack("package");
        String name;

        ChangeSetType(String name) {
            this.name = name;
        }

        public static ChangeSetType fromName(String changeSetType) {
            if (changeSetType == null) {
                return null;
            }

            for (ChangeSetType value : values()) {
                if (changeSetType.equals(value.name)) {
                    return value;
                }
            }
            // not sure what happens with the normal converter, might result in a 500 instead of a
            // 400, no time to verify and eventually amend
            throw new APIException(
                    "IllegalParameterValue",
                    "Could not find a changeset type named " + changeSetType,
                    HttpStatus.BAD_REQUEST);
        }
    }

    public ChangesetTilesService(GWC gwc, ChangesetIndexProvider indexProvider, Catalog catalog) {
        this.gwc = gwc;
        this.indexProvider = indexProvider;
        this.catalog = catalog;
    }

    @GetMapping(
        path = "/collections/{collectionId}/map/{styleId}/tiles/{tileMatrixSetId}",
        name = GET_RENDERED_COLLECTION_TILES,
        produces = {CHANGESET_MIME, ZIP_MIME}
    )
    @ResponseBody
    public Object getMultiTiles(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "styleId") String styleId,
            @PathVariable(name = "tileMatrixSetId") String tileMatrixSetId,
            @RequestParam(name = "scaleDenominator", required = false) String scaleDenominatorSpec,
            @RequestParam(name = "bbox", required = false) String bboxSpec,
            @RequestParam(name = "f-tile", required = false) String tileFormatSpec,
            @RequestParam(name = "checkPoint", required = false, defaultValue = INITIAL_STATE)
                    String checkpoint,
            @RequestParam(name = "changeSetType", required = false) String changeSetTypeName)
            throws GeoWebCacheException, IOException, NoSuchAlgorithmException, FactoryException {
        ChangeSetType changeSetType = ChangeSetType.fromName(changeSetTypeName);
        Filter spatialFilter = APIBBoxParser.toFilter(bboxSpec);
        // collection must be a structured coverage and a tile layer at the same time
        CoverageInfo ci = getStructuredCoverageInfo(collectionId, true);
        TileLayer tileLayer = getTileLayer(collectionId);
        TilesService.validateStyle(tileLayer, styleId);
        GridSubset layerGridSubset = TilesService.getGridSubset(tileLayer, tileMatrixSetId);
        List<MediaType> requestedTileFormats = Collections.emptyList();
        if (tileFormatSpec != null) {
            requestedTileFormats =
                    Collections.singletonList(MediaType.parseMediaType(tileFormatSpec));
        }
        MimeType tileFormat =
                TilesService.getRequestedFormat(tileLayer, true, requestedTileFormats);

        // get (and check) the style too
        StyleInfo style = getStyle(styleId);
        NumberRange<Double> styleScaleRange =
                CapabilityUtil.searchMinMaxScaleDenominator(Collections.singleton(style));
        NumberRange<Double> scaleRange = styleScaleRange;

        // now we can check the eventual scale denominators in the request
        if (scaleDenominatorSpec != null && !scaleDenominatorSpec.trim().isEmpty()) {
            NumberRange<Double> requestedScaleRange = parseScaleDenominator(scaleDenominatorSpec);
            scaleRange = (NumberRange<Double>) styleScaleRange.intersect(requestedScaleRange);
        }

        // finally check with the tile matrix scale ranges, the result of the intersection
        // might not contain any zoom level
        if (scaleRange == null
                || scaleRange.isEmpty()
                || !rangeHitsGridset(layerGridSubset, scaleRange)) {
            throw new APIException(
                    "NoChanges", "No changes occurred since checkpoint", HttpStatus.NOT_MODIFIED);
        }

        // get the changed areas
        SimpleFeatureCollection areas =
                indexProvider.getModifiedAreas(ci, checkpoint, spatialFilter);
        if ((areas == null || areas.isEmpty())) {
            throw new APIException(
                    "NoChanges", "No changes occurred since checkpoint", HttpStatus.NOT_MODIFIED);
        }

        // compute the changed bboxes
        List<ReferencedEnvelope> extentOfChangedItems = new ArrayList<>();
        try (SimpleFeatureIterator fi = areas.features()) {
            while (fi.hasNext()) {
                // TODO: if they are multipolygons, would make sense to split them
                Envelope envelope =
                        ((Geometry) fi.next().getDefaultGeometry()).getEnvelopeInternal();
                ReferencedEnvelope re =
                        new ReferencedEnvelope(
                                envelope, areas.getSchema().getCoordinateReferenceSystem());
                CoordinateReferenceSystem gridsetCRS =
                        CRS.decode("EPSG:" + layerGridSubset.getSRS().getNumber(), true);
                try {
                    // TODO: might want to use the projection handler to avoid impossible
                    // reprojections
                    ReferencedEnvelope boundsInGridsetCRS = re.transform(gridsetCRS, true);
                    extentOfChangedItems.add(boundsInGridsetCRS);
                } catch (TransformException e) {
                    throw new APIException(
                            "InternalError",
                            "Failed to reproject extent of changed items to gridset crs",
                            HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }

        ModifiedTiles modifiedTiles =
                new ModifiedTiles(
                        ci,
                        tileLayer,
                        tileLayer.getGridSubset(tileMatrixSetId),
                        areas,
                        APIBBoxParser.parse(bboxSpec),
                        scaleRange);
        ChangeSet changeSet =
                new ChangeSet(
                        checkpoint,
                        extentOfChangedItems,
                        modifiedTiles,
                        tileFormat,
                        Collections.singletonMap("styles", styleId));
        changeSet.setScaleOfChangedItems(styleScaleRange);
        return changeSet;
    }

    /** Checks if the the scale range actually hits a scale denominator in the gridset, */
    private boolean rangeHitsGridset(GridSubset gridSubset, NumberRange<Double> scaleRange) {
        GridSet gridSet = gridSubset.getGridSet();
        for (int z = gridSubset.getZoomStart(); z <= gridSubset.getZoomStop(); z++) {
            if (scaleRange.contains((Number) gridSet.getGrid(z).getScaleDenominator())) {
                return true;
            }
        }

        return false;
    }

    private NumberRange<Double> parseScaleDenominator(String scaleDenominatorSpec) {
        // assuming a min/max expression
        String[] split = scaleDenominatorSpec.split("/");
        if (split.length != 2) {
            throw new InvalidParameterValueException(
                    "Unexpected format for 'scaleDenominator', should be minScale/maxScale");
        }
        try {
            double min = Double.parseDouble(split[0]);
            double max = Double.parseDouble(split[1]);
            if (max < min) {
                throw new InvalidParameterValueException(
                        "Unexpected values in 'scaleDenominator', minScale/maxScale, but minScale is greater than maxScale");
            }
            return new NumberRange<>(Double.class, min, max);
        } catch (NumberFormatException e) {
            throw new InvalidParameterValueException(
                    "Unexpected values in 'scaleDenominator', could not parse numbers out of them");
        }
    }

    private org.geowebcache.mime.MimeType parseMimeType(
            TileLayer tileLayer, String tileFormatSpec) {
        Optional<org.geowebcache.mime.MimeType> mimeTypeMaybe =
                tileLayer
                        .getMimeTypes()
                        .stream()
                        .filter(mt -> tileFormatSpec.equals(mt.getMimeType()))
                        .findFirst();
        if (!mimeTypeMaybe.isPresent()) {
            throw new InvalidParameterValueException(
                    "Tiled collection "
                            + tileLayer.getName()
                            + " does not support format "
                            + tileFormatSpec);
        }

        return mimeTypeMaybe.get();
    }

    public StyleInfo getStyle(@PathVariable(name = "styleId") String styleId) {
        StyleInfo styleInfo = catalog.getStyleByName(styleId);
        if (styleInfo == null) {
            throw new APIException(
                    "NotFound", "Could not locate style " + styleId, HttpStatus.NOT_FOUND);
        }
        return styleInfo;
    }

    CoverageInfo getStructuredCoverageInfo(String collectionId, boolean failIfNotFound)
            throws IOException {
        org.geoserver.catalog.CoverageInfo coverageInfo = catalog.getCoverageByName(collectionId);
        if (coverageInfo != null
                && coverageInfo.getGridCoverageReader(null, null)
                        instanceof StructuredGridCoverage2DReader) {
            return coverageInfo;
        }

        if (failIfNotFound) {
            throw new ResourceNotFoundException("Could not locate collection " + collectionId);
        } else {
            return null;
        }
    }

    private TileLayer getTileLayer(String collectionId) {
        try {
            return gwc.getTileLayerByName(collectionId);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(
                    "Tiled collection " + collectionId + " not found", e);
        }
    }
}
