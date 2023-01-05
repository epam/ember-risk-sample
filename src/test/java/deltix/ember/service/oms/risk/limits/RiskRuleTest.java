package deltix.ember.service.oms.risk.limits;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import deltix.anvil.util.CharSequenceUtil;
import deltix.anvil.util.clock.SystemEpochClock;
import deltix.anvil.util.codec.AlphanumericCodec;
import deltix.ember.message.risk.ProjectionKey;
import deltix.ember.message.smd.InstrumentType;
import deltix.ember.message.smd.MutableBondUpdate;
import deltix.ember.message.smd.MutableEquityUpdate;
import deltix.ember.message.smd.MutableFutureUpdate;
import deltix.ember.message.trade.*;
import deltix.ember.service.data.InstrumentInfo;
import deltix.ember.service.data.OrderState;
import deltix.ember.service.oms.cache.OrdersCacheSettings;
import deltix.ember.service.oms.cache.TwoLevelOrdersCache;
import deltix.ember.service.oms.position.ConstProjectionPath;
import deltix.ember.service.oms.position.Projection;
import deltix.ember.service.oms.position.ProjectionPath;
import deltix.ember.service.oms.risk.api.RiskObserver;
import deltix.ember.service.oms.risk.api.RiskOrder;
import deltix.ember.service.oms.risk.api.RiskRule;
import deltix.ember.service.oms.util.FixedPricingService;
import deltix.ember.service.oms.util.OrderRequestBuilder;
import deltix.ember.service.oms.util.TestOrderProcessor;
import deltix.ember.service.op.OrderEventProcessor;
import deltix.ember.service.op.SimpleSecurityMetadataService;
import deltix.ember.service.valid.FullOrderRequestValidator;
import deltix.ember.service.valid.OrderRequestValidator;
import deltix.ember.service.valid.ValidationSettings;
import org.junit.Assert;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

public abstract class RiskRuleTest<R extends RiskRule> {
    private static final long SOURCE_ID = AlphanumericCodec.encode("SOURCE");
    private static final long DESTINATION_ID = AlphanumericCodec.encode("DEST");
    protected R rule;

    private final OrderRequestValidator validator = new FullOrderRequestValidator(ValidationSettings.makeWithDefault());
    protected final FixedPricingService ps = new FixedPricingService(Decimal64Utils.NULL);
    protected final SimpleSecurityMetadataService smd = new SimpleSecurityMetadataService(ps, 1000);
    protected final TwoLevelOrdersCache<RiskOrder> cache = new TwoLevelOrdersCache<>(RiskOrder::new, OrdersCacheSettings.create(1000, 1000, 3, 1));
    protected final OrderEventProcessor<RiskOrder> processor = new TestOrderProcessor<>(cache, validator);

    protected void addInstrument(String symbol, InstrumentType type, double price) {
        ps.addPrice(symbol, Decimal64Utils.fromDouble(price));
        switch (type) {
            case EQUITY:
                MutableEquityUpdate equity = new MutableEquityUpdate();
                equity.setSymbol(symbol);
                equity.setInstrumentType(InstrumentType.EQUITY);
                smd.onEquityUpdate(equity);
                break;
            case BOND:
                MutableBondUpdate bond = new MutableBondUpdate();
                bond.setSymbol(symbol);
                bond.setInstrumentType(InstrumentType.BOND);
                smd.onBondUpdate(bond);
                break;
            case FUTURE:
                MutableFutureUpdate future = new MutableFutureUpdate();
                future.setSymbol(symbol);
                future.setInstrumentType(InstrumentType.FUTURE);
                future.setRootSymbol(symbol.substring(0, 2)); // simplified
                smd.onFutureUpdate(future);
                break;
            default:
                throw new IllegalArgumentException(type.name());
        }
    }


    /// region Order Factory methods

    protected MutableOrderNewRequest newOrder(Side side, int quantity, String symbol, double price) {
        MutableOrderNewRequest result = newOrder(side, quantity, symbol);
        result.setOrderType(OrderType.LIMIT);
        result.setLimitPrice(Decimal64Utils.fromDouble(price));
        return result;
    }

    protected MutableOrderNewRequest newOrder(Side side, int quantity, String symbol) {
        OrderRequestBuilder result = new OrderRequestBuilder(new MutableOrderNewRequest(), nextOrderId(), SystemEpochClock.INSTANCE, null);
        result.dma(true);
        result.symbol(symbol);
        result.sourceId(SOURCE_ID);
        result.destinationId(DESTINATION_ID);
        result.quantity(Decimal64Utils.fromLong(quantity));
        result.instrumentType(getInstrumentType(symbol));
        result.side(side);
        return (MutableOrderNewRequest) result.build();
    }

    protected MutableOrderReplaceRequest cancelReplaceOrder(OrderEntryRequest originalOrder, int quantity) {
        return cancelReplaceOrder(originalOrder.getOrderId(), originalOrder.getSide(), quantity, originalOrder.getSymbol());
    }

    protected MutableOrderReplaceRequest cancelReplaceOrder(OrderEntryRequest originalOrder, Side side, int quantity, CharSequence symbol, double price) {
        MutableOrderReplaceRequest result = cancelReplaceOrder(originalOrder, side, quantity, symbol);
        result.setOrderType(OrderType.LIMIT);
        result.setLimitPrice(Decimal64Utils.fromDouble(price));
        return result;
    }

    protected MutableOrderReplaceRequest cancelReplaceOrder(OrderEntryRequest originalOrder, Side side, int quantity, CharSequence symbol) {
        return cancelReplaceOrder(originalOrder.getOrderId(), side, quantity, symbol);
    }

    protected MutableOrderReplaceRequest cancelReplaceOrder(CharSequence originalOrderId, Side side, int quantity, CharSequence symbol) {
        MutableOrderReplaceRequest result = new MutableOrderReplaceRequest();
        result.setOrderId(nextOrderId());
        result.setOriginalOrderId(originalOrderId);
        result.setSourceId(SOURCE_ID);
        result.setDestinationId(DESTINATION_ID);
        result.setSide(side);
        result.setQuantity(Decimal64Utils.fromLong(quantity));
        result.setSymbol(symbol);
        result.setInstrumentType(getInstrumentType(symbol));
        result.setOrderType(OrderType.MARKET);
        result.setTimeInForce(TimeInForce.DAY);
        result.setTimestamp(currentTimeMillis());
        return result;
    }

    protected MutableOrderCancelRequest cancelOrder(CharSequence orderId) {
        MutableOrderCancelRequest result = new MutableOrderCancelRequest();
        result.setRequestId(nextOrderId());
        result.setOrderId(orderId);
        result.setSourceId(SOURCE_ID);
        result.setDestinationId(DESTINATION_ID);
        result.setTimestamp(currentTimeMillis());
        return result;
    }

    private static String nextOrderId() {
        return Long.toString(nextOrderId++);
    }

    private static long nextOrderId = 1001;


    /// endregion

    /// region Events

    protected final HaltTradingRiskObserver haltTradingRiskObserver = new HaltTradingRiskObserver();
    private final RiskObserver defaltEventRiskObserver = (path, limitName, rejectCode, reason) -> fail("Unexpected Risk reject: " + reason);

    // NewOrderSingle  ack (pre-submitted)

    protected OrderNewRequest ackEvent(OrderNewRequest newOrderSingle) {
        return ackEvent(newOrderSingle, defaltEventRiskObserver);
    }

    protected OrderNewRequest ackEvent(OrderNewRequest newOrderSingle, RiskObserver riskObserver) {

        RiskOrder order = cache.get(newOrderSingle.getSourceId(), newOrderSingle.getOrderId());
        assertNotNull(order);

        MutableOrderPendingNewEvent event = new MutableOrderPendingNewEvent();
        event.setSourceId(newOrderSingle.getDestinationId());
        event.setDestinationId(newOrderSingle.getSourceId());
        event.setOrderId(newOrderSingle.getOrderId());
        event.setTimestamp(currentTimeMillis());
        event.setEventId(nextEventId());

        OrderState previousState = order.getState();
        processor.onOrderPendingNew(order, event);
        rule.onOrderEvent(order, event, previousState, riskObserver);
        return newOrderSingle;
    }

    // NewOrderSingle  confirmation (submitted)

    protected OrderNewRequest openEvent(OrderNewRequest newOrderSingle) {
        return openEvent(newOrderSingle, defaltEventRiskObserver);
    }

    protected OrderNewRequest openEvent(OrderNewRequest newOrderSingle, RiskObserver riskObserver) {

        RiskOrder order = cache.get(newOrderSingle.getSourceId(), newOrderSingle.getOrderId());
        assertNotNull(order);

        MutableOrderNewEvent event = new MutableOrderNewEvent();
        event.setSourceId(newOrderSingle.getDestinationId());
        event.setDestinationId(newOrderSingle.getSourceId());
        event.setOrderId(newOrderSingle.getOrderId());
        event.setTimestamp(currentTimeMillis());
        event.setEventId(nextEventId());

        OrderState previousState = order.getState();
        processor.onOrderNew(order, event);
        rule.onOrderEvent(order, event, previousState, riskObserver);
        return newOrderSingle;
    }

    // Cancel Replace confirmation

    protected OrderReplaceRequest openEvent(OrderReplaceRequest cancelReplace) {
        return openEvent(cancelReplace, defaltEventRiskObserver);
    }

    protected OrderReplaceRequest openEvent(OrderReplaceRequest cancelReplace, RiskObserver riskObserver) {

        RiskOrder order = cache.get(cancelReplace.getSourceId(), cancelReplace.getOrderId());
        assertNotNull(order);

        MutableOrderReplaceEvent event = new MutableOrderReplaceEvent();
        event.setSourceId(cancelReplace.getDestinationId());
        event.setDestinationId(cancelReplace.getSourceId());
        event.setOrderId(cancelReplace.getOrderId());
        event.setOriginalOrderId(cancelReplace.getOriginalOrderId());
        event.setTimestamp(currentTimeMillis());
        event.setEventId(nextEventId());

        OrderState previousState = order.getState();
        processor.onOrderReplace(order, event);
        rule.onOrderEvent(order, event, previousState, riskObserver);
        return cancelReplace;
    }

    //  Reject

    protected OrderNewRequest rejectEvent(OrderNewRequest newOrderSingle) {
        return rejectEvent(newOrderSingle, defaltEventRiskObserver);
    }

    protected OrderNewRequest rejectEvent(OrderNewRequest newOrderSingle, RiskObserver riskObserver) {

        RiskOrder order = cache.get(newOrderSingle.getSourceId(), newOrderSingle.getOrderId());
        assertNotNull(order);

        MutableOrderRejectEvent event = new MutableOrderRejectEvent();
        event.setSourceId(newOrderSingle.getDestinationId());
        event.setDestinationId(newOrderSingle.getSourceId());
        event.setOrderId(newOrderSingle.getOrderId());
        event.setTimestamp(currentTimeMillis());
        event.setReason("Some good rejection reason");
        event.setEventId(nextEventId());

        OrderState previousState = order.getState();

        processor.onOrderReject(order, event);
        rule.onOrderEvent(order, event, previousState, riskObserver);
        return newOrderSingle;
    }

    // Cancel

    protected OrderEntryRequest cancelEvent(OrderEntryRequest orderRequest) {
        return cancelEvent(orderRequest, defaltEventRiskObserver);
    }

    protected OrderEntryRequest cancelEvent(OrderEntryRequest orderRequest, RiskObserver riskObserver) {

        RiskOrder order = cache.get(orderRequest.getSourceId(), orderRequest.getOrderId());
        assertNotNull(order);

        MutableOrderCancelEvent event = new MutableOrderCancelEvent();
        event.setSourceId(orderRequest.getDestinationId());
        event.setDestinationId(orderRequest.getSourceId());
        event.setOrderId(orderRequest.getOrderId());
        event.setTimestamp(currentTimeMillis());
        event.setReason("Some good cancel reason");
        event.setEventId(nextEventId());

        OrderState previousState = order.getState();

        processor.onOrderCancel(order, event);
        rule.onOrderEvent(order, event, previousState, riskObserver);
        return orderRequest;
    }

    // Partial fill

    protected OrderEntryRequest partialFillEvent(OrderEntryRequest orderRequest, int fillSize) {
        return partialFillEvent(orderRequest, fillSize, defaltEventRiskObserver);
    }

    protected OrderEntryRequest partialFillEvent(OrderEntryRequest orderRequest, int fillSize, RiskObserver riskObserver) {

        RiskOrder order = cache.get(orderRequest.getSourceId(), orderRequest.getOrderId());
        assertNotNull(order);

        MutableOrderTradeReportEvent event = new MutableOrderTradeReportEvent();
        event.setSourceId(orderRequest.getDestinationId());
        event.setDestinationId(orderRequest.getSourceId());
        event.setOrderId(orderRequest.getOrderId());
        event.setTimestamp(currentTimeMillis());
        event.setTradePrice(guessTradePrice(orderRequest));
        event.setTradeQuantity(Decimal64Utils.fromLong(fillSize));
        event.setEventId(nextEventId());

        OrderState previousState = order.getState();
        processor.onTradeReport(order, event);
        rule.onOrderEvent(order, event, previousState, riskObserver);
        return orderRequest;
    }

    // Complete fill

    protected OrderEntryRequest completeFillEvent(OrderEntryRequest orderRequest) {
        return completeFillEvent(orderRequest, defaltEventRiskObserver);
    }

    protected OrderEntryRequest completeFillEvent(OrderEntryRequest orderRequest, RiskObserver riskObserver) {

        RiskOrder order = cache.get(orderRequest.getSourceId(), orderRequest.getOrderId());
        assertNotNull(order);

        MutableOrderTradeReportEvent event = new MutableOrderTradeReportEvent();
        event.setSourceId(orderRequest.getDestinationId());
        event.setDestinationId(orderRequest.getSourceId());
        event.setOrderId(orderRequest.getOrderId());
        event.setTimestamp(currentTimeMillis());
        event.setTradePrice(guessTradePrice(orderRequest));
        event.setTradeQuantity(orderRequest.getQuantity());
        event.setEventId(nextEventId());

        OrderState previousState = order.getState();
        processor.onTradeReport(order, event);
        rule.onOrderEvent(order, event, previousState, riskObserver);
        return orderRequest;
    }

    private @Decimal
    long guessTradePrice(OrderEntryRequest orderRequest) {
        if (!Decimal64Utils.isNaN(orderRequest.getLimitPrice()))
            return orderRequest.getLimitPrice();
        return ps.getInstrumentPrices(orderRequest.getSymbol()).getBestAvailableMarketPrice(orderRequest.getSide() == Side.BUY);
    }

    private static String nextEventId() {
        return "EVT" + Long.toString(nextEventId++);
    }

    private static long nextEventId = 1001;

    //  Cancel Reject

    protected OrderReplaceRequest cancelRejectEvent(OrderReplaceRequest cancelReplace) {
        return cancelRejectEvent(cancelReplace, defaltEventRiskObserver);
    }

    protected OrderReplaceRequest cancelRejectEvent(OrderReplaceRequest cancelReplace, RiskObserver riskObserver) {

        RiskOrder order = cache.get(cancelReplace.getSourceId(), cancelReplace.getOrderId());
        assertNotNull(order);

        MutableOrderCancelRejectEvent event = new MutableOrderCancelRejectEvent();
        event.setSourceId(cancelReplace.getDestinationId());
        event.setDestinationId(cancelReplace.getSourceId());
        event.setOrderId(cancelReplace.getOrderId());
        event.setTimestamp(currentTimeMillis());
        event.setReason("Some good rejection reason");

        OrderState previousState = order.getState();

        processor.onOrderCancelReject(order, event);
        rule.onOrderEvent(order, event, previousState, riskObserver);
        return cancelReplace;
    }

    /// endregion

    /// region Assertions


    protected OrderNewRequest assertValid(OrderNewRequest newOrderSingle) {
        return assertValid(newOrderSingle, (path, limitName, rejectCode, reason) -> fail("Unexpected Risk reject: " + reason));
    }

    protected OrderNewRequest assertValid(OrderNewRequest newOrderSingle, RiskObserver observer) {
        RiskOrder order = processor.placeOrder(newOrderSingle);
        order.setDMA(OrderRequestBuilder.isDMA(newOrderSingle));
        cache.add(order.getLastOrder());


        rule.onNewOrderRequest(order, newOrderSingle, observer);

        return newOrderSingle;
    }

    protected OrderReplaceRequest assertValid(OrderReplaceRequest cancelReplace) {
        return assertValid(cancelReplace, (path, limitName, rejectCode, reason) -> fail("Unexpected Risk reject: " + reason));
    }

    protected OrderReplaceRequest assertValid(OrderReplaceRequest cancelReplace, RiskObserver observer) {
        RiskOrder order = cache.get(cancelReplace.getSourceId(), cancelReplace.getOriginalOrderId());
        assertNotNull(order);
        processor.replaceOrder(order, cancelReplace);
        cache.add(order.getLastOrder());


        rule.onReplaceOrderRequest(order, cancelReplace, observer);

        return cancelReplace;
    }

    protected OrderNewRequest assertInvalid(OrderNewRequest newOrderSingle, String expectedMessage) {
        RiskOrder order = processor.placeOrder(newOrderSingle);
        order.setDMA(OrderRequestBuilder.isDMA(newOrderSingle));
        cache.add(order.getLastOrder());


        try {
            rule.onNewOrderRequest(order, newOrderSingle, RiskVetoException::onBreach);
            fail("Risk rule missed invalid order (Exepected rejection reason: " + expectedMessage + ')');
        } catch (RiskVetoException e) {
            String actualMessage = e.getMessage();
            if (!isPatternFind(expectedMessage, actualMessage)) {
                fail("Risk rule REJECTED order with unexpected reason: \n" + actualMessage + "\n Expected:\n" + expectedMessage);
            }

            rejectEvent(newOrderSingle);
        }
        return newOrderSingle;
    }

    protected OrderReplaceRequest assertInvalid(OrderReplaceRequest cancelReplace, String expectedMessage) {
        RiskOrder order = cache.get(cancelReplace.getSourceId(), cancelReplace.getOriginalOrderId());
        assertNotNull(order);
        cache.add(order.getLastOrder());
        processor.replaceOrder(order, cancelReplace);

        try {
            rule.onReplaceOrderRequest(order, cancelReplace, RiskVetoException::onBreach);
            fail("Risk rule missed invalid order (Expected rejection reason: " + expectedMessage + ')');
        } catch (RiskVetoException e) {
            String actualMessage = e.getMessage();
            if (!isPatternFind(expectedMessage, actualMessage)) {
                fail("Risk rule REJECTED order with unexpected reason: \n" + actualMessage + "\n Expected:\n" + expectedMessage);
            }

            cancelRejectEvent(cancelReplace);
        }
        return cancelReplace;
    }

    protected static boolean isPatternFind(String pattern, String value) {
        return Pattern.compile(pattern).matcher(value).find(); // substring
    }

    /// endregion

    public class HaltTradingRiskObserver implements RiskObserver {

        private String haltTradingReason;

        @Override
        public void onBreach(ConstProjectionPath path, String limitName, int rejectCode, CharSequence reason) {
            haltTradingReason = CharSequenceUtil.isEmptyOrNull(reason) ? "Risk Breached" : String.valueOf(reason);
        }

        public void assertTradingNotHalted() {
            assertTrue("Unexpected trading halt: " + haltTradingReason, haltTradingReason == null);
        }

        public void assertTradingHalted(String haltReasonPattern) {
            if (haltTradingReason == null)
                fail("Trading was supposed to halt with the following reason: " + haltReasonPattern);
            else
                assertTrue("Unexpected trading halt reason:\n" + haltTradingReason + "\nExpected:\n" + haltReasonPattern, isPatternFind(haltReasonPattern, haltTradingReason));
        }

        public void resumeTrading() {
            haltTradingReason = null;
        }
    }


    private static final class RiskVetoException extends RuntimeException {

        RiskVetoException(CharSequence error) {
            super(error.toString());
        }

        static void onBreach(ProjectionPath path, String limitName, int rejectCode, CharSequence reason) {
            throw new RiskVetoException(reason);
        }
    }

    private InstrumentType getInstrumentType(CharSequence symbol) {
        InstrumentInfo instrument = smd.get(symbol);
        Assert.assertNotNull("Can't find symbol in SMD: " + symbol, symbol);
        return instrument.getInstrumentType();
    }

    // consider inheriting your test from RiskRuleTestEx if you need to redefine this method
    protected long currentTimeMillis() {
        return SystemEpochClock.INSTANCE.time();
    }

    protected static ConstProjectionPath makeProjectionPath(ProjectionKey key, CharSequence value) {
        return new ConstProjectionPath(new Projection(key), new String[]{CharSequenceUtil.toString(value)});
    }
}
