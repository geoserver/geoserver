from geoscript.feature import Feature, Schema

def vector_format(name, mime):
  """
  Decorator for a vector format.
  """
  def wrap(func):
     def wrapper(features, output):
        def f(fc1, fc2):
          fc2.next = fc1 
          return fc2
        
        list = [_FeatureCollection(f) for f in features]
        list.reverse()
        data = reduce(f, list)
        return func(data, output)
     
     wrapper.__vector_format__ = None
     wrapper.name = name
     wrapper.mime = mime
     return wrapper
     
  return wrap

class _FeatureCollection(object):
  
  def __init__(self, features, next=None):
      self.schema = Schema(ft=features.getSchema())
      self.features = _FeatureIterator(features)
      self.next = next
      
  class Iterator(object):
     def __init__(self, item): 
       self.item = item

     def next(self):
       if not self.item: 
          raise StopIteration

       it = self.item
       self.item = self.item.next
       return it

  def __iter__(self):
     return _FeatureCollection.Iterator(self)

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