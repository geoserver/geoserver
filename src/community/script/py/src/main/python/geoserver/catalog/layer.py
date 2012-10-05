from geoserver.util import info, lazy
from geoscript.layer import Layer as GeoScriptLayer
from org.geoserver.catalog import LayerInfo, ResourceInfo, StoreInfo
from org.geotools.feature import NameImpl as Name

class Layer(object):
  """
  A GeoServer layer.

  The constructor takes a *layer* argument which is the name of a layer. 

  >>> l = Layer('topp:states')

  The optional *store* argument is used to specify the containing store.

  >>> l = Layer('states', 'states_shapefile')
  """

  def __init__(self, layer, store=None):
    if store and hasattr(store, 'catalog'):
      self.catalog = store.catalog 
    else:
      from geoserver.catalog import Catalog
      self.catalog = Catalog()

    self.store = None
    self._info = None

    cat = self.catalog._catalog
    if isinstance(layer, ResourceInfo):
      self._info = layer 
    elif isinstance(layer, LayerInfo):
      self._info = layer.resource
    elif isinstance(layer, (str,unicode)):
      if store and isinstance(store, (str, unicode)):
        s = cat.getStoreByName(store, StoreInfo)
        self._info = cat.getResourceByStore(s, layer, ResourceInfo)
      elif store and hasattr(store, '_info'):
        self._info = cat.getResourceByStore(store._info, layer, ResourceInfo)
      elif not store:
        l = cat.getLayerByName(layer)
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
    """
    The data backing the layer as a geoscript layer.
    
    >>> l = Layer('sf:archsites')
    >>> l.data.schema
    archsites [the_geom: Point, cat: long, str1: str]
    >>> l.data.count()
    25
    """
    fs = self._info.getFeatureSource(None, None)
    return GeoScriptLayer(workspace=self.store.data, source=fs)

Layer = info(Layer)
