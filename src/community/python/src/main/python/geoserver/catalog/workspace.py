from geoserver.catalog.store import Store
from geoserver.catalog.util import info
from org.geoserver.catalog import WorkspaceInfo

class Workspace(object):
  """
  A GeoServer workspace/namespace.

  This class behaves like a dictionary in which the keys are ``str`` that 
  correspond to store names and the values are instances of 
  :class:`<Store> geoserver.catalog.Store`. 

  *workspace* is the workspace name (equivalent to namespace prefix). If the name 
  does not correspond to an existing workspace in the catalog one will be created
  "disconnected" from the catalog. If no name is specified then it assumed to mean
  the default workspace in the catalog.
  """

  def __init__(self, workspace=None, catalog=None):
     if not catalog:
       from geoserver.catalog import Catalog
       catalog = Catalog()
     self.catalog = catalog

     if isinstance(workspace, WorkspaceInfo):
       self._info = workspace
     elif isinstance(workspace, (str,unicode)):
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

     self._nsinfo = catalog._catalog.getNamespaceByPrefix(self._info.name)

  def geturi(self):
    return self._nsinfo.getURI()
  uri = property(geturi)

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
       return Store(store, self, self.catalog)

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
