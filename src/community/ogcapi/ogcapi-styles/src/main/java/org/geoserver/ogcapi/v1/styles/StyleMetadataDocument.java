/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.styles;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.api.style.Description;
import org.geotools.api.style.NamedLayer;
import org.geotools.api.style.StyledLayer;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.api.style.UserLayer;
import org.geotools.util.Version;

/** Style metadadata information */
@JsonInclude(NON_NULL)
public class StyleMetadataDocument extends AbstractDocument {

    String id;
    String title;
    boolean titleSet;
    String description;
    boolean descriptionSet;
    List<String> keywords;
    boolean keywordsSet;
    String pointOfContact;
    boolean pointOfContactSet;
    String accessConstraints;
    boolean accessConstraintsSet;
    StyleDates dates;
    boolean datesSet;

    @JsonProperty(access = READ_ONLY)
    String scope = "style";

    @JsonProperty(access = READ_ONLY)
    List<Stylesheet> stylesheets = new ArrayList<>();

    @JsonProperty(access = READ_ONLY)
    List<StyleLayer> layers = new ArrayList<>();

    SampleDataSupport sampleDataSupport;

    public StyleMetadataDocument() {
        // empty constructor for Jackson usage
    }

    public StyleMetadataDocument(
            StyleInfo si,
            GeoServer gs,
            SampleDataSupport sampleDataSupport,
            ThumbnailBuilder thumbnails)
            throws IOException {
        this.sampleDataSupport = sampleDataSupport;
        this.id = si.prefixedName();
        StyledLayerDescriptor sld = si.getSLD();
        Optional<StyleMetadataInfo> styleMetadata =
                Optional.ofNullable(
                        si.getMetadata()
                                .get(StyleMetadataInfo.METADATA_KEY, StyleMetadataInfo.class));
        this.title = styleMetadata.map(StyleMetadataInfo::getTitle).orElseGet(() -> getTitle(sld));
        this.description =
                styleMetadata
                        .map(StyleMetadataInfo::getAbstract)
                        .orElseGet(() -> getDescription(sld));
        this.keywords = styleMetadata.map(StyleMetadataInfo::getKeywords).orElse(null);
        this.pointOfContact =
                styleMetadata
                        .map(StyleMetadataInfo::getPointOfContact)
                        .orElseGet(() -> gs.getSettings().getContact().getContactPerson());
        this.accessConstraints =
                styleMetadata.map(StyleMetadataInfo::getAccessConstraints).orElse("unclassified");

        // TODO: dates

        // stylesheet links
        for (StyleHandler handler : Styles.handlers()) {
            for (Version version : handler.getVersions()) {
                Stylesheet ss = new Stylesheet(si, handler, version);
                if (ss.isNative() || handler.supportsEncoding(version)) {
                    stylesheets.add(ss);
                }
            }
        }

        // layers links
        StyledLayer[] styledLayers = sld.getStyledLayers();

        if (styledLayers.length == 1) {
            // common GeoServer case, there is a single layer referenced and we use only the style
            // portion, not the layer one, we allow both userlayer and namedlayer
            StyleLayer sl =
                    new StyleLayer(si, styledLayers[0], gs.getCatalog(), sampleDataSupport, false);
            layers.add(sl);
        } else {
            // here we skip the UserLayer, as they should contain data inline, so they get skipped
            layers =
                    Arrays.stream(styledLayers)
                            .filter(sl -> sl instanceof NamedLayer)
                            .map(
                                    nl ->
                                            new StyleLayer(
                                                    si,
                                                    nl,
                                                    gs.getCatalog(),
                                                    sampleDataSupport,
                                                    true))
                            .collect(Collectors.toList());
        }

        // link to the thumbnail if possible
        if (thumbnails.canGenerateThumbnail(si)) {
            String baseURL = APIRequestInfo.get().getBaseURL();
            String href =
                    ResponseUtils.buildURL(
                            baseURL,
                            "ogc/styles/v1/styles/"
                                    + ResponseUtils.urlEncode(si.prefixedName())
                                    + "/thumbnail",
                            Collections.singletonMap("f", "image/png"),
                            URLMangler.URLType.SERVICE);
            addLink(new Link(href, "preview", "image/png", "Thumbnail for " + si.prefixedName()));
        }
    }

    private String getTitle(StyledLayerDescriptor sld) {
        if (sld.getTitle() != null) {
            return sld.getTitle();
        }

        return getDescriptions(sld)
                .filter(d -> d.getTitle() != null)
                .map(d -> d.getTitle().toString())
                .findFirst()
                .orElse(null);
    }

    private String getDescription(StyledLayerDescriptor sld) {
        if (sld.getAbstract() != null) {
            return sld.getAbstract();
        }

        return getDescriptions(sld)
                .filter(d -> d.getAbstract() != null)
                .map(d -> d.getAbstract().toString())
                .findFirst()
                .orElse(null);
    }

    /** Extracts the description of all styles contained in user layers or named layers */
    private Stream<Description> getDescriptions(StyledLayerDescriptor sld) {
        return Arrays.stream(sld.getStyledLayers())
                .flatMap(
                        sl -> {
                            if (sl instanceof NamedLayer) {
                                return Arrays.stream(((NamedLayer) sl).getStyles());
                            } else if (sl instanceof UserLayer) {
                                return Arrays.stream(((UserLayer) sl).getUserStyles());
                            }
                            return Stream.empty();
                        })
                .filter(s -> s != null && s.getDescription() != null && s.getDescription() != null)
                .map(s -> s.getDescription());
    }

    @Override
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getPointOfContact() {
        return pointOfContact;
    }

    public String getAccessConstraints() {
        return accessConstraints;
    }

    public StyleDates getDates() {
        return dates;
    }

    public String getScope() {
        return scope;
    }

    public List<Stylesheet> getStylesheets() {
        return stylesheets;
    }

    public List<StyleLayer> getLayers() {
        return layers;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.titleSet = true;
        this.title = title;
    }

    public void setDescription(String description) {
        this.descriptionSet = true;
        this.description = description;
    }

    public void setKeywords(List<String> keywords) {
        this.keywordsSet = true;
        this.keywords = keywords;
    }

    public void setPointOfContact(String pointOfContact) {
        this.pointOfContactSet = true;
        this.pointOfContact = pointOfContact;
    }

    public void setAccessConstraints(String accessConstraints) {
        this.accessConstraintsSet = true;
        this.accessConstraints = accessConstraints;
    }

    public void setDates(StyleDates dates) {
        this.dates = dates;
    }

    @JsonIgnore
    public boolean isTitleSet() {
        return titleSet;
    }

    @JsonIgnore
    public boolean isDescriptionSet() {
        return descriptionSet;
    }

    @JsonIgnore
    public boolean isKeywordsSet() {
        return keywordsSet;
    }

    @JsonIgnore
    public boolean isPointOfContactSet() {
        return pointOfContactSet;
    }

    @JsonIgnore
    public boolean isAccessConstraintsSet() {
        return accessConstraintsSet;
    }

    @JsonIgnore
    public boolean isDatesSet() {
        return datesSet;
    }
}
