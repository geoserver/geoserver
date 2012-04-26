//straight from http://code.google.com/p/simile-widgets/wiki/Timeline_GettingStarted
var tl;
function onLoad() {
	var eventSource = new Timeline.DefaultEventSource();
	var bandInfos = [
   /*
	Timeline.createBandInfo({
		eventSource : eventSource,
		date : "${date}",
		width : "70%",
		intervalUnit : Timeline.DateTime.HOUR,
		intervalPixels : 100
	}),
	*/ 
	Timeline.createBandInfo({
		overview : false,
		eventSource : eventSource,
		date : "${date}",
		width : "75%",
		intervalUnit : Timeline.DateTime.DAY,
		intervalPixels : 100
	}), 
	Timeline.createBandInfo({
		overview : true,
		eventSource : eventSource,
		date : "${date}",
		width : "15%",
		intervalUnit : Timeline.DateTime.MONTH,
		intervalPixels : 100
	}), 
	Timeline.createBandInfo({
		overview : true,
		eventSource : eventSource,
		// date : "May 28 2011 00:00:00 GMT",
		date : "${date}",
		width : "10%",
		intervalUnit : Timeline.DateTime.YEAR,
		intervalPixels : 200
	 })
	 
	];

	bandInfos[1].syncWith = 0;
	bandInfos[1].highlight = true;
	bandInfos[2].syncWith = 1;
	bandInfos[2].highlight = true;
	//bandInfos[3].syncWith = 2;
	//bandInfos[3].highlight = true;

	tl = Timeline.create(document.getElementById("my-timeline"), bandInfos);
	Timeline.loadXML("../rest/versioning/timeline.xml", function(xml, url) {
		eventSource.loadXML(xml, url);
	});
}

var resizeTimerID = null;
function onResize() {
	if (resizeTimerID == null) {
		resizeTimerID = window.setTimeout(function() {
			resizeTimerID = null;
			tl.layout();
		}, 500);
	}
}