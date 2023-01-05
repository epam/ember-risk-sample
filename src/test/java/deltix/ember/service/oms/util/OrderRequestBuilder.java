package deltix.ember.service.oms.util;

import deltix.anvil.util.CharSequenceUtil;
import deltix.anvil.util.annotation.Timestamp;
import deltix.anvil.util.clock.EpochClock;
import deltix.anvil.util.codec.AlphanumericCodec;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import deltix.ember.message.smd.InstrumentType;
import deltix.ember.message.trade.*;
import deltix.util.collections.generated.ObjectArrayList;
import deltix.util.collections.generated.ObjectList;

import java.util.function.Consumer;

@SuppressWarnings({"unused", "WeakerAccess"})
public class OrderRequestBuilder {
    public static final String DEFAULT_SOURCE = "SOURCE";
    public static final String DEFAULT_DESTINATION = "DESTINY";
    public static final String DEFAULT_SYMBOL = "BTCUSD";
    public static final String DEFAULT_EXCHANGE = "CME";
    public static final String DEFAULT_ACCOUNT = "Gold";
    public static final String DEFAULT_TRADER = "jdoe";


    public static final int TEST_ONLYDMA_ORDER_TAG = 8765;
    protected final Consumer<OrderRequest> target;
    protected final MutableOrderEntryRequest result;

    public OrderRequestBuilder(MutableOrderEntryRequest result, CharSequence orderId, EpochClock clock, Consumer<OrderRequest> target) {
        this.result = result;
        this.target = target;
        this.result.setOrderType(OrderType.MARKET);
        this.result.setOrderId(CharSequenceUtil.toString(orderId));
        this.result.setTimeInForce(TimeInForce.DAY);
        this.result.setSourceId(AlphanumericCodec.encode(DEFAULT_SOURCE));
        this.result.setDestinationId(AlphanumericCodec.encode(DEFAULT_DESTINATION));
        this.result.setSymbol(DEFAULT_SYMBOL);
        this.result.setAccount(DEFAULT_ACCOUNT);
        this.result.setTraderId(DEFAULT_TRADER);
        this.result.setExchangeId(AlphanumericCodec.encode(DEFAULT_EXCHANGE));
        this.result.setSide(Side.BUY);
        this.result.setQuantity(Decimal64Utils.ONE);

        this.result.setTimestamp(clock.time());
    }

    public OrderRequestBuilder timeInForce(TimeInForce timeInForce) {
        result.setTimeInForce(timeInForce);
        return this;
    }

    public OrderRequestBuilder expireTime(@Timestamp long timestamp) {
        result.setExpireTime(timestamp);
        return this;
    }

    public OrderRequestBuilder side(Side side) {
        result.setSide(side);
        return this;
    }

    public OrderRequestBuilder symbol(String symbol) {
        result.setSymbol(symbol);
        return this;
    }

    public OrderRequestBuilder instrumentType(InstrumentType instrumentType) {
        result.setInstrumentType(instrumentType);
        return this;
    }

    public OrderRequestBuilder limitPrice(final @Decimal long limitPrice) {
        result.setOrderType(OrderType.LIMIT);
        result.setLimitPrice(limitPrice);
        return this;
    }

    public OrderRequestBuilder pegDifference(final @Decimal long pegDifference) {
        assert (result.getOrderType() == OrderType.PEG_TO_MARKET || result.getOrderType() == OrderType.PEG_TO_MIDPOINT || result.getOrderType() == OrderType.PEG_TO_PRIMARY);
        result.setLimitPrice(pegDifference);
        return this;
    }

    public OrderRequestBuilder orderType(OrderType orderType) {
        result.setOrderType(orderType);
        switch (orderType) {
            case MARKET:
                result.setLimitPrice(Decimal64Utils.NULL);
                result.setStopPrice(Decimal64Utils.NULL);
                result.setPegDifference(Decimal64Utils.NULL);
                break;
            case LIMIT:
                result.setStopPrice(Decimal64Utils.NULL);
                result.setPegDifference(Decimal64Utils.NULL);
                break;
            case STOP:
                result.setLimitPrice(Decimal64Utils.NULL);
                result.setPegDifference(Decimal64Utils.NULL);
                break;
            case PEG_TO_MARKET:
            case PEG_TO_MIDPOINT:
            case PEG_TO_PRIMARY:
                result.setStopPrice(Decimal64Utils.NULL);
                break;
        }
        return this;
    }

    public OrderRequestBuilder limitPrice(String limitPrice) {
        return limitPrice(Decimal64Utils.parse(limitPrice));
    }

    public OrderRequestBuilder stopPrice(String stopPrice) {
        result.setOrderType(OrderType.STOP);
        result.setStopPrice(Decimal64Utils.parse(stopPrice));
        return this;
    }

    public OrderRequestBuilder quantity(final @Decimal long quantity) {
        result.setQuantity(Decimal64Utils.abs(quantity));
        result.setSide(Decimal64Utils.isPositive(quantity) ? Side.BUY : Side.SELL);
        return this;
    }

    public OrderRequestBuilder quantity(String quantity) {
        return quantity(Decimal64Utils.parse(quantity));
    }

    public OrderRequestBuilder displayQuantity(final @Decimal long displayQuantity) {
        result.setDisplayQuantity(displayQuantity);
        return this;
    }

    public OrderRequestBuilder displayQty(String displayQuantity) {
        return displayQuantity(Decimal64Utils.parse(displayQuantity));
    }

    public OrderRequestBuilder originalId(String originalId) {
        ((MutableOrderReplaceRequest) result).setOriginalOrderId(originalId);
        return this;
    }

    // Only for advanced cases otherwise ID is auto-assigned
    public OrderRequestBuilder orderId(String orderId) {
        result.setOrderId(orderId);
        return this;
    }

    public OrderRequestBuilder sourceId(long sourceId) {
        result.setSourceId(sourceId);
        return this;
    }

    public OrderRequestBuilder sourceId(CharSequence source) {
        result.setSourceId(AlphanumericCodec.encode(source));
        return this;
    }

    public OrderRequestBuilder destinationId(long sourceId) {
        result.setDestinationId(sourceId);
        return this;
    }

    public OrderRequestBuilder destinationId(CharSequence source) {
        result.setDestinationId(AlphanumericCodec.encode(source));
        return this;
    }

    public OrderRequestBuilder exchange(String exchange) {
        return exchange(AlphanumericCodec.encode(exchange));
    }

    public OrderRequestBuilder exchange(long exchangeId) {
        result.setExchangeId(exchangeId);
        return this;
    }

    public OrderRequestBuilder account(String account) {
        result.setAccount(account);
        return this;
    }

    public OrderRequestBuilder userData(String userData) {
        ((MutableOrderNewRequest) result).setUserData(userData);
        return this;
    }

    public OrderRequestBuilder currency (String currency) {
        result.setCurrency(AlphanumericCodec.encode(currency));
        return this;
    }

    public OrderRequestBuilder externalOrderId(CharSequence externalOrderId) {
        ((MutableOrderReplaceRequest) result).setExternalOrderId(externalOrderId);
        return this;
    }

    public OrderRequestBuilder dma(boolean isDMA) {
        attribute(TEST_ONLYDMA_ORDER_TAG, "Y");
        return this;
    }

    public static boolean isDMA (OrderNewRequest request) {
        if (request.hasAttributes()) {
            ObjectList<CustomAttribute> attributes = request.getAttributes();
            for (int i=0; i < attributes.size(); i++) {
                CustomAttribute attribute = attributes.get(i);
                if (attribute.getKey() == TEST_ONLYDMA_ORDER_TAG && CharSequenceUtil.equals(attribute.getValue(), "Y"))
                    return true;
            }
        }
        return false;
    }


    protected OrderRequestBuilder attribute(int tag, CharSequence value) {
        if (!result.hasAttributes())
            result.setAttributes(new ObjectArrayList<>());

        MutableCustomAttribute attribute = new MutableCustomAttribute();
        attribute.setKey(tag);
        attribute.setValue(value);
        ((ObjectArrayList<CustomAttribute>) result.getAttributes()).add(attribute);
        return this;
    }

    public MutableOrderEntryRequest build() {
        return result;
    }

    public void send() {
        target.accept(result);
        result.reuse();
    }

    /** We do clear after send() anyway, but here is explicit method */
    public OrderRequestBuilder clear () {
        result.reuse();
        return this;
    }

    @Override
    public String toString() {
        return result.toString();
    }
}
