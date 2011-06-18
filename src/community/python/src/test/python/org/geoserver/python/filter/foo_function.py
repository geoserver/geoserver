from geoserver.filter import function

@function
def foo(obj, bar, baz):
  return "bam"
 
@function
def acme(obj):
  return obj['foo'] == 'bar'