* Implementation of a Key Value store with TTL for keys. Uses LRU to displace keys if MAX_SIZE reached
* One can refer to Ideas.txt as to how I approached the problem and came to the current solution.
* I have designed the data structure to support multiple threads working on the data structure at the same time using a read write lock.
* Multiple threads can read the key values but only one can write at a time.
* A sweeper thread wakes up every 2 seconds and removes expired items that have expired.
