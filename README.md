# Scaling CAMP

## What is CAMP?
[CAMP](http://dblab.usc.edu/users/papers/CAMPTR.pdf) is a cache eviction policy
that provides eviction sensitive to the size and cost of the elements within
the cache.
It approximates the behavior of the Greedy Dual Size algorithm, with
a tunable precision parameter.

#### Performance
With precision 0, CAMP imitates LRU and functions in O(1) time.
With infinite precision, CAMP imitates GDS with O(log n) time, where n is the
number of elements in the cache.

## Goal
This repository implements basic versions of LRU and CAMP, and experiments with
scaling both policies to efficiently serve several threads at once.

