var minX;
var maxX;
var minY;
var maxY;

function initMap() {

	var vectorSource = new ol.source.Vector({
		url : 'data/geojson/countries.geojson',
		format : new ol.format.GeoJSON()
	});

	var map = new ol.Map({
		layers : [ new ol.layer.Tile({
			source : new ol.source.OSM()
		}), new ol.layer.Vector({
			source : vectorSource
		}) ],
		renderer : 'canvas',
		target : 'map',
		view : new ol.View({
			center : [ 1702405.493967, 8829563.846199 ],
			zoom : 4,
			minZoom : 2,
		// , extent: [-6300000,-6200000,6800000,6500000]
		})
	});

	var source = new ol.source.Vector({
		wrapX : false
	});
	var vector = new ol.layer.Vector({
		source : source
	});

	map.addLayer(vector);

	// a normal select interaction to handle click
	var select = new ol.interaction.Select();
	map.addInteraction(select);

	// a DragBox interaction used to select features by drawing boxes
	var dragBox = new ol.interaction.DragBox({
		condition : ol.events.condition.shiftKeyOnly,
		style : new ol.style.Style({
			stroke : new ol.style.Stroke({
				color : [ 0, 0, 255, 1 ]
			}),
			fill : new ol.style.Fill({
				color : [ 0, 0, 255, 0.1 ]
			})
		})
	});

	map.addInteraction(dragBox);

	dragBox.on('boxend', function(e) {
		source.clear(true);
		// returnes the drag box coordinates
		var infoCoord = dragBox.getGeometry().getCoordinates();
		var poly = new ol.geom.Polygon(infoCoord);
		var polyStyle = new ol.style.Style({
			fill : new ol.style.Fill({
				color : [ 0, 0, 255, 0.1 ]
			}),
			stroke : new ol.style.Stroke({
				color : [ 0, 0, 255, 1 ]
			})
		})

		var feature = new ol.Feature(poly);
		feature.setStyle(polyStyle);
		source.addFeature(feature);
		splitCoords(infoCoord);
		document.getElementById("okeyButton").disabled = false;
	});

	function splitCoords(coord) {
		var splitCoord = coord.toString().split(",");

		if (splitCoord[0] < splitCoord[4]) {
			minX = splitCoord[0];
			maxX = splitCoord[4];
		} else {
			minX = splitCoord[4];
			maxX = splitCoord[0];
		}

		if (splitCoord[1] < splitCoord[3]) {
			minY = splitCoord[1];
			maxY = splitCoord[3];
		} else {
			minY = splitCoord[3];
			maxY = splitCoord[1];
		}

	}

}

function confirmCoord() {
	window.parent.setOldCoordValues(minX, minY, maxX, maxY);
	window.parent.loadRefScript();

}

function exitIframe() {
	window.parent.exit();
}
