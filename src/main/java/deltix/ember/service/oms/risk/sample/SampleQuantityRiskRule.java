package deltix.ember.service.oms.risk.sample;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import deltix.ember.message.trade.OrderEntryRequest;
import deltix.ember.message.trade.OrderNewRequest;
import deltix.ember.message.trade.OrderReplaceRequest;
import deltix.ember.service.oms.risk.api.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Sample Max Order Quantity RiskRule implementation.
 * Limits the quantity of the Order to the value set in maxQuantity.
 */
public class SampleQuantityRiskRule extends AbstractRiskRule {
    public static final String MaxQuantity = "MaxQuantity";

    @Decimal
    private long maxQuantity = Decimal64Utils.NULL; // NULL if unlimited

    public SampleQuantityRiskRule() {
        super("Max Order Quantity");
    }

    @Override
    public void setLimits(RiskLimits riskLimits) {
        setMaxQuantity((riskLimits == null) ? -1 : riskLimits.getIntLimit(MaxQuantity, -1));
    }

    public long getMaxQuantity() {
        return Decimal64Utils.toLong(maxQuantity);
    }

    // negative maxQuantity for unlimited
    public void setMaxQuantity(long maxQuantity) {
        this.maxQuantity = (maxQuantity >= 0) ? Decimal64Utils.fromLong(maxQuantity) : Decimal64Utils.NULL;
    }

    /**
     * Process new order submission request before it goes to destination venue
     * @param order order state
     * @param request original order submission request
     * @param observer veto/halt callback. Will be <code>NULL</code> during warm-up mode.
     */
    @Override
    public void onNewOrderRequest(@Nonnull RiskOrder order, @Nonnull OrderNewRequest request, @Nullable RiskObserver observer) {
        validateQuantity(request, observer);
    }

    /**
     * Process order replacement (cancel/replace) request before it goes out to destination venue
     * @param order current state of the order that will be modified
     * @param request cancel/replace request
     * @param observer veto/halt callback. Will be <code>NULL</code> during warm-up mode.
     */
    @Override
    public void onReplaceOrderRequest(@Nonnull RiskOrder order, @Nonnull OrderReplaceRequest request, @Nullable RiskObserver observer) {
        validateQuantity(request, observer);
    }

    private void validateQuantity(@Nonnull OrderEntryRequest request, @Nullable RiskObserver observer) {
        if (observer != null && !Decimal64Utils.isNull(maxQuantity) && Decimal64Utils.isGreater(request.getQuantity(), maxQuantity))
            observer.onBreach(getProjectionPath(), MaxQuantity, DeltixRiskCodes.MAX_ORDER_SIZE.ordinal(), formatMessage(request));
    }

    private CharSequence formatMessage(OrderEntryRequest request) {
        return getCleanBuffer()
                .append("Order quantity ").appendDecimal64(request.getQuantity())
                .append(" exceeds maximum ").appendDecimal64(maxQuantity);
    }
}