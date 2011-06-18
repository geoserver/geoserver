from geoserver.catalog.util import info, lazy
from geoscript.layer import Layer as GeoScriptLayer
from org.geotools.feature import NameImpl as Name

class Layer(object):
  """
  A GeoServer layer.
  """

  def __init__(self, layer, store):
    self.store = store
    self.catalog = store.catalog

    if isinstance(layer, (str,unicode)):
      cat = self.catalog
      ft = cat._catalog.getFeatureTypeByDataStore(self.store._info, layer)
      if not ft:
        b = cat.builder()
        b.setStore(store._info)
        ft = b.buildFeatureType(Name(layer))

      self._info = ft
    else:
      self._info = layer

  @lazy
  def data(self):
    fs = self._info.getFeatureSource(None, None)
    return GeoScriptLayer(workspace=self.store.data, fs=fs)

Layer = info(Layer)
