public interface ITTLMap <K,V>{
    public void put(K key, V value, int ttl);
    public V get(K key);
}
