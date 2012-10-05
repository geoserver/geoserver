from geoserver.util import info, lazy
from geoserver.catalog.layer import Layer
from geoscript.workspace import Workspace as GeoScriptWorkspace
from org.geoserver.catalog import StoreInfo

class Store(object):
   """
   A GeoServer store. 

   This class behaves like a dictionary in which the keys are ``str`` that 
   correspond to layer namespace and the values are instances of 
   :class:`Layer <geoserver.catalog.Layer>`

   >>> shapes = Store('taz_shapes', 'topp')
   >>> shapes.keys()
   [u'tasmania_cities', u'tasmania_roads', u'tasmania_state_boundaries', u'tasmania_water_bodies']
   >>> roads = shapes['tasmanisa_roads']

   The constructor takes two arguments: 

   *store* is the name of the store. If the store name does not correspond to an
   existing store in the catalog the instance will be created and "disconnected"
   from the catalog. 
   
   *workspace* is the name of the workspace the store is a part of or an 
   instance of :class:`Workspace <geoserver.catalog.Workspace>`. If no workspace
   is specified it is taken to mean the default workspace in the catalog.
   """

   def __init__(self, store, workspace=None, catalog=None):
     if not catalog:
        from geoserver.catalog import Catalog
        catalog = Catalog()

     self.catalog = catalog
     self.workspace = None
     self._info = None

     if isinstance(store, StoreInfo): 
        self._info = store

        from geoserver.catalog.workspace import Workspace
        self.workspace = Workspace(self._info.getWorkspace())
     
     if not self.workspace:
        if isinstance(workspace, (str,unicode)):
           from geoserver.catalog.workspace import Workspace
           workspace = Workspace(workspace)

        if not workspace:
           from geoserver.catalog.workspace import Workspace
           workspace = Workspace()

        self.workspace = workspace

     if not self._info:
        if isinstance(store, (str,unicode)):
           ds = catalog._catalog.getDataStoreByName(store)
           if not ds:
              ds = catalog._catalog.getFactory().createDataStore()
              ds.setName(store)
              if workspace: 
                 ds.setWorkspace(workspace._info)   
              else:
                 ds.setWorkspace(catalog._catalog.getDefaultWorkspace())

           self._info = ds
        else:
           raise Exception('Unable to create store from %s' % str(store))

   @lazy
   def data(self):
     """
     The data backing the store as a geoscript workspace.
     
     >>> shapes = Store('taz_shapes', 'topp')
     >>> shapes.data.layers()
     ['tasmania_state_boundaries', 'tasmania_water_bodies', 'tasmania_roads', 'tasmania_cities']
     """
     return GeoScriptWorkspace(ds=self._info.getDataStore(None))

   def keys(self):
     featureTypes = self.catalog._catalog.getFeatureTypesByStore(self._info)
     return [ft.getName() for ft in featureTypes]

   def __getitem__(self, key):
     layer = self.catalog._catalog.getFeatureTypeByStore(self._info, key)
     if layer:
       return Layer(layer, self)

     raise KeyError('No such layer %s' % (key))

   def __iter__(self):
     return self.layers().__iter__()

   def iterkeys(self):
     return self.layers().__iter__()

   def iteritems(self):
     for l in self.layers():
       return (l, self.__getitem__(l))

Store = info(Store)
