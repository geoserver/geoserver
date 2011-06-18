geoserver.GeoServer = OpenLayers.Class({

   initialize: function(url,options) {
      this.url = url + "/rest/";
   },

   request: function(method,url,options,handler,caller) {
     if ( !( url.match( "^http://") == "http://")  ) {
        url = gs.url + url;
     }
     
     var config = { url: url, async: false }; 
     OpenLayers.Util.extend(config,options);

     var res = OpenLayers.Request[method].apply(this,[config]);
     if ( handler != null ) {
        return handler.apply(caller,[res]);
     }
     else {
        if ( res.responseText ) { 
           return eval( '(' + res.responseText + ')' );
        }
     }
   },

   get: function(url,handler,caller) {
     return this.request('GET',url,null,handler,caller);
   },

   put: function(url,options) {
     return this.request('PUT',url,options);
   },

   post: function(url,options) {
     return this.request('POST',url,options);
   },

   delete: function(url,options) {
     return this.request('DELETE',url,options);
   },

   getDefaultWorkspace: function() {
      var res = this.get('workspaces/default.json');
      return new geoserver.Workspace(res.workspace,this);
   },

   setDefaultWorkspace: function(workspace) {

   },

   getWorkspaceNames: function() {
      var res = this.get('workspaces.json');
      var ws = res.workspaces.workspace;
      var wsNames = new Array(ws.length); 
      for (var i = 0; i < ws.length; i++ ) {
         wsNames[i] = ws[i].name;
      }
      return wsNames;
   },

   getWorkspace: function(name) {
      var res = this.get('workspaces/' + name + '.json');
      return new geoserver.Workspace( res.workspace, this );
   },

   addWorkspace: function(workspace) {
      var json = JSON.stringify(workspace);
      this.post( 'workspaces', 
       {data:json,headers:{'Content-type': 'application/json'}} );
   },

   removeWorkspace: function(workspace) {
      this.delete( 'workspaces/' + workspace.name );
   },

   getStyleNames: function() {
      var res = this.get('styles.json');
      var styleNames = new Array(res.styles.style.length);
      for (var i = 0; i < res.styles.style.length; i++) {
         styleNames[i] = res.styles.style[i].name;
      }
      return styleNames;
   },

   getStyle: function(name) {
      var res = this.get( 'styles/' + name + '.json' );
      return new geoserver.Style( res.style, this );
   },
    
   getLayerNames: function() {
      var res = this.get( 'layers.json' );
      var layerNames = new Array(res.layers.layer.length);
      for (var i = 0; i < res.layers.layer.length; i++ ) {
         layerNames[i] = new geoserver.Layer( res.layers.layer[i],this );
      }
      return layerNames;
   },

   getLayer: function(name) {
      var res = this.get( 'layers/' + name + '.json' );
      return new geoserver.Layer( res.layer, this );
   }
});

