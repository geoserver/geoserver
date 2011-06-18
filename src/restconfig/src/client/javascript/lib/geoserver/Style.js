geoserver.Style = OpenLayers.Class(geoserver.Object, {

  initialize: function(options,gs) {
    geoserver.Object.prototype.initialize.apply(this,[options,gs]);
    this.parser = new OpenLayers.Format.SLD();
  }, 

  getSLD: function() {
     var handler = function(req) {
        this.parser.read( req.responseXML || req.responseText );
     }
     return gs.get( 'styles/' + this.name + '.sld', handler, this );
  }, 

  setSLD: function(sld) {
    var xml = this.parser.write(sld);
    gs.put( 'styles/' + this.name + '.sld', 
      {data: xml, headers: {'Content-Type': 'application/vnd.ogc.sld+xml'}} ); 
  },

  toJSON: function() {
    return { style: { name: this.name } };
  }

});
