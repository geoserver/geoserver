from geoserver.catalog.store import Store
from geoserver.catalog.util import info

class Workspace(object):
  """
  A GeoServer workspace/namespace.
  """

  def __init__(self, workspace, catalog=None):
     if not catalog:
       from geoserver.catalog import Catalog
       catalog = Catalog()
     self.catalog = catalog

     if isinstance(workspace, (str,unicode)):
       ws = catalog._catalog.getWorkspaceByName(workspace)       
       if not ws:
         ws = catalog._catalog.getFactory().createWorkspace()  
         ws.setName(workspace) 

       self._info = ws
     else:
       if not workspace:
         self._info = catalog._catalog.getDefaultWorkspace()
       else:
         self._info = workspace

  def save(self):
     cat = self.catalog._catalog
     if not self._info.getId():
        cat.add(self._info)
     else:
        cat.save(self._info) 

  def add(self, data, name, **attrs):

     cat = self.catalog._catalog
     store = cat.getFactory().createDataStore()
     store.setName(name)

     store.setWorkspace(self._info)
     store.getConnectionParameters().putAll(data.params)
     for name, valu in attrs.iteritems():
       if hasattr(store, name):
         setattr(store, name, valu)

     cat.add(store)
     return store

  def keys(self):
     stores = self.catalog._catalog.getDataStoresByWorkspace(self._info);
     return [s.name for s in stores]

  def __getitem__(self, key):
     store = self.catalog._catalog.getDataStoreByName(self._info, key)
     if store:
       return Store(store, self.catalog)

     raise KeyError('No such store %s' % key)

  def __setitem__(self, key, val):
    self.add(key, val)

  def __iter__(self):
    return self.keys().__iter__()    

  def iterkeys(self):
    return self.keys().__iter__()

  def iteritems(self):
    for st in self.keys():
      yield (st, self.get(st))

Workspace = info(Workspace)
