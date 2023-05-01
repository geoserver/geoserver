/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.Serializable;
import java.util.List;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geotools.util.GrowableInternationalString;
import org.opengis.util.InternationalString;

/**
 * A LayerGroupStyle providing is a different named configuration (as a set of {@PublishedInfo} and
 * {@StyleInfo}) for a LayerGroup.
 */
public interface LayerGroupStyle extends Serializable, Info {

    /**
     * Get the Style name
     *
     * @return the style name as a StyleInfo.
     */
    StyleInfo getName();

    /**
     * Set the style name.
     *
     * @param name the style name as a StyleInfo object.
     */
    void setName(StyleInfo name);

    /**
     * Get the contained List of PublishedInfo.
     *
     * @return the list of contained PublishedInfo.
     */
    List<PublishedInfo> getLayers();

    /**
     * Set the List of PublishedInfo.
     *
     * @param layers the list of published info.
     */
    void setLayers(List<PublishedInfo> layers);

    /**
     * Get the List of StyleInfo.
     *
     * @return the List of StyleInfo.
     */
    List<StyleInfo> getStyles();

    /**
     * Set the List of StyleInfo.
     *
     * @param styles the List of StyleInfo.
     */
    void setStyles(List<StyleInfo> styles);

    /**
     * Set the id.
     *
     * @param id the id.
     */
    void setId(String id);

    /**
     * Get the title.
     *
     * @return the title
     */
    String getTitle();

    /**
     * Set the title.
     *
     * @param title the title.
     */
    void setTitle(String title);

    /**
     * Get the internationalTitle.
     *
     * @return the international Title.
     */
    GrowableInternationalString getInternationalTitle();

    /**
     * Set the internationalTitle.
     *
     * @param internationalTitle the international title.
     */
    void setInternationalTitle(InternationalString internationalTitle);

    /**
     * Get the abstract.
     *
     * @return the abstract.
     */
    String getAbstract();

    /**
     * Set the abstract.
     *
     * @param abstractTxt the abstract.
     */
    void setAbstract(String abstractTxt);

    /**
     * Get the internationalAbstract.
     *
     * @return the internationalAbstract.
     */
    GrowableInternationalString getInternationalAbstract();

    /**
     * Set the internationalAbstract.
     *
     * @param internationalAbstract the internationalAbstract.
     */
    void setInternationalAbstract(GrowableInternationalString internationalAbstract);
}
