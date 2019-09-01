/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.CascadedLayerInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geotools.ows.wms.StyleImpl;
import org.geotools.styling.NamedStyleImpl;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;

public class CascadedLayerInfoImpl extends LayerInfoImpl implements CascadedLayerInfo {

    static final Logger LOGGER = Logging.getLogger(CascadedLayerInfo.class);

    /** serialVersionUID */
    private static final long serialVersionUID = -4772580517514254546L;

    protected String forcedRemoteStyle = "";
    protected String prefferedFormat = "image/png";

    private List<String> selectedRemoteFormats = new ArrayList<String>();

    private List<String> selectedRemoteStyles = new ArrayList<String>();

    public CascadedLayerInfoImpl() {}

    public CascadedLayerInfoImpl(LayerInfo delegate) {

        super.setResource(delegate.getResource());
        super.setName(delegate.getResource().getName());
        super.setEnabled(delegate.enabled());
        super.setType(delegate.getType());
    }

    @Override
    public void reset() {
        // select all formats for use
        selectedRemoteStyles.addAll(getRemoteStyles());
        // set first style is selected
        if (!selectedRemoteStyles.isEmpty()) forcedRemoteStyle = selectedRemoteStyles.get(0);
        // select all styles available for us
        selectedRemoteFormats.addAll(getAvailableFormats());
    }

    @Override
    public List<String> getRemoteStyles() {

        WMSLayerInfo layerInfo = (WMSLayerInfo) super.resource;

        try {
            // read from cap doc
            return layerInfo
                    .getWMSLayer(null)
                    .getStyles()
                    .stream()
                    .map(s -> s.getName())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Unable to fetch styles for cascaded layer " + layerInfo.getName());
            LOGGER.log(Level.SEVERE, e.getMessage());
        }

        return Collections.EMPTY_LIST;
    }

    @Override
    public String getForcedRemoteStyle() {
        return forcedRemoteStyle;
    }

    @Override
    public void setForcedRemoteStyle(String forcedRemoteStyle) {
        this.forcedRemoteStyle = forcedRemoteStyle;
    }

    @Override
    public List<String> getAvailableFormats() {
        WMSLayerInfo layerInfo = (WMSLayerInfo) super.resource;
        try {
            return layerInfo
                    .getStore()
                    .getWebMapServer(null)
                    .getCapabilities()
                    .getRequest()
                    .getGetMap()
                    .getFormats()
                    .stream()
                    .filter(CascadedLayerInfoImpl::isImage)
                    . // only image formats
                    collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Unable to fetch available formats for cascaded layer " + layerInfo.getName());
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public String getPrefferedFormat() {
        return this.prefferedFormat;
    }

    @Override
    public void setPrefferedFormat(String prefferedFormat) {
        this.prefferedFormat = prefferedFormat;
    }

    @Override
    public boolean isSelectedRemoteFormat(String format) {
        if (prefferedFormat.equalsIgnoreCase(format)) return true;
        else return selectedRemoteFormats.contains(format);
    }

    public static boolean isImage(String format) {
        return format.startsWith("image") && !format.contains("xml") && !format.contains("svg");
    }

    @Override
    public Set<StyleInfo> getStyles() {
        // legacy functionality for empty
        if (getRemoteStyles().isEmpty()) return super.getStyles();

        return getRemoteStyleInfos()
                .stream()
                .filter(s -> !forcedRemoteStyle.equalsIgnoreCase(s.getName()))
                .filter(s -> selectedRemoteStyles.contains(s.getName()))
                .collect(Collectors.toSet());
    }

    @Override
    public StyleInfo getDefaultStyle() {
        if (forcedRemoteStyle != null)
            if (!forcedRemoteStyle.isEmpty()) {
                Optional<StyleInfo> defaultRemoteStyle =
                        getRemoteStyleInfos()
                                .stream()
                                .filter(s -> s.getName().equalsIgnoreCase(forcedRemoteStyle))
                                .findFirst();

                if (defaultRemoteStyle.isPresent()) return defaultRemoteStyle.get();
            }
        // default to legacy if layer has no forcedRemoteStyle(unlikely)
        return super.getDefaultStyle();
    }

    @Override
    public Set<StyleInfo> getRemoteStyleInfos() {
        WMSLayerInfo layerInfo = (WMSLayerInfo) super.resource;

        try {
            return layerInfo
                    .getWMSLayer(null)
                    .getStyles()
                    .stream()
                    .map(CascadedLayerInfoImpl::getStyleInfo)
                    .collect(Collectors.toSet());

        } catch (IOException e) {
            LOGGER.severe(e.getMessage());
        }
        // on error default to super
        return super.getStyles();
    }

    @Override
    public Optional<Style> findRemoteStyleByName(final String name) {

        WMSLayerInfo layerInfo = (WMSLayerInfo) super.resource;
        // temp wrapper for the remote style name
        final Style style = new NamedStyleImpl();
        style.setName(name);
        try {
            return layerInfo
                    .getWMSLayer(null)
                    .getStyles()
                    .stream()
                    .filter(s -> s.getName().equalsIgnoreCase(name))
                    .map(s -> style)
                    .findFirst();
        } catch (IOException e) {
            LOGGER.severe(e.getMessage());
        }

        return Optional.empty();
    }

    public static StyleInfo getStyleInfo(StyleImpl gtWmsStyle) {

        StyleInfoImpl styleInfo = new StyleInfoImpl();

        styleInfo.setId(gtWmsStyle.getName());
        styleInfo.setName(gtWmsStyle.getName());
        // a hint
        styleInfo.getMetadata().put("isRemote", true);

        LegendInfo remoteLegendInfo = new LegendInfoImpl();
        remoteLegendInfo.setOnlineResource((String) gtWmsStyle.getLegendURLs().get(0));
        remoteLegendInfo.setHeight(20);
        remoteLegendInfo.setWidth(20);
        remoteLegendInfo.setFormat("image/png");
        styleInfo.setLegend(remoteLegendInfo);

        return styleInfo;
    }

    public static Style getStyleInfo(StyleInfo styleInfo) {

        NamedStyleImpl gtStyle = new NamedStyleImpl();
        gtStyle.setName(styleInfo.getName());
        return gtStyle;
    }

    public boolean isSelectedRemoteStyles(String name) {
        if (name == null) return false;
        else if (forcedRemoteStyle.equalsIgnoreCase(name)) return true;
        else return selectedRemoteStyles.contains(name);
    }

    public List<String> getSelectedRemoteFormats() {
        return selectedRemoteFormats;
    }

    public void setSelectedRemoteFormats(List<String> selectedRemoteFormats) {
        this.selectedRemoteFormats = selectedRemoteFormats;
    }

    public List<String> getSelectedRemoteStyles() {
        return selectedRemoteStyles;
    }

    public void setSelectedRemoteStyles(List<String> selectedRemoteStyles) {
        this.selectedRemoteStyles = selectedRemoteStyles;
    }
}
