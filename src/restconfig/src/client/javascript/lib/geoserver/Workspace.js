geoserver.Workspace = OpenLayers.Class(geoserver.Object, {

  getDataStoreNames: function() {
     var res = this.gs.get( 'workspaces/' + this.name + '/datastores.json' );
     var ds = res.dataStores.dataStore;
     var dsNames = new Array(ds.length);
     for (var i = 0; i < ds.length; i++) {
       dsNames = ds[i].name;
     }   
     return dsNames;
  },  

  getDataStore: function(datastore) {
     var res = this.gs.get( 'workspaces/' + this.name + '/datastores/' + 
       datastore + '.json' ); 
     return new geoserver.DataStore( res.dataStore, this.gs);
  },

  addDataStore: function(datastore) {
     var json = JSON.stringify(datastore);     
     this.gs.post( 'workspaces/' + this.name + '/datastores', 
       {data:json, headers:{'Content-type':'application/json'}} );
  },

  toJSON: function() {
    return {workspace:{name: this.name}};
  }

})
