package fixit.dreams;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class SingleInstanceLock {

    private static FileLock lock;
    private static FileChannel channel;

    public static boolean acquireLock() {
        try {
            File lockFile = new File(System.getProperty("user.home"), ".dreamapp.lock");
            channel = new RandomAccessFile(lockFile, "rw").getChannel();
            lock = channel.tryLock();
            return lock != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static void releaseLock() {
        try {
            if (lock != null) lock.release();
            if (channel != null) channel.close();
        } catch (Exception ignored) {}
    }
}