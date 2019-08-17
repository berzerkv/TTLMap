import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TTLMap<K,V> implements ITTLMap<K,V> {

    private final int maxSize = 100000;
    private int currentSize = 0;

    public int getCurrentSize() {
        return currentSize;
    }

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    private HashMap<K,V> keyToValue = new HashMap<K, V>();
    private HashMap<K,Integer> keyToExpireTime = new HashMap<K, Integer>();
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
    TreeSet<HeapEntry> entries = new TreeSet<HeapEntry>(heapComparator);

    public void TTLMap(){
        initialize();
    }

    void initialize() {
        new SweeperThread().start();
    }

    @Override
    public void put(K key, V value, int ttl) {
        long currentTimestamp = (long)((new Date().getTime())/1000);
        writeLock.lock();
        try{
            if(keyToValue.containsKey(key)){
                long oldTimestamp = keyToExpireTime.get(key);
                keyToValue.put(key,value);
                keyToExpireTime.put(key,ttl);
                entries.remove(new HeapEntry(oldTimestamp,key));
                entries.add(new HeapEntry(currentTimestamp+ttl,key));
            }
            else{
                if(currentSize == maxSize){
                    entries.pollFirst();
                    currentSize--;
                }
                entries.add(new HeapEntry(currentTimestamp+ttl,key));
                currentSize++;
            }
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public V get(K key) {
        readLock.lock();
        try {
            V ret = keyToValue.get(key);
            return ret;
        }
        finally {
            readLock.unlock();
        }
    }

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
                writeLock.unlock();
            }
        }
    }

    public static void main(String[] args){
        TTLMap<Integer,Integer> tmap = new TTLMap<Integer, Integer>();
        tmap.put(1,4,5);
        System.out.println(tmap.getCurrentSize());
        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(tmap.getCurrentSize());

    }
}
