public interface ITTLMap {
    public void put(int key, int value, int ttl);
    public int get(int key);
}
