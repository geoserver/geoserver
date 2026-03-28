document.addEventListener("DOMContentLoaded", () => {
    // --- Constants ---
    const GEOM_SIZE_THRESHOLD = 100000;    // ~100 KB of coordinate JSON before falling back to bbox
    const FEATURE_INFO_COUNT = 10;
    const DEBUG_COPY_FEEDBACK_MS = 1500;
    const CUSTOM_PARAMS_DEBOUNCE_MS = 800;
    // OGC standard pixel size (0.28 mm) used for scale computation
    const OGC_PIXEL_SIZE_MM = 0.28;
    const MM_PER_INCH = 25.4;
    const INCHES_PER_METER = 39.37;

    const elements = {
        pureCoverage: document.getElementById("pureCoverage"),
        minimumX: document.getElementById("minX"),
        minimumY: document.getElementById("minY"),
        maximumX: document.getElementById("maxX"),
        maximumY: document.getElementById("maxY"),
        antialiasSelector: document.getElementById("antialiasSelector"),
        supportsFiltering: document.getElementById("supportsFiltering"),
        filterType: document.getElementById("filterType"),
        filter: document.getElementById("filter"),
        updateFilterButton: document.getElementById("updateFilterButton"),
        resetFilterButton: document.getElementById("resetFilterButton"),
        location: document.getElementById("location"),
        baseUrl: document.getElementById("baseUrl"),
        servicePath: document.getElementById("servicePath"),
        spatialReferenceSystem: document.getElementById("SRS"),
        units: document.getElementById("units"),
        global: document.getElementById("global"),
        axisOrderYx: document.getElementById("yx"),
        scale: document.getElementById("scale"),
        map: document.getElementById("map"),
        wmsVersionSelector: document.getElementById("wmsVersionSelector"),
        tilingModeSelector: document.getElementById("tilingModeSelector"),
        imageFormatSelector: document.getElementById("imageFormatSelector"),
        styleSelector: document.getElementById("styleSelector"),
        popupContainer: document.getElementById("popup"),
        popupContent: document.getElementById("popup-content"),
        popupCloser: document.getElementById("popup-closer"),
        debugToggle: document.getElementById("debug-mode-toggle"),
        debugTilePopup: document.getElementById("debug-tile-popup"),
        debugCopyBtn: document.getElementById("debug-copy-btn"),
        debugLayerType: document.getElementById("debug-layer-type"),
        customParamsInput: document.getElementById("customParamsInput"),
        sidebarToggle: document.getElementById("sidebar-menu"),
        sidebarContent: document.getElementById("sidebar-content"),
    };

    const escapeHtml = (str) => String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");

    const pureCoverage = elements.pureCoverage?.value === "true";
    const format = pureCoverage ? "image/jpeg" : "image/png";
    const bounds = [
        parseFloat(elements.minimumX.value),
        parseFloat(elements.minimumY.value),
        parseFloat(elements.maximumX.value),
        parseFloat(elements.maximumY.value),
    ];

    if (pureCoverage) {
        if (elements.antialiasSelector) {
            elements.antialiasSelector.disabled = true;
        }
        if (elements.imageFormatSelector) {
            elements.imageFormatSelector.value = "image/jpeg";
        }
    }

    const supportsFiltering = elements.supportsFiltering?.value === "true";
    if (!supportsFiltering) {
        if (elements.filterType) elements.filterType.disabled = true;
        if (elements.filter) elements.filter.disabled = true;
        if (elements.updateFilterButton) {
            elements.updateFilterButton.disabled = true;
        }
        if (elements.resetFilterButton) {
            elements.resetFilterButton.disabled = true;
        }
    }

    const mousePositionControl = new ol.control.MousePosition({
        className: "custom-mouse-position",
        target: elements.location,
        coordinateFormat: ol.coordinate.createStringXY(5),
        undefinedHTML: "&nbsp;",
    });

    const untiledParameters = { FORMAT: format, VERSION: "1.1.1" };
    const tiledParameters = {
        FORMAT: format,
        VERSION: "1.1.1",
        tiled: true,
        tilesOrigin: `${elements.minimumX.value},${elements.minimumY.value}`,
    };

    document.querySelectorAll("input.param").forEach((input) => {
        untiledParameters[input.title] = input.value;
        tiledParameters[input.title] = input.value;
    });

    const urlParams = new URLSearchParams(window.location.search);
    const RESERVED_KEYS = new Set([
        "service",
        "version",
        "request",
        "layers",
        "bbox",
        "width",
        "height",
        "srs",
        "styles",
        "format",
        "tiled",
        "transparent",
        "cql_filter",
        "filter",
        "featureid",
        "format_options",
    ]);

    const FILTER_TYPE_TO_PARAM = { cql: "CQL_FILTER", ogc: "FILTER", fid: "FEATUREID" };

    let appliedCustomParams = {};
    let initialCustomParamsStr = "";

    urlParams.forEach((value, key) => {
        if (!RESERVED_KEYS.has(key.toLowerCase())) {
            const upperKey = key.toUpperCase();
            untiledParameters[upperKey] = value;
            tiledParameters[upperKey] = value;
            appliedCustomParams[upperKey] = value;
            initialCustomParamsStr += `${key}=${value}&`;
        }
    });

    if (initialCustomParamsStr.length > 0) {
        initialCustomParamsStr = initialCustomParamsStr.slice(0, -1);
        if (elements.customParamsInput) {
            elements.customParamsInput.value = initialCustomParamsStr;
        }
    }

    const getUrlParamCaseInsensitive = (paramName) => {
        const key = Array.from(urlParams.keys()).find(
            (k) => k.toLowerCase() === paramName.toLowerCase(),
        );
        return key ? urlParams.get(key) : null;
    };

    const initVersion = getUrlParamCaseInsensitive("version");
    if (initVersion && elements.wmsVersionSelector) {
        const normalizedVersion = initVersion.startsWith("1.1")
            ? "1.1.1"
            : initVersion;

        const isValidOption = Array.from(elements.wmsVersionSelector.options)
            .some((opt) => opt.value === normalizedVersion);
        const finalVersion = isValidOption ? normalizedVersion : "1.1.1";

        elements.wmsVersionSelector.value = finalVersion;
        untiledParameters["VERSION"] = finalVersion;
        tiledParameters["VERSION"] = finalVersion;
        if (finalVersion === "1.3.0") {
            tiledParameters["tilesOrigin"] = `${bounds[1]},${bounds[0]}`;
        }
    }

    const initStyles = getUrlParamCaseInsensitive("styles");
    if (initStyles !== null && elements.styleSelector) {
        elements.styleSelector.value = initStyles;
        untiledParameters["STYLES"] = initStyles;
        tiledParameters["STYLES"] = initStyles;
    }

    const initFormatOptions = getUrlParamCaseInsensitive("format_options");
    if (initFormatOptions && elements.antialiasSelector) {
        const antialiasMatch = initFormatOptions.match(
            /antialias:(full|text|none)/i,
        );
        if (antialiasMatch) {
            elements.antialiasSelector.value = antialiasMatch[1].toLowerCase();
            untiledParameters["FORMAT_OPTIONS"] = initFormatOptions;
            tiledParameters["FORMAT_OPTIONS"] = initFormatOptions;
        }
    }

    const initTiled = getUrlParamCaseInsensitive("tiled");
    if (initTiled && elements.tilingModeSelector) {
        elements.tilingModeSelector.value = initTiled.toLowerCase() === "true"
            ? "tiled"
            : "untiled";
    }

    if (supportsFiltering) {
        const initCql = getUrlParamCaseInsensitive("cql_filter");
        const initOgc = getUrlParamCaseInsensitive("filter");
        const initFid = getUrlParamCaseInsensitive("featureid");

        let activeFilterType = "cql";
        let activeFilterValue = "";

        if (initCql) {
            activeFilterType = "cql";
            activeFilterValue = initCql;
        } else if (initOgc) {
            activeFilterType = "ogc";
            activeFilterValue = initOgc;
        } else if (initFid) {
            activeFilterType = "fid";
            activeFilterValue = initFid;
        }

        if (activeFilterValue && elements.filterType && elements.filter) {
            elements.filterType.value = activeFilterType;
            elements.filter.value = activeFilterValue;

            const paramKey = FILTER_TYPE_TO_PARAM[activeFilterType];

            untiledParameters[paramKey] = activeFilterValue;
            tiledParameters[paramKey] = activeFilterValue;
        }
    }

    const serviceUrl =
        `${elements.baseUrl.value}/${elements.servicePath.value}`;
    const startTiled = elements.tilingModeSelector?.value === "tiled";

    const untiledLayer = new ol.layer.Image({
        visible: !startTiled,
        source: new ol.source.ImageWMS({
            ratio: 1,
            url: serviceUrl,
            params: untiledParameters,
        }),
    });

    const tiledLayer = new ol.layer.Tile({
        visible: startTiled,
        source: new ol.source.TileWMS({
            url: serviceUrl,
            params: tiledParameters,
        }),
    });

    const srsCode = elements.spatialReferenceSystem.value;
    const requestedGlobal = elements.global.value === "true";
    const projectionUnits = elements.units.value;

    const crsMinX = document.getElementById("crsMinX")?.value;
    const crsMinY = document.getElementById("crsMinY")?.value;
    const crsMaxX = document.getElementById("crsMaxX")?.value;
    const crsMaxY = document.getElementById("crsMaxY")?.value;

    let projection = ol.proj.get(srsCode);

    if (!projection) {
        let worldExtent = null;
        let safeGlobal = requestedGlobal;

        if (crsMinX && crsMinY && crsMaxX && crsMaxY) {
            const wgs84Extent = [
                parseFloat(crsMinX),
                parseFloat(crsMinY),
                parseFloat(crsMaxX),
                parseFloat(crsMaxY),
            ];

            if (srsCode !== "EPSG:4326") {
                worldExtent = ol.proj.transformExtent(
                    wgs84Extent,
                    "EPSG:4326",
                    srsCode,
                );
            } else {
                worldExtent = wgs84Extent;
            }
        } else if (requestedGlobal) {
            if (projectionUnits === "degrees") {
                worldExtent = [-180, -90, 180, 90];
            } else if (
                projectionUnits === "m" &&
                (srsCode.includes("3857") || srsCode.includes("900913"))
            ) {
                worldExtent = [
                    -20037508.34,
                    -20037508.34,
                    20037508.34,
                    20037508.34,
                ];
            } else {
                safeGlobal = false;
                console.warn(
                    `Wrapping disabled: No global extent defined for ${srsCode}`,
                );
            }
        }

        projection = new ol.proj.Projection({
            code: srsCode,
            units: projectionUnits,
            global: safeGlobal,
            extent: worldExtent,
            ...(elements.axisOrderYx.value === "true" &&
                { axisOrientation: "neu" }),
        });
    }

    const defaultControls = ol.control.defaults.defaults({ attribution: false });

    const layerWidth = bounds[2] - bounds[0];
    const layerHeight = bounds[3] - bounds[1];
    const fallbackMaxResolution = Math.max(layerWidth, layerHeight) / 256;

    const map = new ol.Map({
        controls: defaultControls.extend([mousePositionControl]),
        target: "map",
        layers: [untiledLayer, tiledLayer],
        view: new ol.View({
            projection: projection,
            center: [
                bounds[0] + (layerWidth / 2),
                bounds[1] + (layerHeight / 2),
            ],
            maxResolution: fallbackMaxResolution,
            multiWorld: true,
            constrainResolution: true,
            maxZoom: 22,
            zoom: 1,
        }),
    });

    map.getView().fit(bounds, {
        size: map.getSize(),
        padding: [50, 50, 50, 50],
    });

    const popupOverlay = new ol.Overlay({
        element: elements.popupContainer,
        positioning: "bottom-center",
        stopEvent: true,
        autoPan: { animation: { duration: 250 } },
    });
    map.addOverlay(popupOverlay);

    const highlightSource = new ol.source.Vector();
    const highlightLayer = new ol.layer.Vector({
        source: highlightSource,
        style: new ol.style.Style({
            stroke: new ol.style.Stroke({
                color: "rgba(44, 181, 232, 1)",
                width: 3,
            }),
            fill: new ol.style.Fill({
                color: "rgba(44, 181, 232, 0.2)",
            }),
            image: new ol.style.Circle({
                radius: 6,
                stroke: new ol.style.Stroke({
                    color: "rgba(44, 181, 232, 1)",
                    width: 2,
                }),
                fill: new ol.style.Fill({
                    color: "rgba(44, 181, 232, 0.2)",
                }),
            }),
        }),
        zIndex: 999,
    });
    map.addLayer(highlightLayer);

    const tileDebugLayer = new ol.layer.Tile({
        visible: false,
        source: new ol.source.TileDebug({
            projection: projection,
            tileGrid: tiledLayer.getSource().getTileGrid() ||
                ol.tilegrid.getForProjection(projection),
            template: " ",
        }),
        zIndex: 998,
    });
    map.addLayer(tileDebugLayer);

    let currentDebugUrl = "";
    const debugOverlay = new ol.Overlay({
        element: elements.debugTilePopup,
        positioning: "center-center",
        stopEvent: true,
    });
    map.addOverlay(debugOverlay);

    elements.popupCloser?.addEventListener("click", (event) => {
        event.preventDefault();
        popupOverlay.setPosition(undefined);
        highlightSource.clear();
        elements.popupCloser.blur();
    });

    map.getView().on("change:resolution", (event) => {
        const resolution = event.target.get("resolution");
        const currentProjection = map.getView().getProjection();
        const metersPerUnit = currentProjection.getMetersPerUnit();
        const dotsPerInch = MM_PER_INCH / OGC_PIXEL_SIZE_MM;
        let scale = resolution * metersPerUnit * INCHES_PER_METER * dotsPerInch;

        if (scale >= 9500 && scale <= 950000) {
            scale = `${Math.round(scale / 1000)}K`;
        } else if (scale >= 950000) {
            scale = `${Math.round(scale / 1000000)}M`;
        } else {
            scale = Math.round(scale);
        }
        if (elements.scale) elements.scale.textContent = `Scale = 1 : ${scale}`;
    });

    const geojsonFormat = new ol.format.GeoJSON();

    map.on("singleclick", (event) => {
        if (elements.debugToggle?.checked) return;

        popupOverlay.setPosition(undefined);
        if (elements.popupContent) {
            elements.popupContent.innerHTML =
                `<p class="popup-status">Loading...</p>`;
        }

        const view = map.getView();
        const source = untiledLayer.getVisible()
            ? untiledLayer.getSource()
            : tiledLayer.getSource();

        const url = source.getFeatureInfoUrl(
            event.coordinate,
            view.getResolution(),
            view.getProjection(),
            { "INFO_FORMAT": "application/json", "FEATURE_COUNT": FEATURE_INFO_COUNT },
        );

        if (url) {
            fetch(url)
                .then((response) => {
                    if (!response.ok) {
                        throw new Error("Network response was not ok");
                    }
                    const contentType = response.headers.get("content-type");
                    if (
                        !contentType ||
                        !contentType.includes("application/json")
                    ) {
                        throw new Error(
                            "Format application/json is not supported by this layer.",
                        );
                    }
                    return response.json();
                })
                .then((data) => {
                    highlightSource.clear();

                   if (data.features && data.features.length > 0) {
                        const safeFeaturesToRender = [];
                        const viewProjection = map.getView().getProjection();

                        data.features.forEach((feature) => {
                            if (!feature.geometry) {
                                safeFeaturesToRender.push(new ol.Feature({
                                    geometry: new ol.geom.Point(event.coordinate)
                                }));
                                return;
                            }

                            const geomStringLength = JSON.stringify(feature.geometry.coordinates).length;

                            if (geomStringLength > GEOM_SIZE_THRESHOLD) {
                                console.warn(`Geometry for ${feature.id} is too large (${geomStringLength} chars). Using fallback shape.`);
                                
                                if (feature.bbox) {
                                    const transformedBbox = ol.proj.transformExtent(feature.bbox, "EPSG:4326", viewProjection);
                                    safeFeaturesToRender.push(new ol.Feature({
                                        geometry: ol.geom.Polygon.fromExtent(transformedBbox)
                                    }));
                                } else {
                                    safeFeaturesToRender.push(new ol.Feature({
                                        geometry: new ol.geom.Point(event.coordinate)
                                    }));
                                }
                            } else {
                                safeFeaturesToRender.push(geojsonFormat.readFeature(feature, {
                                    featureProjection: viewProjection
                                }));
                            }
                        });

                        highlightSource.addFeatures(safeFeaturesToRender);

                        let html =
                            `<h4 class="popup-title">Found ${data.features.length} Features</h4>`;

                        data.features.forEach((feature) => {
                            html += `<div class="popup-feature-section">`;
                            html +=
                                `<div class="popup-feature-id">${escapeHtml(feature.id)}</div>`;
                            html += `<table class="popup-attribute-table">`;

                            for (
                                const [key, value] of Object.entries(
                                    feature.properties ?? {},
                                )
                            ) {
                                if (key !== "geometry" && key !== "bbox") {
                                    html += `<tr>
                                                <th>${escapeHtml(key)}</th>
                                                <td>${
                                        value !== null ? escapeHtml(value) : ""
                                    }</td>
                                            </tr>`;
                                }
                            }
                            html += `</table></div>`;
                        });

                        if (elements.popupContent) {
                            elements.popupContent.innerHTML = html;
                        }
                        popupOverlay.setPosition(event.coordinate);
                    } else {
                        if (elements.popupContent) {
                            elements.popupContent.innerHTML =
                                `<p class="popup-status">No features found here.</p>`;
                        }
                        popupOverlay.setPosition(event.coordinate);
                    }
                })
                .catch((error) => {
                    console.error("Error fetching feature info:", error);
                    if (elements.popupContent) {
                        elements.popupContent.innerHTML =
                            "<p>Error loading data.</p>";
                    }
                    popupOverlay.setPosition(event.coordinate);
                });
        }
    });

    elements.debugCopyBtn?.addEventListener("click", () => {
        if (!currentDebugUrl) return;
        navigator.clipboard.writeText(currentDebugUrl).then(() => {
            const originalText = elements.debugCopyBtn.textContent;
            elements.debugCopyBtn.textContent = "✅ Copied!";
            setTimeout(
                () => elements.debugCopyBtn.textContent = originalText,
                DEBUG_COPY_FEEDBACK_MS,
            );
        }).catch((err) => console.error("Failed to copy!", err));
    });

    elements.debugToggle?.addEventListener("change", (event) => {
        const isDebug = event.target.checked;
        const isTiled = tiledLayer.getVisible();

        if (elements.debugTilePopup) {
            elements.debugTilePopup.classList.toggle("hidden", !isDebug);
        }
        tileDebugLayer.setVisible(isDebug && isTiled);

        if (!isDebug) {
            highlightSource.clear();
            debugOverlay.setPosition(undefined);
        }
    });

    map.on("pointermove", (event) => {
        if (!elements.debugToggle?.checked || event.dragging) return;
        if (!(event.originalEvent.target instanceof HTMLCanvasElement)) return;

        const isTiled = tiledLayer.getVisible();
        const source = isTiled
            ? tiledLayer.getSource()
            : untiledLayer.getSource();
        const view = map.getView();
        const projection = view.getProjection();
        const resolution = view.getResolution();

        highlightSource.clear();
        let extentToHighlight = null;

        if (isTiled) {
            if (elements.debugLayerType) {
                elements.debugLayerType.textContent = "TileWMS URL";
            }
            const tileGrid = source.getTileGrid() ||
                ol.tilegrid.getForProjection(projection);
            const z = tileGrid.getZForResolution(resolution);
            const tileCoord = tileGrid.getTileCoordForCoordAndZ(
                event.coordinate,
                z,
            );

            extentToHighlight = tileGrid.getTileCoordExtent(tileCoord);
            const tileUrlFunction = source.getTileUrlFunction();
            currentDebugUrl = tileUrlFunction(tileCoord, 1, projection);
        } else {
            if (elements.debugLayerType) {
                elements.debugLayerType.textContent = "ImageWMS Full BBOX";
            }
            const size = map.getSize();
            extentToHighlight = view.calculateExtent(size);

            const params = source.getParams();
            const baseUrl = source.getUrl();
            const urlObj = new URL(
                baseUrl.startsWith("http")
                    ? baseUrl
                    : window.location.origin + baseUrl,
            );

            urlObj.searchParams.set("SERVICE", "WMS");
            urlObj.searchParams.set("REQUEST", "GetMap");
            urlObj.searchParams.set("TRANSPARENT", "true");

            for (const [key, value] of Object.entries(params)) {
                if (value !== undefined && value !== null) {
                    urlObj.searchParams.set(key.toUpperCase(), value);
                }
            }

            const is130 = (params.VERSION || "1.1.1") === "1.3.0";
            let bbox = extentToHighlight;

            if (is130 && projection.getAxisOrientation().startsWith("ne")) {
                bbox = [
                    extentToHighlight[1],
                    extentToHighlight[0],
                    extentToHighlight[3],
                    extentToHighlight[2],
                ];
            }

            urlObj.searchParams.set("BBOX", bbox.join(","));
            urlObj.searchParams.set("WIDTH", Math.round(size[0]));
            urlObj.searchParams.set("HEIGHT", Math.round(size[1]));
            urlObj.searchParams.set(
                is130 ? "CRS" : "SRS",
                projection.getCode(),
            );

            currentDebugUrl = urlObj.toString();
        }

        if (extentToHighlight) {
            const polygon = ol.geom.Polygon.fromExtent(extentToHighlight);
            highlightSource.addFeature(new ol.Feature(polygon));

            const centerX = (extentToHighlight[0] + extentToHighlight[2]) / 2;
            const centerY = (extentToHighlight[1] + extentToHighlight[3]) / 2;
            debugOverlay.setPosition([centerX, centerY]);
        }
    });

    const updateParametersOnAllLayers = (parameters) => {
        untiledLayer.getSource().updateParams(parameters);
        tiledLayer.getSource().updateParams(parameters);
    };

    const updateFilter = () => {
        if (!supportsFiltering) return;

        const filterType = elements.filterType?.value;
        const filterValue = elements.filter?.value.trim();

        const filterParameters = {
            "FILTER": undefined,
            "CQL_FILTER": undefined,
            "FEATUREID": undefined,
        };

        const currentUrl = new URL(window.location.href);
        const filterKeys = ["filter", "cql_filter", "featureid"];
        filterKeys.forEach((key) => {
            currentUrl.searchParams.delete(key);
            currentUrl.searchParams.delete(key.toUpperCase());
        });

        if (filterValue !== "") {
            const activeParam = FILTER_TYPE_TO_PARAM[filterType];
            if (activeParam) {
                filterParameters[activeParam] = filterValue;
                currentUrl.searchParams.set(activeParam, filterValue);
            }
        }

        updateParametersOnAllLayers(filterParameters);
        window.history.replaceState({}, "", currentUrl.toString());
    };

    const updateBrowserUrlParam = (key, value) => {
        const currentUrl = new URL(window.location.href);

        currentUrl.searchParams.delete(key);
        currentUrl.searchParams.delete(key.toLowerCase());
        currentUrl.searchParams.delete(key.toUpperCase());

        if (value !== null && value !== "") {
            const finalKey = key.toLowerCase() === "tiled"
                ? "tiled"
                : key.toUpperCase();
            currentUrl.searchParams.set(finalKey, value);
        } else if (key.toUpperCase() === "STYLES") {
            currentUrl.searchParams.set("STYLES", "");
        }

        window.history.replaceState({}, "", currentUrl.toString());
    };

    elements.wmsVersionSelector?.addEventListener("change", (event) => {
        const wmsVersion = event.target.value;
        updateParametersOnAllLayers({ "VERSION": wmsVersion });
        updateBrowserUrlParam("VERSION", wmsVersion);

        const origin = wmsVersion === "1.3.0"
            ? `${bounds[1]},${bounds[0]}`
            : `${bounds[0]},${bounds[1]}`;
        tiledLayer.getSource().updateParams({ "tilesOrigin": origin });
    });

    elements.tilingModeSelector?.addEventListener("change", (event) => {
        const isTiled = event.target.value === "tiled";
        untiledLayer.setVisible(!isTiled);
        tiledLayer.setVisible(isTiled);
        updateBrowserUrlParam("tiled", isTiled ? "true" : "false");

        if (elements.debugToggle?.checked) {
            tileDebugLayer.setVisible(isTiled);
            highlightSource.clear();
            debugOverlay.setPosition(undefined);
        }
    });

    elements.antialiasSelector?.addEventListener("change", (event) => {
        const formatOptionVal = `antialias:${event.target.value}`;
        updateParametersOnAllLayers({ "FORMAT_OPTIONS": formatOptionVal });
        updateBrowserUrlParam("FORMAT_OPTIONS", formatOptionVal);
    });

    elements.imageFormatSelector?.addEventListener("change", (event) => {
        updateParametersOnAllLayers({ "FORMAT": event.target.value });
    });

    elements.styleSelector?.addEventListener("change", (event) => {
        const styleVal = event.target.value;
        updateParametersOnAllLayers({ "STYLES": styleVal });
        updateBrowserUrlParam("STYLES", styleVal);
    });

    elements.updateFilterButton?.addEventListener("click", updateFilter);

    elements.resetFilterButton?.addEventListener("click", () => {
        if (!supportsFiltering) return;
        if (elements.filter) elements.filter.value = "";
        updateFilter();
    });

    let customParamsDebounceTimer;

    const applyCustomParams = () => {
        if (!elements.customParamsInput) return;
        const paramStr = elements.customParamsInput.value.trim();
        const newParamsToSend = {};

        const currentUrl = new URL(window.location.href);

        Object.keys(appliedCustomParams).forEach((key) => {
            newParamsToSend[key] = undefined;
            currentUrl.searchParams.delete(key);
            currentUrl.searchParams.delete(key.toLowerCase());
        });

        if (paramStr !== "") {
            const parsed = new URLSearchParams(paramStr);
            parsed.forEach((value, key) => {
                if (!RESERVED_KEYS.has(key.toLowerCase())) {
                    const upperKey = key.toUpperCase();
                    newParamsToSend[upperKey] = value;
                    currentUrl.searchParams.set(key, value);
                } else {
                    console.warn(
                        `Blocked attempt to override protected WMS parameter: ${key}`,
                    );
                }
            });
        }

        window.history.replaceState({}, "", currentUrl.toString());

        appliedCustomParams = {};
        Object.keys(newParamsToSend).forEach((key) => {
            if (newParamsToSend[key] !== undefined) {
                appliedCustomParams[key] = newParamsToSend[key];
            }
        });

        updateParametersOnAllLayers(newParamsToSend);
    };

    elements.customParamsInput?.addEventListener("input", () => {
        clearTimeout(customParamsDebounceTimer);
        customParamsDebounceTimer = setTimeout(applyCustomParams, CUSTOM_PARAMS_DEBOUNCE_MS);
    });

    elements.customParamsInput?.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            clearTimeout(customParamsDebounceTimer);
            applyCustomParams();
        }
    });

    if (elements.map) {
        new ResizeObserver(() => map.updateSize()).observe(elements.map);
    }

    const toggleSidebar = (forceOpen) => {
        const isOpen = forceOpen !== undefined
            ? forceOpen
            : !elements.sidebarContent.classList.contains("is-open");
        elements.sidebarContent?.classList.toggle("is-open", isOpen);
        elements.sidebarToggle?.setAttribute("aria-expanded", isOpen.toString());
        document.body.style.overflow = isOpen ? "hidden" : "";
    };

    elements.sidebarToggle?.addEventListener("click", () => toggleSidebar());

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && elements.sidebarContent?.classList.contains("is-open")) {
            toggleSidebar(false);
        }
    });
});
