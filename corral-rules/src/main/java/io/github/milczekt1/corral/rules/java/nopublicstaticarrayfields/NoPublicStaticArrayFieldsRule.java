package io.github.milczekt1.corral.rules.java.nopublicstaticarrayfields;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No {@code public static} field of a production class may be declared with an array type,
 * {@code final} or not. Interface fields count, being implicitly {@code public static final}.
 *
 * <p>Reads the field's modifiers and declared raw type only: the declaring class's own visibility is
 * not read, and a zero-length array constant is flagged although nothing can be written into it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoPublicStaticArrayFieldsRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.java.no-public-static-array-fields")
            .why("""
                    An array is mutable whatever its modifiers: final fixes the reference, not the \
                    contents. Defaults.NAMES[0] = null compiles from every class that can see the \
                    field, and one caller that sorts or fills the "constant" changes it for everyone, \
                    with no write to the field itself for a search to find. A public static array is \
                    therefore shared mutable state that reads as a constant.""")
            .howToFix("""
                    Declare public static final List<String> NAMES = List.of(...), which is immutable \
                    and needs no copy on read. If callers need an array, make the field private and \
                    return NAMES.clone() from a method. A zero-length array constant is flagged too, \
                    though nothing can be written into it, because only the declared type is read: \
                    make it private and expose it from a method, or freeze it. Generated code you \
                    cannot edit, such as an ANTLR lexer's or parser's ruleNames and tokenNames, is not \
                    fixed by hand: freeze what exists as debt, since its lines name the field, not a \
                    line number, and survive regeneration.""")
            .howNotToFix("""
                    Do NOT replace the array with new ArrayList<>(...) or Arrays.asList(...): this rule \
                    does not flag either, and any class can still call set. Do NOT make the field \
                    private, protected or package-private and return the array itself from a getter: \
                    this rule no longer sees it, and every caller still holds the shared array. Do NOT \
                    wrap the array in a final holder object that exposes it: this rule does not flag \
                    the holder, and the contents are as writable as before. Do NOT move the array into \
                    an interface: interface fields are public static final, and the array is flagged \
                    there too.""")
            .build();

    static final ArchRule DEFINITION = noFields()
            .that().areDeclaredInClassesThat(TestScope.PRODUCTION_CLASSES)
            .and().arePublic()
            .and().areStatic()
            .should().haveRawType(describe("an array", JavaClass::isArray));

    @ArchTest
    public static final ArchRule rule = new NoPublicStaticArrayFieldsRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
