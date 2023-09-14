package deltix.ember.service.oms.risk.limits;

import deltix.anvil.util.annotation.Timestamp;
import deltix.anvil.util.clock.EpochClock;
import deltix.anvil.util.clock.SystemEpochClock;

import java.util.concurrent.TimeUnit;

public class ManualClock implements EpochClock {

    @Timestamp(TimeUnit.MILLISECONDS)
    public long time;

    public ManualClock (@Timestamp(TimeUnit.MILLISECONDS) long time) {
        this.time = time;
    }

    public ManualClock() {
        this.time = SystemEpochClock.INSTANCE.time();
    }

    @Override
    @Timestamp(TimeUnit.MILLISECONDS)
    public long time() {
        return time;
    }

    @Override
    @Timestamp(TimeUnit.NANOSECONDS)
    public long timeNs() {
        return time*NANOS_IN_MILLISECOND;
    }
}