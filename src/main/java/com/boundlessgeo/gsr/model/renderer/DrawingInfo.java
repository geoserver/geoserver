package com.boundlessgeo.gsr.model.renderer;

/**
 * Wraper class for renderer
 */
public class DrawingInfo {
    public final Renderer renderer;
    // transparency - not supported
    // labelingInfo - not supported. could read from style?

    public DrawingInfo(Renderer renderer) {
        this.renderer = renderer;
    }
}
