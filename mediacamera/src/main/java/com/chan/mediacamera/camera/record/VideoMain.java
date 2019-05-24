package com.chan.mediacamera.camera.record;

/**
 * 三线程控制音视频录制的大概思路
 *
 * Obj.wait()，与Obj.notify()必须要与synchronized(Obj)一起使用，
 * 也就是wait,与notify是针对已经获取了Obj锁进行操作，从语法角度来说就是Obj.wait(),
 * Obj.notify必须在synchronized(Obj){...}语句块内。
 * 从功能上来说wait就是说线程在获取对象锁后，主动释放对象锁，同时本线程休眠。
 * 直到有其它线程调用对象的notify()唤醒该线程，才能继续获取对象锁，并继续执行。
 * 相应的notify()就是对对象锁的唤醒操作。
 * 但有一点需要注意的是notify()调用后，并不是马上就释放对象锁的，而是在相应的synchronized(){}语句块执行结束，
 * 自动释放锁后，JVM会在wait()对象锁的线程中随机选取一线程，赋予其对象锁，唤醒线程，继续执行。
 * 这样就提供了在线程间同步、唤醒的操作。
 * Thread.sleep()与Object.wait()二者都可以暂停当前线程，释放CPU控制权，
 * 主要的区别在于Object.wait()在释放CPU同时，释放了对象锁的控制。
 */
public class VideoMain {

    private byte[] VideoObject = new byte[0];
    private byte[] AudioObject = new byte[0];
    private byte[] MixtureObject = new byte[0];

    public static void main(String[] args) {
        VideoMain main = new VideoMain();
        MixtureThread mixtureThread = main.new MixtureThread();
        mixtureThread.start();
    }

    class MixtureThread extends Thread {

        volatile int i = 1;
        volatile boolean stop = false;
        VideoThread videoThread;
        AudioThread audioThread;

        public MixtureThread() {
            videoThread = new VideoThread();
            audioThread = new AudioThread();
            videoThread.start();
            audioThread.start();
        }


        @Override
        public void run() {
            while (!stop) {
                try {
                    synchronized (MixtureObject) {
                        synchronized (VideoObject) {
                            System.out.println("合成视频" + i);
                            VideoObject.notifyAll();
                            i++;
                            if (i > 6) {
                                stop = true;
                            }
                        }
                        MixtureObject.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


        class VideoThread extends Thread {
            @Override
            public void run() {
                consume();
            }

            private void consume() {
                while (!stop) {
                    try {
                        synchronized (VideoObject) {
                            synchronized (AudioObject) {
                                System.out.println("插入视频" + i);
                                AudioObject.notifyAll();
                            }
                            VideoObject.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        class AudioThread extends Thread {
            @Override
            public void run() {
                produce();
            }

            private void produce() {
                while (!stop) {
                    try {
                        synchronized (AudioObject) {
                            synchronized (MixtureObject) {
                                System.out.println("插入音频" + i);
                                MixtureObject.notifyAll();
                            }
                            AudioObject.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }


}
