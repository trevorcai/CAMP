package cache.admission;

import cache.MapNode;

import java.util.Random;

public class RandomAdmission implements AdmissionPolicy {
    private double maxCsr = 1;
    private final Random generator = new Random();

    @Override
    public boolean shouldAdmit(MapNode node) {
        double chance = node.getCsr() / maxCsr;
        double roll = generator.nextDouble();
        return (roll < chance);
    }

    @Override
    public void registerWrite(MapNode node) {
        double csr = node.getCsr();
        if (csr > maxCsr) {
            maxCsr = csr;
        }
    }
}
