geoserver.Object = OpenLayers.Class({

   initialize: function(config,gs) {
     OpenLayers.Util.extend(this,config);
     this.gs = gs;
   }

});
