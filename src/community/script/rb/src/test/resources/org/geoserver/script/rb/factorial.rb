def run(value, args)
  return value.downto(1).inject(:*)
end