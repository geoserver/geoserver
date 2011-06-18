from geoserver.catalog.util import info, lazy
from geoserver.catalog.layer import Layer
from geoscript.workspace import Workspace

class Store(object):
   """
   A GeoServer store. 
   """

   def __init__(self, store, workspace=None, catalog=None):
     if not catalog:
        from geoserver.catalog import Catalog
        catalog = Catalog()

     self.catalog = catalog

     if isinstance(workspace, str):
        workspace = catalog._catalog.getWorkspaceByName(workspace)
                
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
        self._info = store

   @lazy
   def data(self):
     return Workspace(ds=self._info.getDataStore(None))
   """
   
   def __getattr__(self, name):
     try:
       return getattr(self._info, name)
     except AttributeError:
       if not self.__dict__.has_key(name):
         raise AttributeError("No such attribute %s" % name)
       return self.__dict__[name]

   def __setattr__(self, name, value):
     if name not in ["_info", "catalog"] and hasattr(self._info, name):
       setattr(self._info, name, value)
     else:
       self.__dict__[name] = value

   """
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
