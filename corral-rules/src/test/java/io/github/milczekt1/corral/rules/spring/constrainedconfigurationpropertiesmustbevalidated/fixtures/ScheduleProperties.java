package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.hibernate.validator.constraints.ScriptAssert;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** FLAGGED: a constraint on the class itself. */
@ScriptAssert(lang = "javascript", script = "_this.start < _this.end")
@ConfigurationProperties("schedule")
public class ScheduleProperties {
    private int start;
    private int end;
}
