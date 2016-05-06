package cache.admission;

import cache.MapNode;

import java.util.Random;

public class WeightedAdmission implements AdmissionPolicy {
    private double maxCsr = 1;
    private final Random generator = new Random();

    @Override
    public boolean shouldAdmit(MapNode node, MapNode toEvict) {
        double chance = node.getCsr() / maxCsr;
        double roll = generator.nextDouble();
        return (roll < chance);
    }

    @Override
    public void registerRead(MapNode node) {}

    @Override
    public void registerWrite(MapNode node) {
        double csr = node.getCsr();
        if (csr > maxCsr) {
            maxCsr = csr;
        }
    }
}
