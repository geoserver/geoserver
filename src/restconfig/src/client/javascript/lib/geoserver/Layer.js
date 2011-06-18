geoserver.Layer = OpenLayers.Class(geoserver.Object, {

  initialize: function(config,gs) {
     geoserver.Object.prototype.initialize.apply(this,[config,gs]);
  },

  getDefaultStyle: function() {
     var res = this.gs.get( this.defaultStyle.href );
     return new geoserver.Style( res.style, this.gs );
  }, 

  setDefaultStyle: function(style) {
     var json = JSON.stringify( {layer:{defaultStyle:{name:style.name}}} );
     this.gs.put( 'layers/' + this.name,
       {data: json, headers: {'Content-Type': 'application/json'}} );
  },

  getStyleNames: function() {
     var res = this.gs.get( 'layers/' + this.name + '/styles.json' );
     var styles = new Array(res.styles.style.length);
     for (var i = 0; i < res.styles.style.length; i++) {
        styles[i] = res.styles.style[i].name;
     }
     return styles;
  },

  addStyle: function(style) {
     var json = JSON.stringify(style);
     var res = this.gs.post( 'layers/' + this.name + '/styles', 
       {data: json, headers: {'Content-Type': 'application/json'}} );
  }
  
});
