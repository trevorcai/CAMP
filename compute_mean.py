#!/usr/bin/env python2
import sys
from collections import defaultdict

def main(fname, wname):
    d = defaultdict(lambda: {'total': 0, 'cnt': 0})
    with open(fname, 'r') as f:
        name = None
        for line in f:
            splits = line.strip().split(',')
            if len(splits) == 1:
                name = splits[0]
                continue
            dd = d[(name, int(splits[0]))]
            dd['total'] += int(splits[-1])
            dd['cnt'] += 1

    with open(wname, 'w') as f:
        output = []
        for k,v in sorted(d.items()):
            mean = float(v['total']) / v['cnt']
            f.write(','.join([k[0], str(k[1]), str(mean)]) + '\n')

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
