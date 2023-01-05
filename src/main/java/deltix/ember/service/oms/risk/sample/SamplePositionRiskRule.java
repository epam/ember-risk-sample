package deltix.ember.service.oms.risk.sample;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import deltix.ember.message.trade.OrderEntryRequest;
import deltix.ember.message.trade.OrderNewRequest;
import deltix.ember.message.trade.OrderReplaceRequest;
import deltix.ember.message.trade.Side;
import deltix.ember.service.oms.risk.api.*;
import deltix.gflog.Log;
import deltix.gflog.LogFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Sample Max Position Size RiskRule implementation.
 * Rejects new order when estimated LONG or SHORT position exceeds the value set in maxPosition.
 * This RiskRule is using PositionView API which is available to risk rules defined
 * at position projections: Symbol, Currency or RootSymbol
 */
public class SamplePositionRiskRule extends AbstractRiskRule {

    public static final String MaxPosition = "MaxPosition";

    private static final Log LOGGER = LogFactory.getLog(SamplePositionRiskRule.class);

    private PositionView position;

    private @Decimal long maxPosition = Decimal64Utils.NULL; // NULL if unlimited

    public SamplePositionRiskRule() {
        super("Max Position");
    }

    @Override
    public void setLimits(RiskLimits riskLimits) {
        setMaxPosition((riskLimits == null) ? -1 : riskLimits.getDoubleLimit(MaxPosition, -1));
    }

    @Override
    public Object getCurrentValue(String limitName) {
        return (position == null) ? null :
            Decimal64Utils.abs(Decimal64Utils.add(position.getActualPositionSize(), Decimal64Utils.subtract(position.getOpenBuySize(), position.getOpenSellSize())));
    }

    public double getMaxPosition() {
        return Decimal64Utils.toDouble(maxPosition);
    }

    // negative maxPosition or unlimited
    public void setMaxPosition(double maxPositon) {
        this.maxPosition = (maxPositon >= 0) ? Decimal64Utils.fromDouble(maxPosition) : Decimal64Utils.NULL;
    }

    @Override
    public void onLive(RiskManagerContext context) {
        // Initialize PositionView. getPositionView() can return null
        // if risk rule was not defined in position projection
        this.position = getParentGroup().getPositionView();
        if (this.position == null)
           LOGGER.error("MaxPosition limit must be defined on Position projection");
    }

    /**
     * Process new order submission request before it goes to destination venue
     * @param order order state
     * @param request original order submission request
     * @param observer veto/halt callback. Will be <code>NULL</code> during warm-up mode.
     */
    @Override
    public void onNewOrderRequest(@Nonnull RiskOrder order, @Nonnull OrderNewRequest request, @Nullable RiskObserver observer) {
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

    private void checkLimits(RiskOrder order, OrderEntryRequest request, RiskObserver observer) {
        if (observer != null && ! Decimal64Utils.isNull(maxPosition)) {
            boolean isBuyOrder = (order.getSide() == Side.BUY);
            @Decimal long positionSize = isBuyOrder ?
                    Decimal64Utils.add(position.getActualPositionSize(), position.getOpenBuySize()) :
                    Decimal64Utils.subtract(position.getOpenSellSize(), position.getActualPositionSize());

            if (Decimal64Utils.isGreater(positionSize, maxPosition)) {
                CharSequence message = getCleanBuffer()
                        .append("Estimated ").append(isBuyOrder ? "LONG" : "SHORT").append(" position ").appendDecimal64(positionSize)
                        .append(" would exceed maximum limit ").appendDecimal64(maxPosition);

                observer.onBreach(getProjectionPath(), MaxPosition, DeltixRiskCodes.MAX_NET_POSITION_SIZE.ordinal(), message);
            }
        }
    }
}
