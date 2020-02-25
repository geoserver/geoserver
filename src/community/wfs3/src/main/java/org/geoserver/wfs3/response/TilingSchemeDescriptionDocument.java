/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;

/** The object representing the list of available tiling schemes */
@JacksonXmlRootElement(localName = "TilingSchemeDescription")
public class TilingSchemeDescriptionDocument {

    private final GridSet gridSet;

    public TilingSchemeDescriptionDocument(GridSet gridSet) {
        this.gridSet = gridSet;
    }

    @JacksonXmlProperty(localName = "identifier")
    public String getIdentifier() {
        return gridSet.getName();
    }

    @JacksonXmlProperty(localName = "supportedCRS")
    public String getSupportedCRS() {
        return gridSet.getSrs().toString();
    }

    @JacksonXmlProperty(localName = "title")
    public String getTitle() {
        return gridSet.getName();
    }

    @JacksonXmlProperty(localName = "type")
    public String getType() {
        return "TileMatrixSet";
    }

    @JacksonXmlProperty(localName = "wellKnownScaleSet")
    public String getWellKnownScaleSet() {
        return "http://www.opengis.net/def/wkss/OGC/1.0/" + gridSet.getName();
    }

    @JacksonXmlProperty(localName = "boundingBox")
    public BoundingBoxDocument getBoundingBox() {
        BoundingBox bbox = gridSet.getBounds();
        BoundingBoxDocument bboxDoc = new BoundingBoxDocument();
        bboxDoc.setLowerCorner(Arrays.asList(bbox.getMinX(), bbox.getMinY()));
        bboxDoc.setUpperCorner(Arrays.asList(bbox.getMaxX(), bbox.getMaxY()));
        bboxDoc.setCrs("http://www.opengis.net/def/crs/EPSG/0/" + gridSet.getSrs().getNumber());
        return bboxDoc;
    }

    @JacksonXmlProperty(localName = "TileMatrix")
    public List<TileMatrixDocument> getTileMatrix() {
        List<TileMatrixDocument> tileMatrix = new ArrayList<>();
        for (Integer i = 0; i < gridSet.getNumLevels(); i++) {
            Grid grid = gridSet.getGrid(i);
            TileMatrixDocument tm = new TileMatrixDocument();
            tm.setIdentifier(i.toString());
            tm.setMatrixHeight(grid.getNumTilesHigh());
            tm.setMatrixWidth(grid.getNumTilesWide());
            tm.setScaleDenominator(grid.getScaleDenominator());
            tm.setTileHeight(256);
            tm.setTileWidth(256);
            tm.setTopLeftCorner(
                    Arrays.asList(
                            gridSet.getOrderedTopLeftCorner(i)[0],
                            gridSet.getOrderedTopLeftCorner(i)[1]));
            tileMatrix.add(tm);
        }
        return tileMatrix;
    }
}
