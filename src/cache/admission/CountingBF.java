package cache.admission;

public class CountingBF {
    private static final int K = 3;
    private static final int BF_SIZE = 512;
    private static final int BF_MASK = BF_SIZE - 1;

    private final short[] countingBF;

    public CountingBF() {
        countingBF = new short[BF_SIZE];
    }

    /* Increments the minimum values in the countingBF that correspond to key.
       Hashing strategy here isn't great - this is an imitation of Guava's
       murmur128 hashing, but using hashcode() instead of the murmur hash.
       TODO Import Guava, use correct murmur hash
     */
    public void increment(Object key) {
        // Copied from estimate() so we don't hash twice
        int hash = key.hashCode();
        int hash2 = hash >> 16;
        int combined = hash;
        short min = Short.MAX_VALUE;
        for (int i = 0; i < K; i++) {
            int index = combined & BF_MASK;
            if (countingBF[index] < min) {
                min = countingBF[index];
            }
            combined += hash2;
        }

        combined = hash;
        for (int i = 0; i < K; i++) {
            int index = combined & BF_MASK;
            if (countingBF[index] == min) {
                countingBF[index]++;
            }
            combined += hash2;
        }
    }

    public short estimate(Object key) {
        int hash = key.hashCode();
        int hash2 = hash >> 16;
        int combined = hash;
        short min = Short.MAX_VALUE;
        for (int i = 0; i < K; i++) {
            int index = combined & BF_MASK;
            if (countingBF[index] < min) {
                min = countingBF[index];
            }
            combined += hash2;
        }
        return min;
    }
}
