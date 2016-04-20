package cache;

public interface Cache {
    String get(String key);
    boolean putIfAbsent(String key, String value, int cost, int size);
}
