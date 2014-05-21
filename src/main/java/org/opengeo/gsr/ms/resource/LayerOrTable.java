package org.opengeo.gsr.ms.resource;

import org.opengeo.gsr.core.geometry.GeometryTypeEnum;
import org.opengeo.gsr.core.renderer.Renderer;
import org.geoserver.catalog.LayerInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;

public class LayerOrTable {
    public final LayerInfo layer;
    public final int id;
    public final GeometryTypeEnum gtype;
    public final ReferencedEnvelope boundingBox;
    public final Renderer renderer;

    LayerOrTable(LayerInfo layer, int id, GeometryTypeEnum gtype, ReferencedEnvelope boundingBox, Renderer renderer) {
        this.layer = layer;
        this.id = id;
        this.gtype = gtype;
        this.boundingBox = boundingBox;
        this.renderer = renderer;
    }

    @Override public String toString() {
        return id + ":" + layer.getName();
    }
}
