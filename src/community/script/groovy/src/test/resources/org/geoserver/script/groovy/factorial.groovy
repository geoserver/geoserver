def fac(n) { n == 0 ? 1 : n * fac(n - 1) }

def run(value, args) {
  fac(value)
}