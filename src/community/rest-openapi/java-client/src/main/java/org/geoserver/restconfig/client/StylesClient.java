package org.geoserver.restconfig.client;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.openapi.model.catalog.StyleInfo;
import org.geoserver.openapi.model.catalog.StyleInfo.FormatEnum;
import org.geoserver.openapi.model.catalog.WorkspaceInfo;
import org.geoserver.openapi.v1.client.StylesApi;
import org.geoserver.openapi.v1.model.NamedLink;
import org.geoserver.openapi.v1.model.StyleInfoWrapper;
import org.geoserver.openapi.v1.model.StyleList;
import org.geoserver.restconfig.api.client.ExtendedStylesApi;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class StylesClient {

    public static @RequiredArgsConstructor(access = AccessLevel.PRIVATE) enum StyleFormat {
        MAPBOX("application/vnd.geoserver.mbstyle+json"), //
        SLD_1_0_0("application/vnd.ogc.sld+xml"), //
        SLD_1_1_0("application/vnd.ogc.se+xml"), //
        GEOCSS("application/vnd.geoserver.geocss+css"), //
        YSLD("application/vnd.geoserver.ysld+yaml");

        private final @Getter String mimeType;
    }

    private @NonNull GeoServerClient client;

    private ExtendedStylesApi stylesApi;

    public ExtendedStylesApi api() {
        if (stylesApi == null) {
            stylesApi = client.api(ExtendedStylesApi.class);
        }
        return stylesApi;
    }

    /** @return the global styles {@link NamedLink}s */
    public StyleList getStyles() {
        return client.collectionCall(() -> api().getStyles().getStyles(), StyleList::new);
    }

    /** @return the requested workspace styles {@link NamedLink}s */
    public StyleList getStyles(@NonNull String workspaceName) {
        return client.collectionCall(
                () -> api().getStylesByWorkspace(workspaceName).getStyles(), StyleList::new);
    }

    /** @return the requested global style info */
    public StyleInfo get(@NonNull String styleName) {
        return api().getStyle(styleName).getStyle();
    }

    public String getBody(@NonNull StyleInfo style) {
        switch (style.getFormat()) {
            case MBSTYLE:
                if (null == style.getWorkspace())
                    return getBody(style.getName(), StyleFormat.MAPBOX);
                else
                    return getBody(
                            style.getWorkspace().getName(), style.getName(), StyleFormat.MAPBOX);
            case SLD:
                if (null == style.getWorkspace())
                    return getBody(style.getName(), StyleFormat.SLD_1_0_0);
                else
                    return getBody(
                            style.getWorkspace().getName(), style.getName(), StyleFormat.SLD_1_0_0);
            default:
                throw new UnsupportedOperationException(
                        "Unknown or unsupported style output format: " + style.getFormat());
        }
    }

    /** @return the requested global style contents */
    public String getBody(@NonNull String styleName, @NonNull StyleFormat format) {
        switch (format) {
            case SLD_1_0_0:
                return api().getStyleBodySLD10(styleName);
            case MAPBOX:
                return api().getStyleBodyMapbox(styleName);
            case GEOCSS:
            case SLD_1_1_0:
            case YSLD:
            default:
                throw new UnsupportedOperationException(
                        "Unknown or unsupported style output format: " + format);
        }
    }

    /** @return the requested style info on the specified workspace */
    public StyleInfo get(@NonNull String workspaceName, @NonNull String styleName) {
        return api().getStyleByWorkspace(workspaceName, styleName).getStyle();
    }

    /** @return the contents of the requested style on the specified workspace */
    public String getBody(
            @NonNull String workspaceName, @NonNull String styleName, @NonNull StyleFormat format) {
        switch (format) {
            case MAPBOX:
                return api().getStyleBodyMapbox(workspaceName, styleName);
            case SLD_1_0_0:
                return api().getStyleBodySLD10(workspaceName, styleName);
            case GEOCSS:
            case SLD_1_1_0:
            case YSLD:
            default:
                throw new UnsupportedOperationException(
                        "Unknown or unsupported style output format: " + format);
        }
    }

    /**
     * Creating a new style is a two step process, first the {@link StyleInfo} is created through
     * {@link StylesApi#createGlobalStyle} or {@link StylesApi#createStyle}, and then the actual
     * style document is uploaded through {@link StylesApi#putStyle}.
     *
     * <p>This method makes sure to roll back the {@code StyleInfo} creation in case the upload
     * fails.
     */
    public StyleInfo createMapboxStyle(@NonNull String name, @NonNull String requestBody) {
        requestBody = validateMapboxStyle(requestBody);
        StyleInfo info =
                new StyleInfo().name(name).filename(name + ".json").format(FormatEnum.MBSTYLE);
        info = createStyleInfo(info);
        api().uploadStyleMapbox(info.getName(), requestBody.getBytes(UTF_8), false);
        return info;
    }

    public StyleInfo createSLDStyle(@NonNull String name, @NonNull String requestBody) {
        StyleInfo info = new StyleInfo().name(name).filename(name + ".sld").format(FormatEnum.SLD);
        info = createStyleInfo(info);
        api().uploadStyleSLD(info.getName(), requestBody.getBytes(UTF_8), false);
        return info;
    }

    public StyleInfo createMapboxStyle(
            @NonNull String workspaceName, @NonNull String name, @NonNull String requestBody) {

        requestBody = validateMapboxStyle(requestBody);
        StyleInfo info =
                new StyleInfo()
                        .name(name)
                        .filename(name + ".json")
                        .format(FormatEnum.MBSTYLE)
                        .workspace(new WorkspaceInfo().name(workspaceName));
        info = createStyleInfo(info);
        api().uploadStyleMapbox(workspaceName, name, requestBody.getBytes(UTF_8), false);
        return info;
    }

    public StyleInfo createSLDStyle(
            @NonNull String workspaceName, @NonNull String name, @NonNull String requestBody) {
        StyleInfo info =
                new StyleInfo()
                        .name(name)
                        .filename(name + ".sld")
                        .format(FormatEnum.SLD)
                        .workspace(new WorkspaceInfo().name(workspaceName));
        info = createStyleInfo(info);
        api().uploadStyleSLD(workspaceName, name, requestBody.getBytes(UTF_8), false);
        return info;
    }

    public StyleInfo createStyle(@NonNull StyleInfo info, @NonNull String requestBody) {
        if (null == info.getFormat()) {
            throw new IllegalArgumentException("StyleInfo format not provided");
        }
        if (info.getFormat() == FormatEnum.MBSTYLE) {
            requestBody = validateMapboxStyle(requestBody);
        }
        info = createStyleInfo(info);
        ExtendedStylesApi api = api();
        switch (info.getFormat()) {
            case MBSTYLE:
                if (null == info.getWorkspace()) {
                    api.uploadStyleMapbox(info.getName(), requestBody.getBytes(UTF_8), false);
                } else {
                    api.uploadStyleMapbox(
                            info.getWorkspace().getName(),
                            info.getName(),
                            requestBody.getBytes(UTF_8),
                            false);
                }
                break;
            case SLD:
                if (null == info.getWorkspace()) {
                    api.uploadStyleSLD(info.getName(), requestBody.getBytes(UTF_8), false);
                } else {
                    api.uploadStyleSLD(
                            info.getWorkspace().getName(),
                            info.getName(),
                            requestBody.getBytes(UTF_8),
                            false);
                }
                break;
            default:
                throw new UnsupportedOperationException(
                        "Unknown or unsupported style output format: " + info.getFormat());
        }
        return info;
    }

    private @NonNull String validateMapboxStyle(@NonNull String requestBody) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode doc;
        try {
            doc = mapper.readTree(requestBody);
        } catch (IOException e) {
            throw new IllegalArgumentException("Malformed JSON document");
        }
        JsonNode atLayers = doc.at("/layers");
        if (!atLayers.isMissingNode()) {
            if (!(atLayers instanceof ArrayNode)) {
                throw new IllegalArgumentException(
                        "layers should be a JSON array, got " + atLayers);
            }
            ArrayNode layers = (ArrayNode) atLayers;
            if (layers.size() > 1) {
                layers.forEach(
                        layerNode -> {
                            JsonNode sourceLayer = layerNode.get("source-layer");
                            if (!(sourceLayer instanceof TextNode)
                                    || ((TextNode) sourceLayer).asText().isEmpty()) {
                                throw new IllegalArgumentException(
                                        "Every layer on a multi-layer style must contain a source-layer property");
                            }
                        });
            }
        }
        return requestBody;
    }

    public StyleInfo createStyleInfo(@NonNull StyleInfo info) {
        Objects.requireNonNull(info.getName());
        Objects.requireNonNull(info.getFormat());
        if (info.getFilename() == null) {
            switch (info.getFormat()) {
                case MBSTYLE:
                    info.setFilename(info.getName() + ".json");
                    break;
                case SLD:
                    info.setFilename(info.getName() + ".sld");
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "Unknown or unsupported style output format: " + info.getFormat());
            }
        }
        try {
            // passing null as name, it's taken from the StyleInfo itself
            String plainTextResponse =
                    api().createStyle(new StyleInfoWrapper().style(info), (String) null);
            log.debug("Created style {}. Response: {}", info.getName(), plainTextResponse);
        } catch (ServerException.InternalServerError ex) {
            throw ex;
        }
        if (info.getWorkspace() == null) {
            return get(info.getName());
        }
        return get(info.getWorkspace().getName(), info.getName());
    }

    /**
     * Deletes a style; the accompanying style body file is deleted from the filesystem and the
     * style reference removed from any layer that points to it
     *
     * @param name Name of the style to delete
     */
    public void delete(@NonNull String name) {
        delete(name, true, true);
    }

    /**
     * Deletes a style from a workspace; the accompanying style body file is deleted from the
     * filesystem and the style reference removed from any layer that points to it
     *
     * @param workspace Name of the workspace containing the style
     * @param name Name of the style to delete
     */
    public void delete(@NonNull String workspace, @NonNull String name) {
        delete(workspace, name, true, true);
    }

    /**
     * Delete a style on a Workspace.
     *
     * @param style Name of the style to delete
     * @param deleteFile Specifies whether the underlying file containing the style should be
     *     deleted on disk. (optional, default to false)
     * @param recurse Removes references to the specified style in existing layers. (optional,
     *     default to false)
     */
    public void delete(@NonNull String name, Boolean deleteFile, Boolean recurse) {
        api().deleteStyle(name, deleteFile, recurse);
    }

    /**
     * Delete a style on a Workspace.
     *
     * @param workspace Name of the workspace containing the style
     * @param style Name of the style to delete
     * @param deleteFile Specifies whether the underlying file containing the style should be
     *     deleted on disk. (optional, default to false)
     * @param recurse Removes references to the specified style in existing layers. (optional,
     *     default to false)
     */
    public void delete(
            @NonNull String workspace, @NonNull String name, Boolean deleteFile, Boolean recurse) {
        api().deleteStyleByWorkspace(workspace, name, deleteFile, recurse);
    }

    public void update(String name, StyleInfo style) {
        api().update(name, new StyleInfoWrapper().style(style));
    }

    public void update(String workspace, String name, StyleInfo style) {
        api().update(workspace, name, new StyleInfoWrapper().style(style));
    }

    public void updateBody(@NonNull StyleInfo style, @NonNull String body) {
        ExtendedStylesApi api = api();
        switch (style.getFormat()) {
            case MBSTYLE:
                body = validateMapboxStyle(body);
                if (null == style.getWorkspace()) {
                    api.uploadStyleMapbox(style.getName(), body.getBytes(UTF_8), false);
                } else {
                    api.uploadStyleMapbox(
                            style.getWorkspace().getName(),
                            style.getName(),
                            body.getBytes(UTF_8),
                            false);
                }
                break;
            case SLD:
                if (null == style.getWorkspace()) {
                    api.uploadStyleSLD(style.getName(), body.getBytes(UTF_8), false);
                } else {
                    api.uploadStyleSLD(
                            style.getWorkspace().getName(),
                            style.getName(),
                            body.getBytes(UTF_8),
                            false);
                }
                break;
            default:
                throw new UnsupportedOperationException(
                        "Unknown or unsupported style output format: " + style.getFormat());
        }
    }
}
