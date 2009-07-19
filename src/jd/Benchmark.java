package jd;

public class Benchmark {

    private static final long DURATION = 5000;

    public static void main(String[] args) {
        Runnable r = new Runnable() {
            public void run() {
                while (true) {
                    long start = System.currentTimeMillis();
                    long i = 0;
                    while (System.currentTimeMillis() - start < DURATION) {
                        i++;
                        if (i == Long.MAX_VALUE) {
                            i = 0;
                            System.out.println("overflow");
                        }
                    }
                    System.out.println(start + " : " + (i / DURATION));
                }
            }
        };

        new Thread(r).start();
        new Thread(r).start();
        new Thread(r).start();
        // new Thread(r).start();
    }
}
