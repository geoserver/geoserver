This mappings are used for testing workspaces isolation which allow the publication of
a complex feature type multiple times in different workspaces. We have three different
types of mappings which allow us to test three particular situations, note that stations
contain measurements:

    * both stations and measurements feature types are mapped and published in the same
      isolated workspace

    * stations feature type is published in the isolated workspace and measurement type
      is an included type (i.e. it is not published)

    * only stations feature type is published in the isolated workspace and the global
      (non isolated) measurements feature type is used for feature chaining

All mappings can be used for GML 3.1 and GML 3.2 with the correct parameterization.