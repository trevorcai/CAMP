package cache.admission;

import cache.MapNode;

/** Admission policy that admits every node */
public class IdlePolicy implements AdmissionPolicy {
    private static IdlePolicy singleton = new IdlePolicy();
    private IdlePolicy() {}

    public static IdlePolicy getInstance() {
        return  singleton;
    }

    @Override
    public boolean shouldAdmit(MapNode node) {
        return true;
    }

    @Override
    public void registerWrite(MapNode node) {}
}
