/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import org.apache.commons.lang3.StringUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;

/** Current tiling request scope data, for context propagation */
public class TileDataRequest {

    private String tilingScheme;
    private Long level;
    private Long row;
    private Long col;
    private ReferencedEnvelope bboxEnvelope;

    public TileDataRequest() {}

    public boolean isTileRequest() {
        return (StringUtils.isNotBlank(tilingScheme) && level != null && row != null & col != null);
    }

    public String getTilingScheme() {
        return tilingScheme;
    }

    public void setTilingScheme(String tilingScheme) {
        this.tilingScheme = tilingScheme;
    }

    public Long getLevel() {
        return level;
    }

    public void setLevel(Long level) {
        this.level = level;
    }

    public Long getRow() {
        return row;
    }

    public void setRow(Long row) {
        this.row = row;
    }

    public Long getCol() {
        return col;
    }

    public void setCol(Long col) {
        this.col = col;
    }

    public ReferencedEnvelope getBboxEnvelope() {
        return bboxEnvelope;
    }

    public void setBboxEnvelope(ReferencedEnvelope bboxEnvelope) {
        this.bboxEnvelope = bboxEnvelope;
    }
}
