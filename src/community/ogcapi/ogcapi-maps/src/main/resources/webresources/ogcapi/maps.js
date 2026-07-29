// OpenLayers 10.8 preview for the OGC API - Maps map endpoint. The map layer is the native
// ol/source/OGCMap driving /collections/{id}/map directly; feature info uses the GeoServer
// /map/info extension, requested by hand since it is not part of the standard.
document.addEventListener("DOMContentLoaded", () => {
    const OGC_PIXEL_SIZE_MM = 0.28;
    const MM_PER_INCH = 25.4;
    const INCHES_PER_METER = 39.37;
    const FEATURE_INFO_LIMIT = 50;
    const DEFAULT_FILTER_LANG = "cql2-text";
    // request keys OGCMap sets on its own (or that live in the request path), never passed as user params
    const OGC_MANAGED = new Set(["layers", "styles", "crs", "bbox-crs", "bbox", "width", "height", "f"]);

    const el = (id) => document.getElementById(id);
    const escapeHtml = (str) => String(str)
        .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;").replace(/'/g, "&#039;");

    // the preview is served at the collection (or styled) map resource, so its own URL, without the query, is the
    // OGC API - Maps /map endpoint that both the OGCMap source and the feature info requests build on
    const mapUrl = el("url").value.split("?")[0];
    // CRS reference (SafeCURIE) and axis order come from the server, used for the hand-built feature info request
    const crsCurie = el("crsCurie").value;
    const swapAxes = el("axisOrder").value === "NORTH_EAST";

    const bounds = [
        parseFloat(el("minX").value),
        parseFloat(el("minY").value),
        parseFloat(el("maxX").value),
        parseFloat(el("maxY").value),
    ];

    const srsCode = el("SRS").value;
    let projection = ol.proj.get(srsCode);
    if (!projection) {
        projection = new ol.proj.Projection({ code: srsCode, units: el("units").value });
        ol.proj.addProjection(projection);
    }

    // GeoServer vendor parameters carried from the request (filter, format options, ...); the OGC-managed keys are
    // skipped, OGCMap sets those itself. They are tracked here so the feature info request can carry them too.
    let imageFormat = "image/png";
    const vendorParams = {};
    document.querySelectorAll("input.param").forEach((input) => {
        if (!OGC_MANAGED.has(input.title.toLowerCase())) vendorParams[input.title] = input.value;
    });

    // a filter that came with the request is shown in the toolbar, so it can be edited instead of retyped
    if (vendorParams.filter) {
        el("filter").value = vendorParams.filter;
        el("filterType").value = vendorParams["filter-lang"] || DEFAULT_FILTER_LANG;
    }

    // the bbox in the axis order the CRS authority declares (OpenLayers works in x,y), shared by the tiled map and
    // the feature info requests
    const bboxParam = (extent) => (swapAxes
        ? [extent[1], extent[0], extent[3], extent[2]]
        : [extent[0], extent[1], extent[2], extent[3]]).join(",");

    // OGCMap emits the crs/bbox-crs in plain "EPSG:4326" form with a bbox already in the CRS authority axis order;
    // swap in the SafeCURIE reference from the server, so it reads the bbox in that same authority order
    const withCrsCurie = (src) =>
        src.replace(/([?&](?:bbox-crs|crs)=)[^&]*/g, `$1${encodeURIComponent(crsCurie)}`);

    // untiled: the native OGC API - Maps source, one image for the whole viewport
    const source = new ol.source.OGCMap({
        url: mapUrl,
        params: { f: imageFormat, ...vendorParams },
        projection,
        imageLoadFunction: (image, src) => {
            image.getImage().src = withCrsCurie(src);
        },
    });
    const untiledLayer = new ol.layer.Image({ source: source });

    // tiled: one /map request per tile, the OGC API - Maps counterpart of a tiled WMS
    const tileGrid = ol.tilegrid.createForProjection(projection, 22, [256, 256]);
    const tileUrl = (tileCoord) => {
        const extent = tileGrid.getTileCoordExtent(tileCoord);
        const [width, height] = ol.size.toSize(tileGrid.getTileSize(tileCoord[0]));
        const query = new URLSearchParams({
            f: imageFormat, bbox: bboxParam(extent), crs: crsCurie, "bbox-crs": crsCurie, width, height,
        });
        Object.entries(vendorParams).forEach(([key, value]) => {
            if (value != null && !OGC_MANAGED.has(key.toLowerCase())) query.set(key, value);
        });
        return `${mapUrl}?${query.toString()}`;
    };
    const tiledSource = new ol.source.TileImage({ projection, tileGrid, tileUrlFunction: tileUrl });
    const tiledLayer = new ol.layer.Tile({ visible: false, source: tiledSource });

    // updates the vendor params on both sources at once
    const updateParams = (update) => {
        Object.assign(vendorParams, update);
        source.updateParams(update);
        tiledSource.refresh();
    };

    const mousePosition = new ol.control.MousePosition({
        className: "custom-mouse-position",
        target: el("location"),
        coordinateFormat: ol.coordinate.createStringXY(5),
        placeholder: "&nbsp;",
    });

    const map = new ol.Map({
        controls: ol.control.defaults.defaults({ attribution: false }).extend([mousePosition]),
        target: "map",
        layers: [untiledLayer, tiledLayer],
        view: new ol.View({ projection: projection, constrainResolution: true }),
    });
    map.getView().fit(bounds, { size: map.getSize(), padding: [20, 20, 20, 20] });
    new ResizeObserver(() => map.updateSize()).observe(el("map"));

    map.getView().on("change:resolution", (event) => {
        const resolution = event.target.get("resolution");
        const metersPerUnit = map.getView().getProjection().getMetersPerUnit();
        const dpi = MM_PER_INCH / OGC_PIXEL_SIZE_MM;
        let scale = resolution * metersPerUnit * INCHES_PER_METER * dpi;
        if (scale >= 9500 && scale <= 950000) scale = `${Math.round(scale / 1000)}K`;
        else if (scale >= 950000) scale = `${Math.round(scale / 1000000)}M`;
        else scale = Math.round(scale);
        el("scale").textContent = `Scale = 1 : ${scale}`;
    });

    // feature info popup
    const popupOverlay = new ol.Overlay({
        element: el("popup"),
        positioning: "bottom-center",
        stopEvent: true,
        autoPan: { animation: { duration: 250 } },
    });
    map.addOverlay(popupOverlay);

    const highlightSource = new ol.source.Vector();
    map.addLayer(new ol.layer.Vector({
        source: highlightSource,
        zIndex: 999,
        style: new ol.style.Style({
            stroke: new ol.style.Stroke({ color: "rgba(44, 181, 232, 1)", width: 3 }),
            fill: new ol.style.Fill({ color: "rgba(44, 181, 232, 0.2)" }),
            image: new ol.style.Circle({
                radius: 6,
                stroke: new ol.style.Stroke({ color: "rgba(44, 181, 232, 1)", width: 2 }),
                fill: new ol.style.Fill({ color: "rgba(44, 181, 232, 0.2)" }),
            }),
        }),
    }));

    el("popup-closer").addEventListener("click", (event) => {
        event.preventDefault();
        popupOverlay.setPosition(undefined);
        highlightSource.clear();
        el("popup-closer").blur();
    });

    const geojson = new ol.format.GeoJSON();

    // Builds the GeoServer /map/info request for a clicked pixel: the current map extent as the bbox (in the CRS
    // authority axis order the server expects), the map size, the pixel offset as i/j, and the active vendor params.
    const featureInfoUrl = (coordinate) => {
        const size = map.getSize();
        const extent = map.getView().calculateExtent(size);
        const [i, j] = map.getPixelFromCoordinate(coordinate).map(Math.round);
        const query = new URLSearchParams({
            f: "application/json",
            bbox: bboxParam(extent),
            crs: crsCurie,
            width: size[0],
            height: size[1],
            i: i,
            j: j,
            limit: FEATURE_INFO_LIMIT,
        });
        Object.entries(vendorParams).forEach(([key, value]) => {
            if (value != null && !OGC_MANAGED.has(key.toLowerCase())) query.set(key, value);
        });
        return `${mapUrl}/info?${query.toString()}`;
    };

    map.on("singleclick", (event) => {
        const infoUrl = featureInfoUrl(event.coordinate);
        el("popup-content").innerHTML = '<p class="popup-status">Loading...</p>';
        popupOverlay.setPosition(event.coordinate);

        fetch(infoUrl)
            .then((response) => {
                if (!response.ok) throw new Error("Feature info request failed");
                return response.json();
            })
            .then((data) => renderFeatureInfo(data, event.coordinate, map.getView().getProjection()))
            .catch((error) => {
                console.error("Error fetching feature info:", error);
                el("popup-content").innerHTML = "<p>Error loading data.</p>";
            });
    });

    const renderFeatureInfo = (data, coordinate, viewProjection) => {
        highlightSource.clear();
        const features = data.features || [];
        if (features.length === 0) {
            el("popup-content").innerHTML = '<p class="popup-status">No features found here.</p>';
            return;
        }
        features.forEach((feature) => {
            const geometry = feature.geometry
                ? geojson.readGeometry(feature.geometry, {
                      dataProjection: viewProjection,
                      featureProjection: viewProjection,
                  })
                : new ol.geom.Point(coordinate);
            highlightSource.addFeature(new ol.Feature({ geometry }));
        });

        let html = `<h4 class="popup-title">Found ${features.length} Features</h4>`;
        features.forEach((feature) => {
            html += '<div class="popup-feature-section">';
            html += `<div class="popup-feature-id">${escapeHtml(feature.id)}</div>`;
            html += '<table class="popup-attribute-table">';
            for (const [key, value] of Object.entries(feature.properties ?? {})) {
                html += `<tr><th>${escapeHtml(key)}</th><td>${value !== null ? escapeHtml(value) : ""}</td></tr>`;
            }
            html += "</table></div>";
        });
        el("popup-content").innerHTML = html;
    };

    // toolbar controls
    el("tilingModeSelector").addEventListener("change", (event) => {
        const tiled = event.target.value === "tiled";
        untiledLayer.setVisible(!tiled);
        tiledLayer.setVisible(tiled);
    });
    el("antialiasSelector").addEventListener("change", (event) => {
        updateParams({ FORMAT_OPTIONS: `antialias:${event.target.value}` });
    });
    el("imageFormatSelector").addEventListener("change", (event) => {
        imageFormat = event.target.value;
        updateParams({ f: imageFormat });
    });

    // the OGC API - Features - Part 3 filter parameters, applied by the server on top of bbox and datetime
    const updateFilter = () => {
        const filterValue = el("filter").value.trim();
        const language = el("filterType").value;
        updateParams(filterValue === ""
            ? { filter: undefined, "filter-lang": undefined }
            : { filter: filterValue, "filter-lang": language });
    };
    el("updateFilterButton").addEventListener("click", updateFilter);
    el("resetFilterButton").addEventListener("click", () => {
        el("filter").value = "";
        updateFilter();
    });

    // sidebar toggle
    const sidebarContent = el("sidebar-content");
    const sidebarToggle = el("sidebar-menu");
    const toggleSidebar = (forceOpen) => {
        const open = forceOpen !== undefined ? forceOpen : !sidebarContent.classList.contains("is-open");
        sidebarContent.classList.toggle("is-open", open);
        sidebarToggle.setAttribute("aria-expanded", open.toString());
    };
    sidebarToggle.addEventListener("click", () => toggleSidebar());
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && sidebarContent.classList.contains("is-open")) toggleSidebar(false);
    });
});
