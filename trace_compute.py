import sys

d = {}
with open(sys.argv[1], 'r') as f:
    for line in f:
        splits = line.strip().split(',')
        if splits[1] not in d:
            d[splits[1]] = int(splits[2])

total_cost = sum(d.values())

print total_cost
