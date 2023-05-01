/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.util.InternationalStringUtils;
import org.geotools.util.GrowableInternationalString;
import org.opengis.util.InternationalString;

public class LayerGroupStyleImpl implements LayerGroupStyle {

    private String id = getClass().getSimpleName().concat("--").concat(new UID().toString());

    // Uses a StyleInfo type for the name in order to be able to reference LayerGroup styles from
    // the default configuration or
    // from other LayerGroupStyle since the contained style are represented as a List of StyleInfo.
    private StyleInfo name;

    private String title;

    private GrowableInternationalString internationalTitle;

    private String abstractTxt;

    private GrowableInternationalString internationalAbstract;

    private List<PublishedInfo> layers = new ArrayList<>();

    private List<StyleInfo> styles = new ArrayList<>();

    public LayerGroupStyleImpl() {}

    public LayerGroupStyleImpl(LayerGroupStyleImpl groupStyle) {
        // copy constructor intended to be used by the reflection
        // mechanism of ModificationProxy.
        this((LayerGroupStyle) groupStyle);
    }

    public LayerGroupStyleImpl(LayerGroupStyle groupStyle) {
        this.id = groupStyle.getId();
        this.name = groupStyle.getName();
        this.title = groupStyle.getTitle();
        this.abstractTxt = groupStyle.getAbstract();
        this.internationalTitle = groupStyle.getInternationalTitle();
        this.internationalAbstract = groupStyle.getInternationalAbstract();
        this.layers = new ArrayList<>();
        this.layers.addAll(groupStyle.getLayers());
        this.styles = new ArrayList<>(groupStyle.getStyles().size());
        this.styles.addAll(groupStyle.getStyles());
    }

    @Override
    public StyleInfo getName() {
        return name;
    }

    @Override
    public void setName(StyleInfo name) {
        this.name = name;
    }

    @Override
    public List<PublishedInfo> getLayers() {
        return layers;
    }

    @Override
    public void setLayers(List<PublishedInfo> layers) {
        this.layers = layers;
    }

    @Override
    public List<StyleInfo> getStyles() {
        return styles;
    }

    @Override
    public void setStyles(List<StyleInfo> styles) {
        this.styles = styles;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getTitle() {
        return InternationalStringUtils.getOrDefault(title, internationalAbstract);
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public GrowableInternationalString getInternationalTitle() {
        return internationalTitle;
    }

    @Override
    public void setInternationalTitle(InternationalString internationalTitle) {
        this.internationalTitle = InternationalStringUtils.growable(internationalTitle);
    }

    @Override
    public String getAbstract() {
        return InternationalStringUtils.getOrDefault(abstractTxt, internationalAbstract);
    }

    @Override
    public void setAbstract(String abstractTxt) {
        this.abstractTxt = abstractTxt;
    }

    @Override
    public GrowableInternationalString getInternationalAbstract() {
        return internationalAbstract;
    }

    @Override
    public void setInternationalAbstract(GrowableInternationalString internationalAbstract) {
        this.internationalAbstract = InternationalStringUtils.growable(internationalAbstract);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LayerGroupStyleImpl that = (LayerGroupStyleImpl) o;
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(title, that.title)
                && Objects.equals(internationalTitle, that.internationalTitle)
                && Objects.equals(abstractTxt, that.abstractTxt)
                && Objects.equals(internationalAbstract, that.internationalAbstract)
                && Objects.equals(layers, that.layers)
                && Objects.equals(styles, that.styles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                name,
                title,
                internationalTitle,
                abstractTxt,
                internationalAbstract,
                layers,
                styles);
    }
}
