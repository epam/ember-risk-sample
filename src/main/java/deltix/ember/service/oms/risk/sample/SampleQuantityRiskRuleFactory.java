package deltix.ember.service.oms.risk.sample;

import deltix.ember.service.oms.position.ProjectionPath;
import deltix.ember.service.oms.risk.api.CustomRiskRuleFactory;
import deltix.ember.service.oms.risk.api.RiskLimitDefinition;
import deltix.ember.service.oms.risk.api.RiskManagerContext;
import deltix.ember.service.oms.risk.api.RiskRule;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static deltix.ember.service.oms.risk.sample.SamplePnLRiskRule.MaxLoss;
import static deltix.ember.service.oms.risk.sample.SamplePositionRiskRule.MaxPosition;
import static deltix.ember.service.oms.risk.sample.SampleQuantityRiskRule.MaxQuantity;

/**
 * Sample implementation of RiskRulesFactory
 *
 * Ember Server uses this factory to construct instance of a risk rule for each configured projection.
 */
public class SampleQuantityRiskRuleFactory implements CustomRiskRuleFactory
{
    private static final List<String> limitNames = Collections.unmodifiableList(Arrays.asList(MaxQuantity, MaxPosition, MaxLoss));


    // Examples of parameters custom risk rule may take from config
    // Note: these parameters are not limits - actual limits are defined via Ember Monitor or RiskUpdateRequest API.
    //
    // Assuming parameters are passed via ember.conf like this:
    //     settings {
    //       marketClosingTime = "17:00:00"
    //       timeZone = "America/New_York"
    //     }
    //
    private String marketClosingTime;
    private String timeZone;

    public void setMarketClosingTime(String marketClosingTime) {
        this.marketClosingTime = marketClosingTime;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * @return Supported limit names
     */
    @Override
    public List<String> getLimitNames() {
        return limitNames;
    }

    /**
     * Creates a new instance of RiskRule that handles specified riks limit.
     * @param limitName Risk limit name
     * @param path Projection path
     * @param context Risk manager context
     * @throws IllegalArgumentException if the limit is not supported by this factory
     * @return RiskRule implementation that handles the limit specified by limitName parameter.
     */
    @Override
    public RiskRule create(String limitName, ProjectionPath path, RiskManagerContext context) {

        // Here we can use initialization parameters if we need to
        //Calendar timerCalendar = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
        //long closingTimeOfDay = OrderAttributesParser.parseDuration(marketClosingTime);

        if (MaxQuantity.equals(limitName)) {
            return new SampleQuantityRiskRule();
        } else if (MaxPosition.equals(limitName)) {
            return new SamplePositionRiskRule();
        } else if (MaxLoss.equals(limitName)) {
            return new SamplePnLRiskRule();
        }
        throw new IllegalArgumentException("Unknown risk limit: " + limitName);
    }

    /**
     * @param limitName Name of the limit handled by this factory
     * @return RiskLimitDefinition of the limit specified by limitName parameter
     * @throws IllegalArgumentException if the limit is not supported by this factory
     */
    @Nonnull
    @Override
    public RiskLimitDefinition getLimitDefinition(String limitName) {
        if (MaxQuantity.equals(limitName)) {
            return new RiskLimitDefinition(MaxQuantity, RiskLimitDefinition.ValueType.INT, SampleQuantityRiskRule.class, "Maximum Order Quantity", this, null, "Limits Order Quantity");
        } else if (MaxPosition.equals(limitName)) {
            return new RiskLimitDefinition(MaxPosition, RiskLimitDefinition.ValueType.DOUBLE, SampleQuantityRiskRule.class, "Maximum Symbol Position", this, null, "Limits LONG and SHORT position");
        } else if (MaxLoss.equals(limitName)) {
            return new RiskLimitDefinition(MaxLoss, RiskLimitDefinition.ValueType.DOUBLE, SamplePnLRiskRule.class, "Maximum Loss", this, null, "Limits realized loss");
        }
        throw new IllegalArgumentException("Unknown risk limit: " + limitName);
    }
}
