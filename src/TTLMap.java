public class TTLMap implements ITTLMap {

    private final int maxSize = 100000;
    private int currentSize = 0;
    // 2 hash maps and 1 min heap declaration

    public void TTLMap(){

    }

    @Override
    public void put(int key, int value, int ttl) {

    }

    @Override
    public int get(int key) {
        return 0;
    }

    class SweeperThread extends Thread {
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

        }
    }
}
