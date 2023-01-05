package deltix.ember.service.oms.tools;

import deltix.anvil.util.codec.AlphanumericCodec;
import com.epam.deltix.dfp.Decimal64Utils;
import deltix.ember.sample.SampleSupportTools;
import deltix.ember.message.trade.*;


/**
 * Sample that illustrates how to send new trade order and listen for trading events
 */
public class OrderSubmitSample extends SampleSupportTools {
    public static void main(String[] args) throws InterruptedException {
        sendRequest(
                (publication) -> {
                    OrderNewRequest request = createNewOrderRequest(Side.BUY, 1000, "BTCUSD", 7500);
                    publication.onNewOrderRequest(request);
                    System.out.println("New order request was sent " + request.getSourceId() + ':' + request.getOrderId());
                }
        );
    }

    private static MutableOrderNewRequest createNewOrderRequest(Side side, int size, String symbol, double price) {
        MutableOrderNewRequest request = new MutableOrderNewRequest();
        request.setOrderId(Long.toString(System.currentTimeMillis() % 100000000000L));
        request.setSide(side);
        request.setQuantity(Decimal64Utils.fromLong(size));
        request.setSymbol(symbol);
        request.setLimitPrice(Decimal64Utils.fromDouble(price));
        request.setTimeInForce(request.hasLimitPrice() ? TimeInForce.DAY : TimeInForce.IMMEDIATE_OR_CANCEL);
        request.setOrderType(request.hasLimitPrice() ? OrderType.LIMIT : OrderType.MARKET);
        request.setDestinationId(AlphanumericCodec.encode("SIM"));
        request.setExchangeId(AlphanumericCodec.encode("HOTSPOT"));
        request.setSourceId(CLIENT_SOURCE_ID); // Identify order source
        request.setTraderId("jdoe");
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }
}

