package com.agrolink.moderation;

import com.agrolink.moderation.rules.MaxLengthRule;
import com.agrolink.moderation.rules.ProfanitySanitizerRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory Method: encapsulates creation of ModerationRule products.
 */
public class ModerationRuleFactory {

    public List<ModerationRule> createDefaultRules() {
        List<ModerationRule> rules = new ArrayList<>();
        rules.add(createRule(RuleType.PROFANITY_SANITIZE));
        rules.add(new MaxLengthRule(500));
        return rules;
    }

    protected ModerationRule createRule(RuleType type) {
        return switch (type) {
            case PROFANITY_SANITIZE -> new ProfanitySanitizerRule();
            case MAX_LENGTH -> new MaxLengthRule(500);
        };
    }
}
