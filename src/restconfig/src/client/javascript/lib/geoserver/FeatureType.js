geoserver.FeatureType = OpenLayers.Class(geoserver.Object,{

   toJSON: function() {
      return {
        featureType: {
          name: this.name, 
          srs: this.srs,
          nativeBoundingBox: this.nativeBoundingBox
        }
      };
   }
});
