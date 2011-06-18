geoserver.DataStore = OpenLayers.Class(geoserver.Object,{
  
  initialize: function(config,gs) {
     geoserver.Object.prototype.initialize.apply(this,[config,gs]);
     if ( !this.enabled ) {
       this.enabled = true;
     }
  },

  path: function() {
     return 'workspaces/' + this.workspace.name + '/datastores/' + this.name 
        + '/';
  },

  getAvailableFeatureTypeNames: function() {
     var res = this.gs.get( this.path() + 'featuretypes.json?list=available');
     var ft = res.featureTypes.featureType;
     var ftNames = new Array(ft.length);
     for ( var i = 0; i < ft.length; i++ ) {
        ftNames[i] = ft[i].name;
     }
     return ftNames;
  },

  getFeatureTypeNames: function() {
     var res = this.gs.get( this.path()  + 'featuretypes.json');
     var ft = res.featureTypes.featureType;
     var ftNames = new Array(ft.length);
     for ( var i = 0; i < ft.length; i++ ) {
        ftNames[i] = ft[i].name;
     }
     return ftNames;
  },

  getFeatureType: function(name) {
     var res = this.gs.get( this.path() +  'featuretypes/' + name + '.json' );
     return new geoserver.FeatureType(res.featureType,this.gs);
  },

  addFeatureType: function(featureType) {
     var json = JSON.stringify(featureType);
     this.gs.post( this.path() + 'featuretypes/',
       {data:json,headers:{'Content-type': 'application/json'}} );
  },

  toJSON: function() {
     return {
       dataStore:{
         name: this.name,
         enabled: this.enabled,
         connectionParameters: this.connectionParameters 
       }
     };
  }
});
