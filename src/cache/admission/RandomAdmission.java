package cache.admission;

import cache.MapNode;

import java.util.Random;

public class RandomAdmission implements AdmissionPolicy {
    private final float chance;
    private final Random generator = new Random();

    public RandomAdmission(float chance) {
        this.chance = chance;
    }

    @Override
    public boolean shouldAdmit(MapNode node, MapNode toEvict) {
        float roll = generator.nextFloat();
        return (roll < chance);
    }

    @Override
    public void registerRead(MapNode node) {}

    @Override
    public void registerWrite(MapNode node) {}
}
