var printProvider, printForm;

Ext.onReady(function() {

    printProvider = new GeoExt.ux.data.PrintProvider({
        // using get for remote service access without same origin restriction.
        // For asynchronous requests, we would set method to "POST".
        //method: "GET",
        method: "POST",
        
        // capabilities from script tag in Printing.html. For asynchonous
        // loading, we would configure url instead of capabilities.
        //capabilities: printCapabilities
        url: "/geoserver/pdf/"
    });
    
    var mapPanel = new GeoExt.MapPanel({
        region: "center",
        layers: [new OpenLayers.Layer.WMS("Natural Earth",
            "/geoserver/wms",
            {layers: "topp:states"})] ,
        center: [-98,40],
        zoom: 5
    });

    // create a vector layer, which will also be printed.
    var redline = new OpenLayers.Layer.Vector("vector", {
        styleMap: new OpenLayers.StyleMap({
            strokeColor: "red",
            fillColor: "red",
            fillOpacity: 0.7,
            strokeWidth: 2,
            pointRadius: 12,
            externalGraphic: "http://geoserver.org/img/geoserver-logo.png"
        })
    });
    var geom = OpenLayers.Geometry.fromWKT, Vec = OpenLayers.Feature.Vector;
    redline.addFeatures([
        new Vec(geom("POLYGON(-97 39,-98 40, -96 41)")),
        new Vec(geom("LINESTRING(-97 40, -98 39, -99 38)")),
        new Vec(geom("POINT(-98 38)"))
    ]);
    mapPanel.map.addLayer(redline);
    
    // a simple print form
    printForm = new GeoExt.ux.form.SimplePrint({
        map: mapPanel,
        printProvider: printProvider,
        bodyStyle: {padding: "5px"},
        labelWidth: 65,
        defaults: {width: 115},
        region: "east",
        border: false,
        width: 200
    });

    // add custom fields to the form
    printForm.insert(0, {
        xtype: "textfield",
        name: "mapTitle",
        fieldLabel: "Title",
        value: "A custom title",
        plugins: new GeoExt.ux.plugins.PrintPageField({
            page: printForm.pages[0]
        })
    });
    printForm.insert(1, {
        xtype: "textarea",
        fieldLabel: "Comment",
        name: "comment",
        value: "A custom comment",
        plugins: new GeoExt.ux.plugins.PrintPageField({
            page: printForm.pages[0]
        })
    });
    
    var formCt = new Ext.Panel({
        layout: "fit",
        region: "east",
        width: 200
    });

    new Ext.Panel({
        renderTo: "content",
        layout: "border",
        width: 800,
        height: 350,
        items: [mapPanel, formCt]
    });
    
    /* add the print form to its container and make sure that the print page
     * fits the max extent
    formCt.add(printForm);
    formCt.doLayout();
    printForm.pages[0].fitPage(mapPanel.map);
     */

    /* use this code block instead of the above one if you configured the
     * printProvider with url instead of capabilities
     */
    var myMask = new Ext.LoadMask(formCt.body, {msg:"Loading data..."});
    myMask.show();
    printProvider.on("loadcapabilities", function() {
        myMask.hide();
        formCt.add(printForm);
        formCt.doLayout();
        printForm.pages[0].fitPage(mapPanel.map);
    });
    
});
