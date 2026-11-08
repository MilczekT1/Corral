package io.github.milczekt1.corral.scope;

import static com.tngtech.archunit.base.DescribedPredicate.describe;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.Source;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import io.github.milczekt1.corral.settings.CorralSettings;
import io.github.milczekt1.corral.warn.FirstEvaluationWarning;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

/**
 * Separates the code a consumer maintains from the code a tool generated for them. A class is
 * hand-written when the source file it was compiled from lies in its module's own tree; generated when
 * that file lies only under {@code target/}, {@code build/}, {@code out/} or a directory listed in
 * {@value CorralSettings#GENERATED_SOURCES}, or when it records no source file at all.
 *
 * <p>A class whose module cannot be located — one from a jar, or from an output layout {@link OutputLayout}
 * does not recognise — counts as hand-written, and so does every class of a module with no source file
 * in its tree. Misjudging a class therefore only ever checks more, except for hand-written sources kept
 * outside the module's directory, which count as generated.
 */
@UtilityClass
public class SourceScope {

    private static final Set<String> BUILD_DIRECTORIES = Set.of("target", "build", "out");

    private static final Set<String> BUILD_FILES = Set.of("pom.xml", "build.gradle", "build.gradle.kts");

    private static final Map<ModuleKey, ModuleSources> MODULES = new ConcurrentHashMap<>();

    private static final AtomicBoolean SKIPPED_PRINTED_THIS_RUN = new AtomicBoolean(false);

    /**
     * Classes compiled from a source file the consumer maintains. Rewording the description changes
     * no freeze-store key: it never reaches a violation line.
     */
    public static final DescribedPredicate<JavaClass> HAND_WRITTEN = describe("hand-written classes",
            javaClass -> isHandWritten(javaClass, CorralSettings.current().generatedSources()));

    /**
     * {@code rule} evaluated on the {@link #HAND_WRITTEN} classes it is given; the first evaluation this
     * run that skips any prints how many. Wrap inside {@code FreezingArchRule}, so a generated class
     * never reaches the store.
     */
    public static ArchRule handWrittenOnly(ArchRule rule) {
        return handWrittenOnly(rule, CorralSettings.current().generatedSources(), SKIPPED_PRINTED_THIS_RUN,
                FirstEvaluationWarning::printWarning);
    }

    static ArchRule handWrittenOnly(
            ArchRule rule, List<String> generatedSources, AtomicBoolean printedThisRun, Consumer<String> sink) {
        return new HandWrittenOnly(rule, List.copyOf(generatedSources), printedThisRun, sink);
    }

    static boolean isHandWritten(JavaClass javaClass, List<String> generatedSources) {
        Optional<Source> source = javaClass.getSource();
        if (source.isEmpty() || !"file".equals(source.get().getUri().getScheme())) {
            return true;
        }
        Optional<Path> moduleRoot = outputRootOf(Path.of(source.get().getUri()), javaClass)
                .flatMap(OutputLayout::moduleRootOf);
        if (moduleRoot.isEmpty()) {
            return true;
        }
        ModuleSources sources = MODULES.computeIfAbsent(new ModuleKey(moduleRoot.get(), List.copyOf(generatedSources)),
                ModuleSources::index);
        if (sources.maintained().isEmpty()) {
            return true;
        }
        Optional<String> fileName = source.get().getFileName();
        if (fileName.isEmpty()) {
            return false;
        }
        List<Path> maintained = sources.maintained().getOrDefault(fileName.get(), List.of());
        Path packageDirectory = packageDirectoryOf(javaClass);
        if (maintained.stream().anyMatch(file -> isIn(file, packageDirectory))) {
            return true;
        }
        if (sources.generated().getOrDefault(fileName.get(), List.of()).stream()
                .anyMatch(file -> isIn(file, packageDirectory))) {
            return false;
        }
        return !maintained.isEmpty();
    }

    private static Optional<Path> outputRootOf(Path classFile, JavaClass javaClass) {
        Path root = classFile.getParent();
        for (int segment = 0; segment < packageDirectoryOf(javaClass).getNameCount() && root != null; segment++) {
            root = root.getParent();
        }
        return Optional.ofNullable(root);
    }

    private static boolean isIn(Path file, Path packageDirectory) {
        return packageDirectory.toString().isEmpty() || file.getParent().endsWith(packageDirectory);
    }

    /** The empty path for the default package. */
    private static Path packageDirectoryOf(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        return packageName.isEmpty() ? Path.of("") : Path.of("", packageName.split("\\."));
    }

    private record ModuleKey(Path root, List<String> generatedSources) {
    }

    @RequiredArgsConstructor
    private static final class HandWrittenOnly implements ArchRule {

        private final ArchRule delegate;
        private final List<String> generatedSources;
        private final AtomicBoolean printedThisRun;
        private final Consumer<String> sink;

        @Override
        public void check(JavaClasses classes) {
            delegate.check(handWritten(classes));
        }

        @Override
        public EvaluationResult evaluate(JavaClasses classes) {
            return delegate.evaluate(handWritten(classes));
        }

        private JavaClasses handWritten(JavaClasses classes) {
            JavaClasses handWritten = classes.that(describe("hand-written classes",
                    javaClass -> isHandWritten(javaClass, generatedSources)));
            int skipped = classes.size() - handWritten.size();
            if (skipped > 0 && printedThisRun.compareAndSet(false, true)) {
                sink.accept("checking hand-written code only; skipped " + skipped + " generated class"
                        + (skipped == 1 ? "" : "es") + " in this scan: compiled from sources under target/,"
                        + " build/, out/ or " + CorralSettings.GENERATED_SOURCES + ", or recording no source"
                        + " file.");
            }
            return handWritten;
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }

        @Override
        public ArchRule as(String newDescription) {
            return new HandWrittenOnly(delegate.as(newDescription), generatedSources, printedThisRun, sink);
        }

        @Override
        public ArchRule because(String reason) {
            return new HandWrittenOnly(delegate.because(reason), generatedSources, printedThisRun, sink);
        }

        @Override
        public ArchRule allowEmptyShould(boolean allowEmptyShould) {
            return new HandWrittenOnly(delegate.allowEmptyShould(allowEmptyShould), generatedSources,
                    printedThisRun, sink);
        }
    }

    /** Source files by file name; class files are not indexed. */
    private record ModuleSources(Map<String, List<Path>> maintained, Map<String, List<Path>> generated) {

        static ModuleSources index(ModuleKey key) {
            Map<String, List<Path>> maintained = new ConcurrentHashMap<>();
            Map<String, List<Path>> generated = new ConcurrentHashMap<>();
            Set<Path> generatedRoots = new HashSet<>();
            BUILD_DIRECTORIES.forEach(name -> generatedRoots.add(key.root().resolve(name)));
            key.generatedSources().forEach(directory -> generatedRoots.add(key.root().resolve(directory)));
            walk(key.root(), generatedRoots, maintained, generated);
            return new ModuleSources(maintained, generated);
        }

        private static void walk(Path root, Set<Path> generatedRoots,
                Map<String, List<Path>> maintained, Map<String, List<Path>> generated) {
            try {
                Files.walkFileTree(root, new SimpleFileVisitor<>() {

                    private Map<String, List<Path>> into = maintained;
                    private Path generatedRoot;

                    @Override
                    public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                        if (directory.equals(root)) {
                            return FileVisitResult.CONTINUE;
                        }
                        if (directory.getFileName().toString().startsWith(".") || isNestedModule(directory)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        if (generatedRoot == null && generatedRoots.contains(directory)) {
                            generatedRoot = directory;
                            into = generated;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                        String name = file.getFileName().toString();
                        if (!name.endsWith(".class")) {
                            into.computeIfAbsent(name, ignored -> new ArrayList<>()).add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path directory, IOException e) {
                        if (directory.equals(generatedRoot)) {
                            generatedRoot = null;
                            into = maintained;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static boolean isNestedModule(Path directory) {
            return BUILD_FILES.stream().anyMatch(name -> Files.isRegularFile(directory.resolve(name)));
        }
    }
}
