/*

Copyright (c) 2008-2009, The Open Source Geospatial Foundation
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the Open Source Geospatial Foundation nor the names
      of its contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

Ext.namespace("GeoExt.ux.plugins");
GeoExt.ux.plugins.PrintProviderField = Ext.extend(Ext.util.Observable, {target: null,constructor: function(config) {
        this.initialConfig = config;
        Ext.apply(this, config);
        GeoExt.ux.plugins.PrintProviderField.superclass.constructor.apply(this, arguments);
    },init: function(target) {
        this.target = target;
        var onCfg = {scope: this,"render": this.onRender};
        onCfg[target instanceof Ext.form.ComboBox ? "select" : "valid"] = this.onFieldChange;
        target.on(onCfg);
    },onRender: function(field) {
        var printProvider = field.ownerCt.printProvider;
        if (field.store === printProvider.layouts) {
            field.setValue(printProvider.layout.get(field.displayField));
            printProvider.on({"layoutchange": function(printProvider, layout) {
                    field.setValue(layout.get(field.displayField));
                }});
        } else if (field.store === printProvider.dpis) {
            field.setValue(printProvider.dpi.get(field.displayField));
            printProvider.on({"dpichange": function(printProvider, dpi) {
                    field.setValue(dpi.get(field.displayField));
                }});
        } else {
            field.setValue(printProvider.customParams[field.name]);
        }
    },onFieldChange: function(field, record) {
        var printProvider = field.ownerCt.printProvider;
        var value = field.getValue();
        if (record) {
            switch (field.store) {
                case printProvider.layouts:
                    printProvider.setLayout(record);
                    break;
                case printProvider.dpis:
                    printProvider.setDpi(record);
            }
        } else {
            printProvider.customParams[field.name] = value;
        }
    }});
Ext.preg && Ext.preg("gx_printproviderfield", GeoExt.ux.plugins.PrintProviderField);
Ext.namespace("GeoExt.ux.plugins");
GeoExt.ux.plugins.PrintPageField = Ext.extend(Ext.util.Observable, {page: null,target: null,constructor: function(config) {
        this.initialConfig = config;
        Ext.apply(this, config);
        GeoExt.ux.plugins.PrintPageField.superclass.constructor.apply(this, arguments);
    },init: function(target) {
        this.target = target;
        onCfg = {scope: this};
        onCfg[target instanceof Ext.form.ComboBox ? "select" : "valid"] = this.onFieldChange;
        target.on(onCfg);
        this.page.on({"change": this.onPageChange,scope: this});
    },onFieldChange: function(field, record) {
        var printProvider = this.page.printProvider;
        var value = field.getValue();
        if (field.store === printProvider.scales) {
            this.page.setScale(record);
        } else if (field.name == "rotation") {
            this.page.setRotation(value);
        } else {
            this.page.customParams[field.name] = value;
        }
    },onPageChange: function(page) {
        var t = this.target;
        t.suspendEvents();
        if (t.store === page.printProvider.scales) {
            t.setValue(page.scale.get(t.displayField));
        } else if (t.name == "rotation") {
            t.setValue(page.rotation);
            t.setDisabled(!page.printProvider.layout.get("rotation"));
        }
        t.resumeEvents();
    }});
Ext.preg && Ext.preg("gx_printpagefield", GeoExt.ux.plugins.PrintPageField);
Ext.namespace("GeoExt");
GeoExt.PrintMapPanel = Ext.extend(GeoExt.MapPanel, {sourceMap: null,printProvider: null,printPage: null,initComponent: function() {
        this.layers = this.sourceMap.layers;
        this.extent = this.sourceMap.getExtent();
        GeoExt.PrintMapPanel.superclass.initComponent.call(this);
        this.printPage = new GeoExt.ux.data.PrintPage({printProvider: this.printProvider});
        this.printPage.fitPage(this.sourceMap);
        var extent = this.printPage.feature.geometry.getBounds();
        var resolution = this.map.getResolution();
        this.setSize(extent.getWidth() / resolution, extent.getHeight() / resolution);
    }});
Ext.namespace("GeoExt.ux.form")
GeoExt.ux.form.PrintForm = Ext.extend(Ext.form.FormPanel, {printProvider: null,map: null,layer: null,control: null,pages: null,initComponent: function() {
        GeoExt.ux.form.PrintForm.superclass.initComponent.call(this);
        if (this.map instanceof GeoExt.MapPanel) {
            this.map = this.map.map;
        }
        if (!this.map) {
            this.map = this.layer.map;
        }
        if (!this.pages) {
            this.pages = [];
        }
        this.initLayer();
        this.initControl();
        this.on({"show": function() {
                this.layer.setVisibility(true);
                this.control.activate();
            },"hide": function() {
                this.control.deactivate();
                this.layer.setVisibility(false);
            }});
    },initLayer: function() {
        if (!this.layer) {
            this.layer = new OpenLayers.Layer.Vector(null, {displayInLayerSwitcher: false});
        }
        if (!this.layer.map) {
            this.map.addLayer(this.layer);
        }
    },initControl: function() {
        var pages = this.pages;
        var updateHandles = function() {
            for (var i = 0, len = pages.length; i < len; ++i) {
                pages[i].updateHandle();
            }
            ;
        }
        this.control = new OpenLayers.Control.DragFeature(this.layer, {onDrag: function(feature) {
                if (feature.geometry instanceof OpenLayers.Geometry.Polygon) {
                    updateHandles();
                } else if (feature.geometry instanceof OpenLayers.Geometry.Point) {
                    for (var i = 0, len = pages.length; i < len; ++i) {
                        pages[i].updateByHandle(false);
                    }
                }
            },onComplete: updateHandles});
        this.map.addControl(this.control);
        this.control.activate();
    },beforeDestroy: function() {
        Ext.each(this.pages, function(page) {
            this.layer.removeFeatures(page.feature, page.handle);
        }, this);
        if (!this.initialConfig.layer) {
            this.layer.destroy();
        }
        this.control.destroy();
        delete this.layer;
        delete this.map;
        delete this.control;
    }});
Ext.reg("gx_printform", GeoExt.ux.form.PrintForm);
Ext.namespace("GeoExt.ux.form")
GeoExt.ux.form.SimplePrint = Ext.extend(GeoExt.ux.form.PrintForm, {layoutText: "Layout",dpiText: "DPI",scaleText: "Scale",rotationText: "Rotation",printText: "Print",printOptions: null,initComponent: function() {
        GeoExt.ux.form.SimplePrint.superclass.initComponent.call(this);
        this.pages.push(new GeoExt.ux.data.PrintPage({printProvider: this.printProvider,layer: this.layer}));
        this.initForm();
    },initForm: function() {
        var items = [{xtype: "combo",fieldLabel: this.layoutText,store: this.printProvider.layouts,displayField: "name",typeAhead: true,mode: "local",forceSelection: true,triggerAction: "all",selectOnFocus: true,plugins: new GeoExt.ux.plugins.PrintProviderField()}, {xtype: "combo",fieldLabel: this.dpiText,store: this.printProvider.dpis,displayField: "name",typeAhead: true,mode: "local",forceSelection: true,triggerAction: "all",selectOnFocus: true,plugins: new GeoExt.ux.plugins.PrintProviderField()}, {xtype: "combo",fieldLabel: this.scaleText,store: this.printProvider.scales,displayField: "name",typeAhead: true,mode: "local",forceSelection: true,triggerAction: "all",selectOnFocus: true,plugins: new GeoExt.ux.plugins.PrintPageField({page: this.pages[0]})}, {xtype: "textfield",fieldLabel: this.rotationText,name: "rotation",enableKeyEvents: true,validator: function(v) {
                    return !isNaN(v)
                },plugins: new GeoExt.ux.plugins.PrintPageField({page: this.pages[0]})}];
        Ext.each(items, function(item) {
            this.add(item);
        }, this);
        this.addButton({text: this.printText,handler: function() {
                this.printProvider.print(this.map, this.pages, this.printOptions);
            },scope: this});
        this.doLayout();
    }});
Ext.reg("gx_simpleprint", GeoExt.ux.form.SimplePrint);
Ext.namespace("GeoExt.ux.data");
GeoExt.ux.data.PrintProvider = Ext.extend(Ext.util.Observable, {url: null,capabilities: null,method: "POST",customParams: null,scales: null,dpis: null,layouts: null,dpi: null,layout: null,constructor: function(config) {
        this.initialConfig = config;
        Ext.apply(this, config);
        if (!this.customParams) {
            this.customParams = {};
        }
        this.addEvents(["capabilitiesload", "layoutchange", "dpichange"]);
        this.scales = new Ext.data.JsonStore({root: "scales",sortInfo: {field: "value",direction: "DESC"},fields: ["name", {name: "value",type: "float"}]});
        this.dpis = new Ext.data.JsonStore({root: "dpis",fields: ["name", {name: "value",type: "float"}]});
        this.layouts = new Ext.data.JsonStore({root: "layouts",fields: ["name", {name: "size",mapping: "map"}, {name: "rotation",type: "boolean"}]});
        if (config.capabilities) {
            this.loadStores();
        } else {
            if (this.url.split("/").pop()) {
                this.url += "/";
            }
            this.loadCapabilities();
        }
        GeoExt.ux.data.PrintProvider.superclass.constructor.apply(this, arguments);
    },setLayout: function(layout) {
        this.layout = layout;
        this.fireEvent("layoutchange", this, layout);
    },setDpi: function(dpi) {
        this.dpi = dpi;
        this.fireEvent("dpichange", this, dpi)
    },print: function(map, pages, options) {
        options = options || {};
        if (map instanceof GeoExt.MapPanel) {
            map = map.map;
        }
        var jsonData = Ext.apply({units: map.baseLayer.units,srs: map.baseLayer.projection.getCode(),layout: this.layout.get("name"),dpi: this.dpi.get("value")}, this.customParams);
        var pagesLayer = pages[0].feature.layer;
        var encodedLayers = [];
        Ext.each(map.layers, function(layer) {
            if (layer !== pagesLayer && layer.getVisibility() === true) {
                var enc = this.encodeLayer(layer);
                enc && encodedLayers.push(enc);
            }
        }, this);
        jsonData.layers = encodedLayers;
        var encodedPages = [];
        Ext.each(pages, function(page) {
            var center = page.getCenter();
            encodedPages.push(Ext.apply({center: [center.lon, center.lat],scale: page.scale.get("value"),rotation: page.rotation}, page.customParams));
        }, this);
        jsonData.pages = encodedPages;
        if (options.legend) {
            var encodedLegends = [];
            options.legend.items.each(function(cmp) {
                var encFn = this.encoders.legends[cmp.getXType()];
                encodedLegends = encodedLegends.concat(encFn.call(this, cmp));
            }, this);
            jsonData.legends = encodedLegends;
        }
        if (this.method === "GET") {
            window.open(this.capabilities.printURL + "?spec=" + 
            escape(Ext.encode(jsonData)));
        } else {
            Ext.Ajax.request({url: this.capabilities.createURL,jsonData: jsonData,success: function(response) {
                    var url = Ext.decode(response.responseText).getURL +
                        (Ext.isIE ? "?inline=true" : "");
                    this.download(url);
                },scope: this});
        }
    },
    download: function(url) {
        if (Ext.isOpera || Ext.isIE) {
            // Make sure that Opera don't replace the content tab with
            // the pdf
            window.open(url);
        } else {
            // This avoids popup blockers for all other browsers
            window.location.href = url;
        } 
    },
    loadCapabilities: function() {
        var url = this.url + "info.json";
        Ext.Ajax.request({url: url,disableCaching: false,success: function(response) {
                this.capabilities = Ext.decode(response.responseText);
                this.loadStores();
                this.fireEvent("loadcapabilities", this.capabilities);
            },scope: this});
    },loadStores: function() {
        this.scales.loadData(this.capabilities);
        this.dpis.loadData(this.capabilities);
        this.layouts.loadData(this.capabilities);
        this.setLayout(this.layouts.getAt(0));
        this.setDpi(this.dpis.getAt(0));
    },encodeLayer: function(layer) {
        var encLayer;
        for (var c in this.encoders.layers) {
            if (layer instanceof OpenLayers.Layer[c]) {
                encLayer = this.encoders.layers[c].call(this, layer);
                break;
            }
        }
        return (encLayer && encLayer.type) ? encLayer : null;
    },getAbsoluteUrl: function(url) {
        return Ext.DomHelper.overwrite(document.createElement("a"), {tag: "a",href: url}).href;
    },encoders: {"layers": {"WMS": function(layer) {
                var enc = this.encoders.layers.HTTPRequest.call(this, layer);
                Ext.apply(enc, {type: 'WMS',layers: [layer.params.LAYERS].join(",").split(","),format: layer.params.FORMAT,styles: [layer.params.STYLES].join(",").split(","),customParams: {}});
                var param;
                for (var p in layer.params) {
                    param = p.toLowerCase();
                    if (!layer.DEFAULT_PARAMS[param] && "layers,styles,width,height,srs".indexOf(param) == -1) {
                        enc.customParams[p] = layer.params[p];
                    }
                }
                return enc;
            },"OSM": function(layer) {
                var enc = this.encoders.layers.TileCache.call(this, layer);
                return Ext.apply(enc, {type: 'Osm',baseURL: enc.baseURL.substr(0, enc.baseURL.indexOf("$")),extension: "png"});
            },"TileCache": function(layer) {
                var enc = this.encoders.layers.HTTPRequest.call(this, layer);
                return Ext.apply(enc, {type: 'TileCache',layer: layer.layername,maxExtent: layer.maxExtent.toArray(),tileSize: [layer.tileSize.w, layer.tileSize.h],extension: layer.extension,resolutions: layer.serverResolutions || layer.resolutions});
            },"HTTPRequest": function(layer) {
                return {baseURL: this.getAbsoluteUrl(layer.url instanceof Array ? layer.url[0] : layer.url),opacity: (layer.opacity != null) ? layer.opacity : 1.0,singleTile: layer.singleTile};
            },"Image": function(layer) {
                return {type: 'Image',baseURL: this.getAbsoluteUrl(layer.getURL(layer.extent)),opacity: (layer.opacity != null) ? layer.opacity : 1.0,extent: layer.extent.toArray(),pixelSize: [layer.size.w, layer.size.h],name: layer.name};
            },"Vector": function(layer) {
                if (!layer.features.length) {
                    return;
                }
                var encFeatures = [];
                var encStyles = {};
                var features = layer.features;
                var featureFormat = new OpenLayers.Format.GeoJSON();
                var styleFormat = new OpenLayers.Format.JSON();
                var nextId = 1;
                var styleDict = {};
                var feature, style, dictKey, dictItem;
                for (var i = 0, len = features.length; i < len; ++i) {
                    feature = features[i];
                    style = feature.style || layer.style || layer.styleMap.createSymbolizer(feature, feature.renderIntent);
                    dictKey = styleFormat.write(style);
                    dictItem = styleDict[dictKey];
                    if (dictItem) {
                        styleName = dictItem;
                    } else {
                        styleDict[dictKey] = styleName = nextId++;
                        if (style.externalGraphic) {
                            encStyles[styleName] = Ext.applyIf({externalGraphic: this.getAbsoluteUrl(style.externalGraphic)}, style);
                        } else {
                            encStyles[styleName] = style;
                        }
                    }
                    var featureGeoJson = featureFormat.extract.feature.call(featureFormat, feature);
                    featureGeoJson.properties = OpenLayers.Util.extend({_gx_style: styleName}, featureGeoJson.properties);
                    encFeatures.push(featureGeoJson);
                }
                return {type: 'Vector',styles: encStyles,styleProperty: '_gx_style',geoJson: {type: "FeatureCollection",features: encFeatures},name: layer.name,opacity: (layer.opacity != null) ? layer.opacity : 1.0};
            }},"legends": {"gx_wmslegend": function(legend) {
                return this.encoders.legends.base.call(this, legend);
            },"gx_urllegend": function(legend) {
                return this.encoders.legends.base.call(this, legend);
            },"base": function(legend) {
                var enc = [];
                legend.items.each(function(cmp) {
                    if (cmp instanceof Ext.form.Label) {
                        enc.push({name: cmp.text,classes: []});
                    } else if (cmp instanceof GeoExt.LegendImage) {
                        enc.push({name: "",icon: this.getAbsoluteUrl(cmp.url),classes: []});
                    }
                }, this);
                return enc;
            }}}});
Ext.namespace("GeoExt.ux.data");
GeoExt.ux.data.PrintPage = Ext.extend(Ext.util.Observable, {printProvider: null,feature: null,handle: null,scale: null,rotation: 0,customParams: null,constructor: function(config) {
        this.initialConfig = config;
        Ext.apply(this, config);
        delete this.layer;
        if (!this.customParams) {
            this.customParams = {};
        }
        this.addEvents(["change"]);
        this.feature = new OpenLayers.Feature.Vector(OpenLayers.Geometry.fromWKT("POLYGON((-1 -1,1 -1,1 1,-1 1,-1 -1))"));
        this.handle = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Point(0, 0));
        if (config.layer) {
            config.layer.addFeatures([this.feature, this.handle]);
        }
        this.printProvider.on({"layoutchange": function() {
                this.updateByHandle();
            },scope: this});
        GeoExt.ux.data.PrintPage.superclass.constructor.apply(this, arguments);
    },getCenter: function() {
        return this.feature.geometry.getBounds().getCenterLonLat();
    },setScale: function(scale, units) {
        var bounds = this.calculatePageBounds(scale, units);
        var geom = bounds.toGeometry();
        var rotation = this.rotation;
        if (rotation != 0) {
            geom.rotate(-rotation, geom.getCentroid());
        }
        this.scale = scale;
        this.updateFeature(geom);
    },setCenter: function(center) {
        var geom = this.feature.geometry;
        var oldCenter = geom.getBounds().getCenterLonLat();
        var dx = center.lon - oldCenter.lon;
        var dy = center.lat - oldCenter.lat;
        geom.move(dx, dy);
        this.updateFeature(geom);
    },setRotation: function(rotation) {
        if (this.printProvider.layout.get("rotation") === true) {
            var geom = this.feature.geometry;
            geom.rotate(this.rotation - rotation, geom.getCentroid());
            this.rotation = rotation;
            this.updateFeature(geom);
        }
    },fitPage: function(map) {
        this.suspendEvents();
        this.setCenter(map.getCenter());
        var extent = map.getExtent();
        var units = map.baseLayer.units;
        var scale;
        this.printProvider.scales.each(function(rec) {
            scale = rec;
            return !extent.containsBounds(this.calculatePageBounds(scale, units));
        }, this)
        this.resumeEvents();
        this.setScale(scale, units);
    },updateByHandle: function(updateHandle) {
        var f = this.feature;
        var rotation = 0;
        if (this.printProvider.layout.get("rotation") === true) {
            var hLoc = this.handle.geometry.getBounds().getCenterLonLat();
            var center = f.geometry.getBounds().getCenterLonLat();
            var dx = hLoc.lon - center.lon;
            var dy = hLoc.lat - center.lat;
            rotation = Math.round(Math.atan2(dx, dy) * 180 / Math.PI);
        }
        var geom = this.handle.geometry;
        var dist = f.geometry.getCentroid().distanceTo(geom);
        var scaleFits = [], distHash = {};
        this.printProvider.scales.each(function(rec) {
            var bounds = this.calculatePageBounds(rec);
            var d = Math.abs((bounds.getHeight() / 2) - dist);
            scaleFits.push(d);
            distHash[d.toPrecision(8)] = rec;
        }, this);
        var min = scaleFits.concat().sort(function(a, b) {
            return a < b ? -1 : 1;
        })[0];
        var scale = distHash[min.toPrecision(8)];
        var bounds = this.calculatePageBounds(scale);
        var geom = bounds.toGeometry();
        geom.rotate(-rotation, geom.getCentroid());
        this.scale = scale;
        this.rotation = rotation;
        this.updateFeature(geom, updateHandle);
    },updateFeature: function(geometry, updateHandle) {
        var f = this.feature;
        geometry.id = f.geometry.id;
        f.geometry = geometry;
        f.layer && f.layer.drawFeature(f);
        if (updateHandle !== false) {
            this.updateHandle();
        }
        this.fireEvent("change", this)
    },updateHandle: function() {
        var f = this.feature;
        var h = this.handle;
        var hLoc = this.calculateHandleLocation();
        var geom = new OpenLayers.Geometry.Point(hLoc.lon, hLoc.lat);
        geom.id = h.geometry.id;
        h.geometry = geom;
        h.layer && h.layer.drawFeature(h);
    },calculateHandleLocation: function() {
        var c = this.feature.geometry.components[0].components;
        var top = new OpenLayers.Geometry.LineString([c[2], c[3]]);
        return top.getBounds().getCenterLonLat();
    },calculatePageBounds: function(scale, units) {
        var s = scale.get("value");
        var geom = this.feature.geometry;
        var center = geom.getBounds().getCenterLonLat();
        var size = this.printProvider.layout.get("size");
        var units = units || this.feature.layer.map.baseLayer.units || "dd";
        var unitsRatio = OpenLayers.INCHES_PER_UNIT[units];
        var w = size.width / 72 / unitsRatio * s / 2;
        var h = size.height / 72 / unitsRatio * s / 2;
        return new OpenLayers.Bounds(center.lon - w, center.lat - h, center.lon + w, center.lat + h);
    }});
