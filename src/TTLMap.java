import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TTLMap<K,V> implements ITTLMap<K,V> {

    private final int maxSize = 100000;
    private int currentSize = 0;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    private HashMap<K,V> keyToValue = new HashMap<K, V>(); // maps key to value
    private HashMap<K, Long> keyToExpireTime = new HashMap<K, Long>(); // maps key to expire time
    private Comparator<HeapEntry> heapComparator = new Comparator<HeapEntry>() {
        @Override
        public int compare(HeapEntry x, HeapEntry y) {
            if (x.getTimestamp() < y.getTimestamp()) {
                return -1;
            }
            if (x.getTimestamp() > y.getTimestamp()) {
                return 1;
            }
            return 0;
        }
    };
//    PriorityQueue<HeapEntry> queue = new PriorityQueue<HeapEntry>(heapComparator);
    TreeSet<HeapEntry> entries = new TreeSet<HeapEntry>(heapComparator); // stores current active keys sorted on basis of heapComparator

    public TTLMap(){
        initialize();
    }
    public int getCurrentSize() {
        return currentSize;
    }
    void initialize() {
        new SweeperThread().start();
    }

    @Override
    public void put(K key, V value, int ttl) {
        long currentTimestamp = (long)((new Date().getTime())/1000);
        writeLock.lock(); // acquires write lock
        try{
            System.out.println("put key : " + key);
            if(keyToValue.containsKey(key)){ // if key already present
                long oldTimestamp = keyToExpireTime.get(key);
                keyToValue.put(key,value);
                keyToExpireTime.put(key,currentTimestamp+ttl);
                entries.remove(new HeapEntry(oldTimestamp,key));
                entries.add(new HeapEntry(currentTimestamp+ttl,key));
            }
            else{
                if(currentSize == maxSize){ // if max size reached remove first element from the set
                    HeapEntry h = entries.pollFirst();
                    keyToValue.remove(h.getKey());
                    keyToExpireTime.remove(h.getKey());
                    currentSize--;
                }
                keyToValue.put(key,value);
                keyToExpireTime.put(key,currentTimestamp+ttl);
                entries.add(new HeapEntry(currentTimestamp+ttl,key));
                currentSize++;
            }
        }
        finally {
            writeLock.unlock(); // releases write lock
        }
    }

    @Override
    public V get(K key) {
        long currentTimestamp = (long)((new Date().getTime())/1000);
        readLock.lock();
        try {
            V ret = keyToValue.get(key);
            return ret;
        }
        finally {
            readLock.unlock();
        }
    }
    // Class for storing TreeSet element
    public class HeapEntry{
        private long timestamp;
        private K key;
        public HeapEntry(long timestamp, K key){
            this.timestamp = timestamp;
            this.key = key;
        }

        public long getTimestamp() {
            return timestamp;
        }
        public K getKey(){
            return key;
        }
    }

    // Sweeper thread. Wakes up every 2 seconds and waits for the write lock on the resources
    public class SweeperThread extends Thread {
        private int wakeUpTime = 2000;
        @Override
        public void run() {
            while (true) {
                sweep();
                try {
                    Thread.sleep(wakeUpTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sweep() {
            long currentTimestamp = (long)((new Date().getTime())/1000);
            writeLock.lock();
            try{
                // removes elements from the start of the set which have expired
                Iterator<HeapEntry> it = entries.iterator();
                while(it.hasNext()){
                    HeapEntry h = it.next();
                    if(h.getTimestamp() <= currentTimestamp){
                        keyToValue.remove(h.getKey());
                        keyToExpireTime.remove(h.getKey());
                        it.remove();
                        currentSize--;
                    }
                    else
                        break;
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
            finally {
                long endTimestamp = (long)((new Date().getTime())/1000);
                writeLock.unlock();
            }
        }
    }

    public static void main(String[] args){
        // driver code to test ttl map
        TTLMap<Integer,Integer> tmap = new TTLMap<Integer, Integer>();
        System.out.println(tmap.getCurrentSize());
        tmap.put(1,4,8);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        tmap.put(1,5,7);
        System.out.println(tmap.getCurrentSize());
    }
}
