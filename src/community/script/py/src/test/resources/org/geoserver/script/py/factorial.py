def run(value, args):
  return reduce(lambda x,y: x*y, range(1, value+1))