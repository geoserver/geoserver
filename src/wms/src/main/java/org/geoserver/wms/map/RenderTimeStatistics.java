/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.util.*;
import java.util.stream.Collectors;
import org.geotools.map.Layer;
import org.geotools.renderer.RenderListener;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public class RenderTimeStatistics implements RenderListener {

    public static final String ID = "statistics";
    private List<Layer> layers;
    private Map<Integer, Long> startRenderingLayersTimes;
    private Map<Integer, Long> endRenderingLayersTimes;
    private Map<Integer, Long> renderingLayersTimes;
    private Long renderingLabelsTimes;
    private Long startRenderingLabelsTimes;
    private Long endRenderingLabelsTimes;
    private int index = 0;
    private List<Integer> renderingLayersIdxs;

    public RenderTimeStatistics() {
        this.layers = new LinkedList<>();
        this.startRenderingLayersTimes = new HashMap<Integer, Long>();
        this.endRenderingLayersTimes = new HashMap<Integer, Long>();
        this.renderingLayersTimes = new HashMap<Integer, Long>();
        this.renderingLayersIdxs = new ArrayList<Integer>();
    }

    @Override
    public void featureRenderer(SimpleFeature feature) {}

    @Override
    public void errorOccurred(Exception e) {}

    @Override
    public void layerStart(Layer layer) {
        layers.add(index, layer);
        startRenderingLayersTimes.put(index, System.currentTimeMillis());
        renderingLayersIdxs.add(index);
        index++;
    }

    @Override
    public void layerEnd(Layer layer) {
        Integer key = layers.indexOf(layer);
        endRenderingLayersTimes.put(key, System.currentTimeMillis());
    }

    @Override
    public void labellingStart() {
        startRenderingLabelsTimes = System.currentTimeMillis();
    }

    @Override
    public void labellingEnd() {
        endRenderingLabelsTimes = System.currentTimeMillis();
    }

    @Override
    public void renderingComplete() {
        for (Integer idx : renderingLayersIdxs) {
            if (renderingLayersTimes.get(idx) == null) {
                Long startingTime = startRenderingLayersTimes.get(idx);
                Long endingTime = endRenderingLayersTimes.get(idx);
                renderingLayersTimes.put(idx, endingTime != null ? endingTime - startingTime : 0L);
            }
        }
        renderingLabelsTimes =
                startRenderingLabelsTimes != null && endRenderingLabelsTimes != null
                        ? endRenderingLabelsTimes - startRenderingLabelsTimes
                        : 0L;
        addSelfAsRequestAttribute();
    }

    public Long getRenderingTime(Integer layerId) {
        return renderingLayersTimes.get(layerId);
    }

    public long getLabellingTime() {
        return this.renderingLabelsTimes;
    }

    public List<String> getLayerNames() {
        return layers.stream()
                .map(l -> l.getTitle() != null ? l.getTitle() : "Layer" + (layers.indexOf(l) + 1))
                .collect(Collectors.toList());
    }

    public List<Layer> getLayers() {
        return layers;
    }

    public void setLayers(List<Layer> layers) {
        this.layers = layers;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Long getRenderingLabelsTimes() {
        return renderingLabelsTimes;
    }

    public void setRenderingLabelsTimes(Long renderingLabelsTimes) {
        this.renderingLabelsTimes = renderingLabelsTimes;
    }

    public Map<Integer, Long> getRenderingLayersTimes() {
        return renderingLayersTimes;
    }

    public void setRenderingLayersTimes(Map<Integer, Long> renderingLayersTimes) {
        this.renderingLayersTimes = renderingLayersTimes;
    }

    public List<Integer> getRenderingLayersIdxs() {
        return renderingLayersIdxs;
    }

    // adding attribute here to avoid code repetition since rendering completed gets called in
    // different places
    // depending on the kind of layer being rendered
    private void addSelfAsRequestAttribute() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.setAttribute(
                    RenderTimeStatistics.ID, this, RequestAttributes.SCOPE_REQUEST);
        }
    }
}
