/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.labeling;

/** Compound type holding bounds and configurations. */
class AttributesGlobeBounds {

    private final GlobeBounds globeBounds;
    private final TextBounds textBounds;
    private final AttributesGlobeConfiguration configuration;

    public AttributesGlobeBounds(
            GlobeBounds globeBounds,
            TextBounds textBounds,
            AttributesGlobeConfiguration configuration) {
        this.globeBounds = globeBounds;
        this.textBounds = textBounds;
        this.configuration = configuration;
    }

    /** Returns the globe shape bounds. */
    public GlobeBounds getGlobeBounds() {
        return globeBounds;
    }

    /** Returns the Text bounds per line */
    public TextBounds getTextBounds() {
        return textBounds;
    }

    public double getWidth() {
        return globeBounds.getWidth();
    }

    public double getHeight() {
        return globeBounds.getHeight() + configuration.getTailHeight();
    }

    public AttributesGlobeConfiguration getConfiguration() {
        return configuration;
    }
}
