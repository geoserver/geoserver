from geoscript.feature import Feature, Schema

def vector_format(name, mime):
  """
  Dectorator for a vector format.
  """
  def wrap(func):
     def wrapper(features, output):
        return func([_FeatureCollection(f) for f in features], output)
     
     wrapper.__vector_format__ = None
     wrapper.name = name
     wrapper.mime = mime
     return wrapper
     
  return wrap
  

class _FeatureCollection(object):
  
  def __init__(self, features):
      self.schema = Schema(ft=features.getSchema())
      self.features = _FeatureIterator(features)
      
class _FeatureIterator(object):

  def __init__(self, features):
    self.it = features.features()
    
  def __iter__(self):
     return self
     
  def next(self):
    if self.it.hasNext():
       return Feature(f=self.it.next())
       
    self.it.close()
    raise StopIteration
      
