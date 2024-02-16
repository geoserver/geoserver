# INSPIRE metadata configuration using metadata and CSW {: #tutorial_metadata }

The INSPIRE directive requires exposure of fairly complex metadata schemes based on the ISO Metadata Profile. This exposure is supported by the built-in [Catalog Services for the Web (CSW)](../../services/csw/index.md) service (can be harvested by GeoNetwork), while the [Metadata](../../extensions/metadata/index.md) community module allows adding any amount of customized metadata fields to layers that may be required for your particular case.

Creating all the needed configuration files in both modules can be a tedious task. Therefore we have added this example configuration.

## Metadata configuration

Place the following files in the `metadata` folder:

UI configuration [metadata-ui.yaml](files/metadata-ui.yaml)

Translate keys to labels [metadata.properties](files/metadata.properties)

Translate keys to Dutch labels [metadata_nl.properties](files/metadata_nl.properties)

Content for gemet-concept dropdown [keyword-gemet-concept.csv](files/keyword-gemet-concept.csv)

Content for reference-system requirebox [keyword-gemet-concept.csv](files/reference-systems-epsg.csv)

Content for inspire-theme-label & inspire-theme-ref [keyword-inspire-theme.csv](files/keyword-inspire-theme.csv)

Geonetwork mapping [metadata-mapping.yaml](files/metadata-mapping.yaml)

Namespaces for geonetwork mapping [metadata-mapping.yaml](files/metadata-namespaces.yaml)

Geonetwork endpoints [metadata-geonetwork.yaml](files/metadata-geonetwork.yaml)

Synchronize native fields [metadata-native-mapping.yaml](files/metadata-native-mapping.yaml)

Open any layer: navigate to **Layers --> Choose the layer --> Metadata tab**.

The metadata fields are available in the panel **Metadata fields**.

You may now add custom metadata to your layers.

## CSW configuration

Map metadata attributes to xml [MD_Metadata.properties](files/MD_Metadata.properties)

Map Feature Catalogue attributes to xml [FC_FeatureCatalogue.properties](files/FC_FeatureCatalogue.properties)

Map Record attributes to xml [Record.properties](files/Record.properties)

You may now see your custom metadata exposed by the built-in CSW service:

e.g. ``https://my.host/geoserver/csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd``.

## GeoNetwork configuration

Create a GeoNetwork CSW harvester that points to your to Geoserver's CSW endpoint:

e.g. ``https://my.host/geoserver/csw?Service=CSW&Request=Getcapabilities``.

You may now start harvesting!
