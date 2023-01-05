package deltix.ember.service.oms.util;


import deltix.anvil.util.clock.SystemEpochClock;
import deltix.ember.message.trade.*;
import deltix.ember.service.OrderEventHandler;
import deltix.ember.service.data.Order;
import deltix.ember.service.engine.TradingMessages;
import deltix.ember.service.oms.cache.OrdersCache;
import deltix.ember.service.op.FullOrderProcessor;
import deltix.ember.service.op.SecurityMetadataProvider;
import deltix.ember.service.valid.FullOrderRequestValidator;
import deltix.ember.service.valid.OrderRequestValidator;
import deltix.ember.service.valid.ValidationSettings;

import java.time.Duration;

public class TestOrderProcessor<T extends Order> extends FullOrderProcessor<T> {

    public TestOrderProcessor(OrdersCache<T> cache) {
        super(cache, new FullOrderRequestValidator(new ValidationSettings(true, true, true, Duration.ofSeconds(45), true, 10)), SystemEpochClock.INSTANCE, new TradingMessages(123));
    }

    public TestOrderProcessor(OrdersCache<T> cache, OrderRequestValidator validator) {
        super(cache, validator, SystemEpochClock.INSTANCE, new TradingMessages(123));
    }

    @Override
    protected void handleNewOrderRequest(OrderNewRequest request, T order) {

    }

    @Override
    protected void handleCancelOrderRequest(OrderCancelRequest request, T order) {

    }

    @Override
    protected void handleReplaceOrderRequest(OrderReplaceRequest request, T order) {

    }

    @Override
    protected void handleOrderStatusRequest(T order, OrderStatusRequest request) {

    }

    @Override
    protected OrderEventHandler getEventService(long destinationId) {
        return new NullOrderEventHandler();
    }
}

class NullOrderEventHandler implements OrderEventHandler {

    public static final NullOrderEventHandler INSTANCE = new NullOrderEventHandler();

    @Override
    public void onOrderPendingNewEvent(OrderPendingNewEvent event) {
    }

    @Override
    public void onOrderNewEvent(OrderNewEvent event) {
    }

    @Override
    public void onOrderRejectEvent(OrderRejectEvent event) {
    }

    @Override
    public void onOrderPendingCancelEvent(OrderPendingCancelEvent event) {
    }

    @Override
    public void onOrderCancelEvent(OrderCancelEvent event) {
    }

    @Override
    public void onOrderCancelRejectEvent(OrderCancelRejectEvent event) {
    }

    @Override
    public void onOrderPendingReplaceEvent(OrderPendingReplaceEvent event) {
    }

    @Override
    public void onOrderReplaceEvent(OrderReplaceEvent event) {
    }

    @Override
    public void onOrderReplaceRejectEvent(OrderReplaceRejectEvent event) {
    }

    @Override
    public void onOrderTradeReportEvent(OrderTradeReportEvent event) {
    }

    @Override
    public void onOrderTradeCancelEvent(OrderTradeCancelEvent event) {
    }

    @Override
    public void onOrderTradeCorrectEvent(OrderTradeCorrectEvent event) {
    }

    @Override
    public void onOrderStatusEvent(OrderStatusEvent event) {
    }

    @Override
    public void onOrderRestateEvent(OrderRestateEvent event) {
    }
}
