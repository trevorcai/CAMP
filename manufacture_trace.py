import random
import sys

costs = [1, 3600, 86400]
sizes = [1198, 83038]

# Record key -> costs/sizes index
d = {}

with open(sys.argv[1], 'r') as f, open(sys.argv[2], 'w') as w_f:
    for line in f:
        splits = line.strip().split(',', 2)
        if splits[1] in d:
            size, cost = d[splits[1]]
        else:
            size = random.choice(sizes)
            cost = random.choice(costs)
            d[splits[1]] = (size, cost)

        w_line = ',' + ','.join([splits[1], str(size), str(cost)]) + '\n'
        w_f.write(w_line)

