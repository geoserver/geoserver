var iframe;
var oldMinX, oldMinY, oldMaxX, oldMaxY;

function setOldCoordValues(minX, minY, maxX, maxY) {
	oldMinX = minX;
	oldMinY = minY;
	oldMaxX = maxX;
	oldMaxY = maxY;
}

function setNewCoordValues(minX, minY, maxX, maxY) {
	$('input[name=bounds:minX]').val(minX);
	$('input[name=bounds:minY]').val(minY);
	$('input[name=bounds:maxX]').val(maxX);
	$('input[name=bounds:maxY]').val(maxY);
}

function exit() {
	iframe.remove();
	document.getElementById("openMap").disabled = false;
}

function openWin() {
	iframe = document.createElement('iframe');
	iframe.src = "resources/org.geoserver.web.GeoServerBasePage/GetMap.html";
	iframe.style.position = 'absolute';
	iframe.style.width = '500px';
	iframe.style.height = '350px';
	iframe.style.left = '500px';
	iframe.style.top = '200px';
	iframe.style.boxShadow = "10px 20px 30px grey";
	window.iframe = iframe;
	document.body.appendChild(iframe);
	document.getElementById("openMap").disabled = true;
}

function loadRefScript() {
	iframe.remove();
	document.getElementById("openMap").disabled = false;

	var refSys = $('input[name=crs:srs]').val().toString();
	ref = refSys.toString().split(":");
	var toRef = ref[1];
	var head = document.getElementsByTagName('head')[0];
	var script = document.createElement('script');
	script.type = 'text/javascript';
	var srcString = new String("http://epsg.io/" + toRef.toString() + ".js");
	script.src = srcString;
	document.head.appendChild(script);

	changeCoordSys();
}

function changeCoordSys() {
	proj4
			.defs(
					"EPSG:3857",
					"+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs");

	var toRef = $('input[name=crs:srs]').val().toString();

	var newCoords1 = proj4(proj4('EPSG:3857'), proj4(toRef),
			[ oldMinX, oldMinY ]);
	var newCoords2 = proj4(proj4('EPSG:3857'), proj4(toRef),
			[ oldMaxX, oldMaxY ]);

	splitCoord(newCoords1, newCoords2);
}

function splitCoord(nc1, nc2) {
	var min = nc1.toString().split(",");
	var max = nc2.toString().split(",");

	setNewCoordValues(min[0], min[1], max[0], max[1]);
}
