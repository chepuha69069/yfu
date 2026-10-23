package applefarm;

/** Простой таймер в миллисекундах. */
public final class Stopwatch {

    private long last;

    public Stopwatch() {
        reset();
    }

    public void reset() {
        last = System.currentTimeMillis();
    }

    public boolean hasElapsed(double ms) {
        return (double) System.currentTimeMillis() - ms >= (double) last;
    }

    /** Делает вид, что с последнего сброса уже прошло указанное время. */
    public Stopwatch setElapsed(long ms) {
        last = System.currentTimeMillis() - ms;
        return this;
    }
}
