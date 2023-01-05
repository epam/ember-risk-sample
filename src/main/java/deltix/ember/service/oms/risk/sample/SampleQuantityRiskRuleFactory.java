package deltix.ember.service.oms.risk.sample;

import deltix.ember.service.oms.position.ProjectionPath;
import deltix.ember.service.oms.risk.api.*;

import javax.annotation.Nonnull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static deltix.ember.service.oms.risk.sample.SamplePnLRiskRule.MaxLoss;
import static deltix.ember.service.oms.risk.sample.SampleQuantityRiskRule.MaxQuantity;
import static deltix.ember.service.oms.risk.sample.SamplePositionRiskRule.MaxPosition;

/**
 * Sample implementation of RiskRulesFactory
 *
 * Ember Server uses this factory to construct instance of a risk rule for each configured projection.
 */
public class SampleQuantityRiskRuleFactory implements CustomRiskRuleFactory
{
    private static final List<String> limitNames = Collections.unmodifiableList(Arrays.asList(MaxQuantity, MaxPosition, MaxLoss));

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
