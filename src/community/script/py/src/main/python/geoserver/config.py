from geoserver import util

class GeoServer(object):
  """
  The GeoServer configuration. 

  >>> gs = GeoServer()
  """
  def __init__(self):
     try:
       from org.geoserver.platform import GeoServerExtensions
     except ImportError:
       pass
     else:
       self._geoserver = GeoServerExtensions.bean('geoServer') 

  def getcatalog(self):
     from geoserver.catalog import Catalog
     return Catalog()
  catalog = property(getcatalog, None, None, 
    'The GeoServer :class:`Catalog <geoserver.catalog.Catalog>`')

  def getconfig(self):
     return Config(self._geoserver.getGlobal(), self)
  config = property(getconfig, None, None, 'The GeoServer configuration.')

class Config(object):

  def __init__(self, config, geoserver):
    self.geoserver = geoserver
    self._info = config

Config = util.info(Config)

    
