def info(clazz):
   
   def __getattr__(self, name):
     try:
       if name != "_info" and self._info:
         return getattr(self._info, name)
     except AttributeError:
       pass

     return object.__getattribute__(self, name)

   def __setattr__(self, name, value):
     if name != "_info" and self.__dict__.has_key("_info"):
       try:
         setattr(self._info, name, value)
       except AttributeError:
         object.__setattr__(self, name, value)
     else:
       object.__setattr__(self, name, value)

   def save(self):
     if not self._info.getId():
        self.catalog._catalog.add(self._info)
     else:
        self.catalog._catalog.save(self._info)

   clazz.__getattr__ = __getattr__
   clazz.__setattr__ = __setattr__
   clazz.save = save

   return clazz 

class lazy(object):
   """ 
   Decorator for lazy property evaluation.
   """
   def __init__(self, func):
      self._func = func;

   def __get__(self, obj, owner):
     if obj is None:
        return self

     value = self._func(obj)
     setattr(obj, self._func.func_name, value)
     return value
