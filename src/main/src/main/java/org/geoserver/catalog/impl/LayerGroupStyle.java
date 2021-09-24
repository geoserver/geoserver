/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.Serializable;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.util.InternationalStringUtils;
import org.geotools.util.GrowableInternationalString;
import org.opengis.util.InternationalString;

/**
 * This class represents a LayerGroupStyle as a named configuration holding a list of Layers and
 * corresponding Styles.
 */
public class LayerGroupStyle implements Serializable {

    private String id = getClass().getSimpleName().concat("--").concat(new UID().toString());

    // Uses a StyleInfo type for the name in order to be able to reference LayerGroup styles from
    // the default configuration or
    // from other LayerGroupStyle since the contained style are represented as a List of StyleInfo.
    private StyleInfo name = new StyleInfoImpl((Catalog) GeoServerExtensions.bean("catalog"));

    private String title;

    private GrowableInternationalString internationalTitle;

    private String abstractTxt;

    private GrowableInternationalString internationalAbstract;

    private List<PublishedInfo> layers = new ArrayList<>();

    private List<StyleInfo> styles = new ArrayList<>();

    /**
     * Get the Style name
     *
     * @return the style name as a StyleInfo.
     */
    public StyleInfo getName() {
        return name;
    }

    /**
     * Set the style name.
     *
     * @param name the style name as a StyleInfo object.
     */
    public void setName(StyleInfo name) {
        this.name = name;
    }

    /**
     * Get the contained List of PublishedInfo.
     *
     * @return the list of contained PublishedInfo.
     */
    public List<PublishedInfo> getLayers() {
        return layers;
    }

    /**
     * Set the List of PublishedInfo.
     *
     * @param layers the list of published info.
     */
    public void setLayers(List<PublishedInfo> layers) {
        this.layers = layers;
    }

    /**
     * Get the List of StyleInfo.
     *
     * @return the List of StyleInfo.
     */
    public List<StyleInfo> getStyles() {
        return styles;
    }

    /**
     * Set the List of StyleInfo.
     *
     * @param styles the List of StyleInfo.
     */
    public void setStyles(List<StyleInfo> styles) {
        this.styles = styles;
    }

    /**
     * Get the id.
     *
     * @return the id.
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id.
     *
     * @param id the id.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the title.
     *
     * @return the title
     */
    public String getTitle() {
        return InternationalStringUtils.getOrDefault(title, internationalAbstract);
    }

    /**
     * Set the title.
     *
     * @param title the title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get the internationalTitle.
     *
     * @return the international Title.
     */
    public GrowableInternationalString getInternationalTitle() {
        return internationalTitle;
    }

    /**
     * Set the internationalTitle.
     *
     * @param internationalTitle the international title.
     */
    public void setInternationalTitle(InternationalString internationalTitle) {
        this.internationalTitle = InternationalStringUtils.growable(internationalTitle);
    }

    /**
     * Get the abstract.
     *
     * @return the abstract.
     */
    public String getAbstract() {
        return InternationalStringUtils.getOrDefault(abstractTxt, internationalAbstract);
    }

    /**
     * Set the abstract.
     *
     * @param abstractTxt the abstract.
     */
    public void setAbstract(String abstractTxt) {
        this.abstractTxt = abstractTxt;
    }

    /**
     * Get the internationalAbstract.
     *
     * @return the internationalAbstract.
     */
    public GrowableInternationalString getInternationalAbstract() {
        return internationalAbstract;
    }

    /**
     * Set the internationalAbstract.
     *
     * @param internationalAbstract the internationalAbstract.
     */
    public void setInternationalAbstract(GrowableInternationalString internationalAbstract) {
        this.internationalAbstract = InternationalStringUtils.growable(internationalAbstract);
    }
}
