package deltix.ember.service.oms.risk.sample;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import deltix.ember.message.trade.*;
import deltix.ember.service.data.OrderState;
import deltix.ember.service.oms.risk.api.*;
import deltix.gflog.Log;
import deltix.gflog.LogFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Max Loss Risk Rule implementation.
 * Risk Rule checks realized P&L after every fill and cancels all active order when the loss exceeds
 * the value set in maxLoss. It will also reject new orders when the limit is breached.
 */
public class SamplePnLRiskRule extends AbstractRiskRule {
    private static final Log LOGGER = LogFactory.getLog(SamplePnLRiskRule.class);

    public static final String MaxLoss = "MaxLoss";

    private @Decimal long maxLoss = Decimal64Utils.NULL; // NULL if unlimited

    protected final List<RiskOrder> activeOrders = new ArrayList<>(128);

    private RiskManagerContext context;
    private PositionView position;

    public SamplePnLRiskRule() {
        super("PnL Limits");
    }

    @Override
    public void setLimits(RiskLimits riskLimits) {
        setMaxPosition((riskLimits == null) ? -1 : riskLimits.getDoubleLimit(MaxLoss, -1));
    }

    @Override
    public Object getCurrentValue(String limitName) {
        return (position == null) ? null :
                Decimal64Utils.abs(Decimal64Utils.min(position.getRealizedPnL(), Decimal64Utils.ZERO));
    }

    public double getMaxLoss() {
        return Decimal64Utils.toDouble(maxLoss);
    }

    /** @param maxLoss not positive if unlimited */
    public void setMaxPosition(double maxLoss) {
        this.maxLoss = (maxLoss >= 0) ? Decimal64Utils.fromDouble(maxLoss) : Decimal64Utils.NULL;
    }

    @Override
    public void onLive(RiskManagerContext context) {
        this.context = context;
        this.position = getParentGroup().getPositionView();
        if (this.position == null)
            LOGGER.error("MaxLoss limit must be defined on Position projection");
    }

    /**
     * Process new order submission request before it goes to destination venue
     * @param order order state
     * @param request original order submission request
     * @param observer veto/halt callback. Will be <code>NULL</code> during warm-up mode.
     */
    @Override
    public void onNewOrderRequest(@Nonnull RiskOrder order, @Nonnull OrderNewRequest request, @Nullable RiskObserver observer) {
        activeOrders.add(order);
        checkLimits(order, request, observer);
    }

    /**
     * Process order replacement (cancel/replace) request before it goes out to destination venue
     * @param order current state of the order that will be modified
     * @param request cancel/replace request
     * @param observer veto/halt callback. Will be <code>NULL</code> during warm-up mode.
     */
    @Override
    public void onReplaceOrderRequest(@Nonnull RiskOrder order, @Nonnull OrderReplaceRequest request, @Nullable RiskObserver observer) {
        checkLimits(order, request, observer);
    }

    /**
     * Process order state event after it was processed by OMS
     * @param order current state of the order (the event already applied)
     * @param event event that needs to be processed
     * @param previousState order state prior to this event
     * @param observer veto/halt callback. Will be <code>NULL</code> during warm-up mode.
     */    @Override
    public void onOrderEvent(@Nonnull RiskOrder order, @Nonnull OrderEvent event, @Nonnull OrderState previousState, @Nullable RiskObserver observer) {
        if (activeOrders.size() > 0 && event instanceof OrderTradeReportEvent) {
            if (observer != null && ! Decimal64Utils.isNull(maxLoss)) {
                @Decimal long realizedPnL = position.getRealizedPnL();
                if (Decimal64Utils.isNegative(realizedPnL) && Decimal64Utils.isGreater(Decimal64Utils.abs(realizedPnL), maxLoss)) {
                    for (RiskOrder activeOrder : activeOrders) {
                        MutableOrderCancelRequest request = new MutableOrderCancelRequest();
                        request.setTimestamp(context.getClock().time());
                        request.setRequestId("cancel_" + activeOrder.getOrderId());
                        request.setSourceId(activeOrder.getSourceId());
                        request.setOrderId(activeOrder.getOrderId());
                        request.setDestinationId(activeOrder.getDestinationId());
                        request.setReason("MaxLoss limit breached");

                        context.sendCancelOrderRequest(request);
                    }
                }
            }
        }

        if (order.isFinal())
            activeOrders.remove(order);
    }

    private void checkLimits(RiskOrder order, OrderEntryRequest request, RiskObserver observer) {
        if (observer != null && ! Decimal64Utils.isNull(maxLoss)) {
            @Decimal long realizedPnL = position.getRealizedPnL();
            if (Decimal64Utils.isNegative(realizedPnL) && Decimal64Utils.isGreater(Decimal64Utils.abs(realizedPnL), maxLoss)) {
                observer.onBreach(getProjectionPath(), MaxLoss, 0, formatMessage(realizedPnL, request));
            }
        }
    }

    private CharSequence formatMessage(@Decimal long actualPosition, OrderEntryRequest request) {
        return getCleanBuffer()
                .append("Realized Loss ").appendDecimal64(actualPosition)
                .append(" exceeds maximum ").appendDecimal64(maxLoss);
    }
}