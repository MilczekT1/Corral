package io.github.milczekt1.corral.rules.java.nooptionalmethodparameters;

import static com.tngtech.archunit.base.DescribedPredicate.anyElementThat;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No method of a production class may declare a parameter typed {@code Optional}, {@code OptionalInt},
 * {@code OptionalLong} or {@code OptionalDouble}.
 *
 * <p>Stands alone, in no group: wire it with {@code ArchTests.in(NoOptionalMethodParametersRule.class)}.
 * Only the raw parameter type is checked, so {@code Supplier<Optional<T>>} and {@code Optional<T>...}
 * are not flagged. Constructors and compiler-generated bridge methods are not checked. One violation
 * per method.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoOptionalMethodParametersRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.java.no-optional-method-parameters")
            .why("""
                    The JDK's own API note says Optional "is primarily intended for use as a method \
                    return type where there is a clear need to represent 'no result'". As a parameter \
                    it makes the argument three-valued: null, Optional.empty() and present. Every call \
                    site has to wrap its value, usually with Optional.ofNullable, and nothing stops a \
                    caller passing null, so the method either null-checks the Optional itself or throws \
                    NullPointerException on the first isPresent() — strictly more work than the \
                    nullable parameter it replaced. An overload, or a parameter annotated @Nullable, \
                    says the same thing with one fewer state. Constructors are exempt: a DI container \
                    or deserializer picks one constructor and supplies Optional.empty() for an absent \
                    input, so an overload cannot replace it.""")
            .howToFix("""
                    Add an overload without the parameter: that is how the JDK expresses "not \
                    supplied", it reads better at the call site, and it removes the third state. \
                    Several optional inputs belong in a parameter object or builder whose fields are \
                    plain types, not Optional. Where a nullable parameter really is right, take the \
                    plain type and annotate it @Nullable (JSpecify's org.jspecify.annotations.Nullable) \
                    so a nullness checker can verify the call sites. For a Spring injection point on a \
                    method — a @Bean method or an @Autowired setter — take ObjectProvider<T>, or the \
                    plain type annotated @Nullable; before Spring Framework 7 a parameter annotation \
                    named Nullable — org.springframework.lang.Nullable, jakarta.annotation.Nullable or \
                    JSR-305's — makes it not required, but JSpecify's @Nullable, a TYPE_USE annotation, \
                    does not; from Spring 7 JSpecify's counts too. For a Jackson @JsonCreator static \
                    factory, take the plain type annotated @Nullable: Jackson passes null for an absent \
                    creator property. For a Spring MVC handler argument, use @RequestParam(required = \
                    false) or @RequestHeader(required = false) on the plain type — the boxed type \
                    (Integer, not int) in place of OptionalInt. A method whose signature is fixed by a \
                    generic interface it implements, such as Comparator<Optional<T>>, cannot drop the \
                    Optional on its own: an existing one stays recorded as frozen debt; for a new one, \
                    work with the unwrapped values instead — sort a List<T> with \
                    Comparator.nullsFirst(...) rather than a List<Optional<T>>. For a Lombok @Setter, \
                    @With or @Builder on an Optional field, the fix is the field's type; for an \
                    Immutables value type, declare the attribute @Nullable T — its builder method and \
                    its with-method then take the plain type. Constructors, return types, fields, local variables, \
                    test classes, and arrays or varargs of Optional are outside this rule.""")
            .howNotToFix("""
                    Do NOT change the parameter to Object or an unbounded type variable and cast \
                    inside: only the erased parameter type is checked, so this is not caught, and the \
                    caller loses the type entirely. Do NOT move the Optional into a type argument \
                    (Supplier<Optional<T>>, List<Optional<T>>) or an array or varargs \
                    (Optional<T>...): only the raw parameter type is checked, so this is not caught, \
                    and each element is still three-valued. Do NOT move the Optional into a parameter \
                    object or record component (find(Criteria c) with record \
                    Criteria(Optional<String> customerId)): constructors and fields are not checked, \
                    so this is not caught, and the value is still three-valued. Do NOT turn the method \
                    into a public constructor, such as replacing a static factory of(Optional<T>): \
                    constructors are not checked, so this is not caught. Do NOT switch to your own \
                    Maybe or Option type, Guava's Optional or Vavr's Option: only the four JDK types \
                    are matched, so this is not caught, and the parameter is still three-valued. Do \
                    NOT try to move the class out of scope by naming it like a test: scope is decided \
                    by output location and declared test methods. Do NOT add a JUnit test method to a \
                    production class: a class declaring one counts as a test class, so this is not \
                    caught.""")
            .build();

    static final ArchRule DEFINITION = noMethods()
            .that().areDeclaredInClassesThat(TestScope.PRODUCTION_CLASSES)
            .and().doNotHaveModifier(JavaModifier.SYNTHETIC)
            .should().haveRawParameterTypes(anyElementThat(type(Optional.class)
                    .or(type(OptionalInt.class))
                    .or(type(OptionalLong.class))
                    .or(type(OptionalDouble.class)))
                    .as("containing Optional, OptionalInt, OptionalLong or OptionalDouble"));

    @ArchTest
    public static final ArchRule rule = new NoOptionalMethodParametersRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
