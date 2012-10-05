from geoserver.catalog.workspace import Workspace

class Catalog(object):
  """
  The GeoServer catalog. 

  This class behaves like a dictionary in which the keys are ``str`` that 
  correspond to namespace prefixes / workspace names. The values are instances
  of :class:`Workspace <geoserver.catalog.Workspace>`. 

  >>> cat = Catalog()
  >>> cat.keys()
  [u'cite', u'tiger', u'topp', u'sf']
  >>> sf = ws['sf']
  """
  def __init__(self):
     try:
       from org.geoserver.platform import GeoServerExtensions
       from org.geoserver.catalog import CatalogBuilder
     except ImportError:
       pass
     else:
       self._catalog = GeoServerExtensions.bean('catalog') 
     
  def builder(self):
     from org.geoserver.catalog import CatalogBuilder
     return CatalogBuilder(self._catalog)

  def keys(self):
     return [ws.getName() for ws in self._catalog.getWorkspaces()]

  def __getitem__(self, key):
    # accept None as default workspace
    key = key or self._catalog.getDefaultWorkspace().getName()
    ws = self._catalog.getWorkspaceByName(key) 
    if not ws:
      raise KeyError('No such folder %s' % key)
     
    return Workspace(ws, self)

  def __setitem__(self, key, val):
    if not key:    
       # set default workspace
       self._catalog.setDefaultWorkspace(val._workspace)
    else:
       val.save() 

  def __iter__(self):
    return self.keys().__iter__()    

  def iterkeys(self):
    return self.__iter__()

  def iteritems(self):
    for st in self.keys():
      yield (st, self.__getitem__(st))

