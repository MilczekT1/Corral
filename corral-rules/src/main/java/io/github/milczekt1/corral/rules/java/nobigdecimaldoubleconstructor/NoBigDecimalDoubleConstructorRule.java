package io.github.milczekt1.corral.rules.java.nobigdecimaldoubleconstructor;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitAccessTarget;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No class may call or reference a {@code BigDecimal} constructor whose first parameter is a
 * {@code double}: {@code BigDecimal(double)} and {@code BigDecimal(double, MathContext)}.
 *
 * <p>Production and test classes alike.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoBigDecimalDoubleConstructorRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.java.no-bigdecimal-double-constructor")
            .why("""
                    new BigDecimal(0.1) is 0.1000000000000000055511151231257827021181583404541015625, \
                    not 0.1. The constructor is exact: no double holds one tenth, so it reproduces every \
                    digit of the binary value the hardware stored, and brings back the floating-point \
                    error BigDecimal was chosen to escape. BigDecimal.valueOf(0.1) goes through \
                    Double.toString and gives 0.1. The damage survives testing. equals against \
                    new BigDecimal("0.1") is false, so the same amount built two ways never matches and \
                    is lost as a HashMap key. compareTo returns 1, so amount.compareTo(limit) <= 0 \
                    rejects an amount sitting exactly on the limit. The scale is 55, and every multiply \
                    adds the scales. A NUMERIC(19,2) column rounds or rejects the value, and a JSON \
                    payload carries 55 digits where the contract says two. In money code a price built \
                    this way, multiplied and summed across an order, drifts from the same sum computed \
                    anywhere else by a fraction of a cent nobody can explain.""")
            .howToFix("""
                    If the value is a literal or arrives as text, never let it become a double: write \
                    new BigDecimal("0.1"), and change the field, the DTO and the column type together so \
                    no double is left to convert. If it arrives as a double you cannot change — a JSON \
                    parser, a legacy DTO — use BigDecimal.valueOf(d), as the constructor's own Javadoc \
                    advises, and replace BigDecimal::new with BigDecimal::valueOf. If it is a float, use \
                    new BigDecimal(Float.toString(f)), not BigDecimal.valueOf(f): that widens the float \
                    to a double first and gives 0.10000000149011612, and this rule cannot see the \
                    widening, so it does not flag it. If you passed a MathContext, use \
                    BigDecimal.valueOf(d).round(mc). For money, round once, at a named place, with an \
                    explicit RoundingMode, not after every operation.""")
            .howNotToFix("""
                    Do NOT wrap the constructor in a helper such as MoneyUtils.of(double): this rule \
                    flags the helper, and the 55 digits are unchanged. Do NOT add \
                    .setScale(2, RoundingMode.HALF_UP) after the constructor at the one site a test \
                    caught: the call is still flagged, and rounding at the end of a chain of \
                    multiplications is not the same number as starting from the right value. Do NOT add \
                    a MathContext argument to silence it: new BigDecimal(d, MathContext.UNLIMITED) is \
                    the same 55 digits, and that constructor is flagged too. Do NOT move the conversion \
                    behind reflection or into a library that calls the constructor for you. Not caught: \
                    this rule sees only constructor calls and references in your own classes, and the \
                    error is still there.""")
            .build();

    /** Do not switch to {@code callConstructor}: it misses a {@code BigDecimal::new} reference. */
    private static final DescribedPredicate<JavaAccess<?>> BIGDECIMAL_FROM_DOUBLE = describe(
            "a BigDecimal constructor taking a double",
            access -> access.getTarget() instanceof CodeUnitAccessTarget target
                    && target.getName().equals(JavaConstructor.CONSTRUCTOR_NAME)
                    && target.getOwner().isEquivalentTo(BigDecimal.class)
                    && target.getRawParameterTypes().stream().findFirst()
                            .filter(first -> first.isEquivalentTo(double.class))
                            .isPresent());

    static final ArchRule DEFINITION = noClasses().should().accessTargetWhere(BIGDECIMAL_FROM_DOUBLE);

    @ArchTest
    public static final ArchRule rule = new NoBigDecimalDoubleConstructorRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
