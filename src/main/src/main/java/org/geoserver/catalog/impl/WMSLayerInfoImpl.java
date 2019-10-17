/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.StyleImpl;
import org.geotools.styling.NamedStyleImpl;
import org.geotools.styling.Style;
import org.opengis.util.ProgressListener;

@SuppressWarnings("serial")
public class WMSLayerInfoImpl extends ResourceInfoImpl implements WMSLayerInfo {

    protected String forcedRemoteStyle = "";
    protected String prefferedFormat = "image/png";

    private List<String> selectedRemoteFormats = new ArrayList<String>();

    private List<String> selectedRemoteStyles = new ArrayList<String>();

    protected WMSLayerInfoImpl() {}

    public WMSLayerInfoImpl(Catalog catalog) {
        super(catalog);
    }

    public Layer getWMSLayer(ProgressListener listener) throws IOException {
        return catalog.getResourcePool().getWMSLayer(this);
    }

    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public WMSStoreInfo getStore() {
        return (WMSStoreInfo) super.getStore();
    }

    @Override
    public void reset() {
        selectedRemoteStyles.clear();
        selectedRemoteFormats.clear();
        // select all formats for use
        selectedRemoteStyles.addAll(remoteStyles());
        // set empty to take whatever is on remote server
        forcedRemoteStyle = "";
        // select all styles available for us
        selectedRemoteFormats.addAll(availableFormats());
    }

    @Override
    public List<String> remoteStyles() {

        try {
            // read from cap doc
            return getWMSLayer(null)
                    .getStyles()
                    .stream()
                    .map(s -> s.getName())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Unable to fetch styles for cascaded wms layer " + getNativeName());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
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
    public List<String> availableFormats() {
        try {
            return getStore()
                    .getWebMapServer(null)
                    .getCapabilities()
                    .getRequest()
                    .getGetMap()
                    .getFormats()
                    .stream()
                    .filter(WMSLayerInfoImpl::isImage)
                    . // only image formats
                    collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Unable to fetch available formats for cascaded layer " + getNativeName());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return Collections.EMPTY_LIST;
        }
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
    public boolean isFormatValid(String format) {
        if (prefferedFormat.equalsIgnoreCase(format)) return true;
        else return selectedRemoteFormats.contains(format);
    }

    public static boolean isImage(String format) {
        return format.startsWith("image") && !format.contains("xml") && !format.contains("svg");
    }

    @Override
    public Set<StyleInfo> getStyles() {
        Set<StyleInfo> remoteStyleInfos = getRemoteStyleInfos();
        if (remoteStyleInfos == null) return null;
        else
            return remoteStyleInfos
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
                // will return null if forcedRemoteStyle is not empty string
                // and was not found in selected remote styles
                if (defaultRemoteStyle.isPresent()) return defaultRemoteStyle.get();
            } else {
                StyleInfoImpl emptyStyleInfo = new StyleInfoImpl();
                emptyStyleInfo.setName("");
                emptyStyleInfo.getMetadata().put("isRemote", true);
                return emptyStyleInfo;
            }

        return null;
    }

    @Override
    public Set<StyleInfo> getRemoteStyleInfos() {

        try {
            return getWMSLayer(null)
                    .getStyles()
                    .stream()
                    .map(WMSLayerInfoImpl::getStyleInfo)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Unable to fetch available styles for cascaded layer " + getNativeName());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        // on error default to super
        return null;
    }

    @Override
    public Optional<Style> findRemoteStyleByName(final String name) {

        // temp wrapper for the remote style name
        final Style style = new NamedStyleImpl();
        style.setName(name);
        try {
            return getWMSLayer(null)
                    .getStyles()
                    .stream()
                    .filter(s -> s.getName().equalsIgnoreCase(name))
                    .map(s -> style)
                    .findFirst();
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Unable to fetch available styles for cascaded layer " + getNativeName());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
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
        else if (name.isEmpty()) return true;
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
