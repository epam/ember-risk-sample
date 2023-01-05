## Sample Risk Rule

This project illustrates how to build, test, debug, and deploy custom Risk Rule for Deltix Execution Server.
This archive contains IntelliJ/IDEA project and Gradle build files for MaxQuantity sample risk rule.

### Build
This project references Ember Java API libraries located in Deltix private maven repository.

Please make sure that you define environment variables `NEXUS_USER` and `NEXUS_PASS` to Deltix repository credentials provided to you.

Now you can build this sample using Gradle or IntelliJ/IDEA.

### Test
This project includes small harness to test your custom risk rules. 

* Test_SampleQuantityRiskRule gives you an ability to mock various input scenarios (order cancellaions, replacements, fills, etc.) and verify risk rule behavior.

### Debug

One simple way to debug your risk rule is running entire Execution Server under debugger. 
Take a look at `Ember Server` Run configuration. It uses `deltix.ember.app.EmberApp` as a main class and `ember.home` 
system property that point to ember configuration home. Sample ember.conf is included in /home folder.
You can just setup breakpoints in your risk rule and launch debugger.

See ES Quick Start document for more information.

```
risk {
  riskLimits: {
    MaxQuantity: deltix.ember.service.oms.risk.sample.SampleQuantityRiskRuleFactory
  }
  riskTables: {
    Symbol: [MaxQuantity] 
  }
}
````

### Deploy

Entire Gradle project uses java-library plugin.

```
gradlew build 
```

The build produces `build/libs/risk-sample-*.jar`. To deploy your risk rule to actual server copy this JAR file under `lib/custom/` directory of your Ember installation.
The last step is to define your risk rule in server's `ember.conf` (just like we did in Debug section).  

You can also run OrderSubmitSample to see how you can send trading requests to your risk rule.


See Risk Rule Developer's Guide for more information.  