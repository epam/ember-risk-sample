package deltix.ember.service.oms.util;


import com.epam.deltix.dfp.Decimal;
import deltix.ember.service.price.api.CurrencyRate;
import deltix.ember.service.price.api.FixedPriceInfo;
import deltix.ember.service.price.api.PriceInfo;
import deltix.ember.service.price.api.PricingService;
import deltix.util.collections.CharSequenceToObjectMapQuick;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Consumer;

public class FixedPricingService implements PricingService {

    private final CharSequenceToObjectMapQuick<FixedPriceInfo> priceMap = new CharSequenceToObjectMapQuick<>();
    private final @Decimal
    long defaultPrice;

    public FixedPricingService(@Decimal long defaultPrice) {
        this.defaultPrice = defaultPrice;
    }

    public FixedPricingService(Map<String, Long> knownPrices, @Decimal long defaultPrice) {
        for (Map.Entry<String, Long> knownPrice : knownPrices.entrySet()) {
            String symbol = knownPrice.getKey();
            Long price = knownPrice.getValue();
            priceMap.put(symbol, new FixedPriceInfo(symbol, price));
        }
        this.defaultPrice = defaultPrice;
    }

    @Override
    public void open() {
    }

    @Override
    public void close() {
    }

    @Override
    public PriceInfo getInstrumentPrices(CharSequence symbol) {
        synchronized (priceMap) {
            FixedPriceInfo price = priceMap.get(symbol, null);
            if (price == null) {
                price = new FixedPriceInfo(symbol, defaultPrice);
                assert price.getSymbol() instanceof String;
                priceMap.put(symbol, price);
            }

            return price;
        }
    }

    @Override
    public void forEach(Consumer<PriceInfo> visitor) {
        synchronized (priceMap) {
            priceMap.forEach(visitor);
        }
    }


    @Nullable
    @Override
    public CurrencyRate getCurrencyRate(long l) {
        throw new UnsupportedOperationException();
    }

    public void addPrice (String symbol, @Decimal long price) {
        priceMap.put(symbol, new FixedPriceInfo(symbol, price));
    }
}

