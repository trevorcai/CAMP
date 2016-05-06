package cache.admission;

import cache.MapNode;

public interface AdmissionPolicy {
    boolean shouldAdmit(MapNode node, MapNode toEvict);
    void registerRead(MapNode node);
    void registerWrite(MapNode node);
}
