package cache;

public interface Cache {
    String getIfPresent(String key);
    void put(String key, String value, int cost, int size);
}
