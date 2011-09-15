from geoserver.catalog.util import info, lazy
from geoscript.layer import Layer as GeoScriptLayer
from org.geoserver.catalog import LayerInfo, ResourceInfo
from org.geotools.feature import NameImpl as Name

class Layer(object):
  """
  A GeoServer layer.

  *layer* is the name of a layer. If the name does not correspond to a layer in the
  catalog one will be created "disconnected" from the catalog.

  *store* 
  """

  def __init__(self, layer, store=None):
    if store:
      self.catalog = store.catalog 
    else:
      from geoserver.catalog import Catalog
      self.catalog = Catalog()

    self.store = None
    self._info = None

    if isinstance(layer, ResourceInfo):
      self._info = layer 
    elif isinstance(layer, LayerInfo):
      self._info = layer.resource
    elif isinstance(layer, (str,unicode)):
      l = self.catalog._catalog.getLayerByName(layer)
      if l:
        self._info = l.resource

    if self._info:
      from geoserver.catalog import Store
      self.store = Store(self._info.store)

    if not self.store:
      if isinstance(store, (str,unicode)):
        store = Store(store)

      if not store: 
        raise Exception('Unable to find store for layer %s' % str(layer))

      self.store = store

    if not self._info:
      if isinstance(layer, (str,unicode)):
        cat = self.catalog
        ft = cat._catalog.getFeatureTypeByDataStore(self.store._info, layer)

        if not ft:
          b = cat.builder()
          b.setStore(store._info)
          ft = b.buildFeatureType(Name(layer))

        self._info = ft
      else:
        raise Exception('Unable to create layer from %s' % str(layer))

  @lazy
  def data(self):
    fs = self._info.getFeatureSource(None, None)
    return GeoScriptLayer(workspace=self.store.data, fs=fs)

Layer = info(Layer)
