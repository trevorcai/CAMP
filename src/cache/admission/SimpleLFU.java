package cache.admission;

import cache.MapNode;

/** Basic version of TinyLFU */
public class SimpleLFU implements AdmissionPolicy {
    private final CountingBF bf = new CountingBF();
    @Override
    public boolean shouldAdmit(MapNode node, MapNode toEvict) {
        short newCount = bf.estimate(node.getKey());
        short evictCount = bf.estimate(node.getKey());

        return newCount >= evictCount;
    }

    @Override
    public void registerRead(MapNode node) {
        bf.increment(node.getKey());
    }

    @Override
    public void registerWrite(MapNode node) {
        bf.increment(node.getKey());
    }
}
