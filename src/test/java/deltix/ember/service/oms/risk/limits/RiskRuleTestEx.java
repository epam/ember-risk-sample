package deltix.ember.service.oms.risk.limits;

import deltix.anvil.util.annotation.Timestamp;
import deltix.anvil.util.clock.EpochClock;
import deltix.anvil.util.clock.SystemEpochClock;
import deltix.anvil.util.timer.ExclusiveTimer;
import deltix.anvil.util.timer.Timer;
import deltix.calendar.providers.TradingCalendarProvider;
import deltix.ember.message.trade.OrderCancelRequest;
import deltix.ember.message.trade.OrderMassCancelRequest;
import deltix.ember.service.oms.risk.api.RiskManagerContext;
import deltix.ember.service.oms.risk.api.RiskObserver;
import deltix.ember.service.oms.risk.api.RiskOrder;
import deltix.ember.service.oms.risk.api.RiskRule;
import deltix.ember.service.op.SecurityMetadataProvider;
import deltix.ember.service.price.api.PricingService;
import deltix.qsrv.hf.tickdb.pub.WritableTickDB;
import org.junit.Assert;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * RiskRuleTest that implements RiskRuleContainer
 */
public abstract class RiskRuleTestEx<R extends RiskRule> extends RiskRuleTest<R> implements RiskManagerContext {

    protected EpochClock clock = SystemEpochClock.INSTANCE;
    protected final ExclusiveTimer timer = new ExclusiveTimer();
    protected boolean isLive = false;

    /// region RiskRuleContainer
    @Override
    public boolean isLive() {
        return isLive;
    }

    public R onLive(R rule) {
        isLive = true;
        rule.onLive(this);
        return rule;
    }

    @Override
    public Timer getTimer() {
        return timer;
    }

    @Override
    public EpochClock getClock() {
        return clock;
    }

    @Override
    protected long currentTimeMillis() {
        return clock.time();
    }

    @Override
    public RiskObserver getTradeHaltHandler() {
        return (path, limitName, rejectCode, reason) -> Assert.fail("Unexpected failure: " + reason);
    }

    @Override
    public WritableTickDB getTimeBase() {
        return null;
    }

    @Override
    public TradingCalendarProvider getTradingCalendarProvider() {
        return null;
    }

    @Override
    public PricingService getPricingService() {
        return smd.getPricingService();
    }

    @Override
    public <Cookie> boolean iterateActiveOrders(BiPredicate<RiskOrder, Cookie> visitor, Cookie cookie) {
        return cache.iterateActive(visitor, cookie);
    }

    @Override
    public SecurityMetadataProvider getSecurityMetadataProvider() {
        throw new UnsupportedOperationException(); // please ask
    }

    @Override
    public boolean isDMAPositionMode() {
        return false;
    }

    protected @Timestamp long setClock(@Timestamp long time) {
        clock = () -> time;
        return time;
    }

    /** @param datetime timestamp in yyyy-MM-dd HH:mm:ss.SSS zzz format */
    protected @Timestamp long setClock(String datetime) {
        @Timestamp long time = parseTimestamp (datetime);
        clock = () -> time;
        return time;
    }

    /** @param duration defines interval, can be negative (to go in the past) */
    protected long advanceClock (Duration duration) {
        @Timestamp long now = clock.time();
        return setClock(now + duration.toMillis());
    }

    /** @param datetime yyyy-MM-dd HH:mm:ss.S z*/
    protected static @Timestamp long parseTimestamp (String datetime) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS zzz", Locale.US).parse(datetime).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    protected R init (R rule) {
        //rule.onInit(this);
        isLive = true;
        rule.onLive(this);
        return rule;
    }

    @Override
    public void visit(Consumer<RiskRule> consumer) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void sendCancelOrderRequest(OrderCancelRequest orderCancelRequest) {

    }

    @Override
    public void sendCancelMassOrderRequest(OrderMassCancelRequest orderMassCancelRequest) {

    }
}
