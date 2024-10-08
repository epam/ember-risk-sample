package deltix.ember.service.oms.risk.sample;

import deltix.ember.message.risk.ProjectionKey;
import deltix.ember.message.smd.InstrumentType;
import deltix.ember.message.trade.Side;
import deltix.ember.service.InstrumentUpdateHandler;
import deltix.ember.service.oms.risk.api.PositionView;
import deltix.ember.service.oms.risk.api.RiskRule;
import deltix.ember.service.oms.risk.limits.RiskRuleTestEx;
import org.junit.Before;
import org.junit.Test;

import java.util.function.BiConsumer;

public class Test_SampleQuantityRiskRule extends RiskRuleTestEx<SampleQuantityRiskRule> {

    @Before
    public void init() {
        addInstrument("MSFT", InstrumentType.EQUITY, 25);
        haltTradingRiskObserver.assertTradingNotHalted();
    }

    @Test
    public void smallOrderTest () {
        rule = newRiskRule(15);
        assertValid(newOrder(Side.BUY, 10, "MSFT"));
    }

    @Test
    public void largeOrderTest () {
        rule = newRiskRule(15);
        assertInvalid(newOrder(Side.BUY, 20, "MSFT"), "Order quantity 20 exceeds maximum 15");
    }


    // helpers


    @Override
    public void addInstrumentUpdateListener(InstrumentUpdateHandler instrumentUpdateHandler) {
        throw new UnsupportedOperationException();
    }

    private SampleQuantityRiskRule newRiskRule(int maxQuantity) {
        rule = new SampleQuantityRiskRule();
        rule.setMaxQuantity(maxQuantity);
        return rule;
    }
}
