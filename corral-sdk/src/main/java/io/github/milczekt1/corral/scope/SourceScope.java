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
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
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
 * generated when the source file it was compiled from lies only under its module's {@code target/},
 * {@code build/} or {@code out/}, or in a directory listed in {@value CorralSettings#GENERATED_SOURCES};
 * every other class is hand-written, including one whose source file is found nowhere.
 *
 * <p>A class compiled without the {@code SourceFile} attribute is looked up by its top-level name, and
 * counts as generated when no such file exists either. That is the one way a hand-written class drops
 * out of scope: compiled without debug info, from sources outside the module or sharing another class's
 * file. Classes from a jar, or from an output layout {@link OutputLayout} does not recognise, are
 * hand-written.
 */
@UtilityClass
public class SourceScope {

    private static final Set<String> BUILD_DIRECTORIES = Set.of("target", "build", "out");

    private static final List<String> SOURCE_EXTENSIONS = List.of(".java", ".kt", ".groovy", ".scala");

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
        Optional<String> recorded = source.get().getFileName();
        List<String> fileNames = recorded.map(List::of).orElseGet(() -> sourceNamesOf(javaClass));
        List<Path> maintained = filesNamed(sources.maintained(), fileNames);
        List<Path> generated = filesNamed(sources.generated(), fileNames);
        Path packageDirectory = packageDirectoryOf(javaClass);
        if (maintained.stream().anyMatch(file -> isIn(file, packageDirectory))) {
            return true;
        }
        if (generated.stream().anyMatch(file -> isIn(file, packageDirectory))) {
            return false;
        }
        if (!maintained.isEmpty()) {
            return true;
        }
        if (!generated.isEmpty()) {
            return false;
        }
        return recorded.isPresent();
    }

    /** For a class compiled without the {@code SourceFile} attribute: the files its top-level name implies. */
    private static List<String> sourceNamesOf(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        String binaryName = javaClass.getName().substring(packageName.isEmpty() ? 0 : packageName.length() + 1);
        String topLevel = binaryName.split("\\$", 2)[0];
        List<String> names = new ArrayList<>();
        SOURCE_EXTENSIONS.forEach(extension -> names.add(topLevel + extension));
        if (topLevel.endsWith("Kt")) {
            names.add(topLevel.substring(0, topLevel.length() - 2) + ".kt");
        }
        return names;
    }

    private static List<Path> filesNamed(Map<String, List<Path>> index, List<String> fileNames) {
        return fileNames.stream().flatMap(name -> index.getOrDefault(name, List.of()).stream()).toList();
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
                        + " build/, out/ or " + CorralSettings.GENERATED_SOURCES + ", or recording no source file"
                        + " that could be found.");
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

    /** Every file of the module but class files, by file name. */
    private record ModuleSources(Map<String, List<Path>> maintained, Map<String, List<Path>> generated) {

        static ModuleSources index(ModuleKey key) {
            Map<String, List<Path>> maintained = new ConcurrentHashMap<>();
            Map<String, List<Path>> generated = new ConcurrentHashMap<>();
            Set<Path> generatedRoots = new HashSet<>();
            BUILD_DIRECTORIES.forEach(name -> generatedRoots.add(key.root().resolve(name)));
            List<Path> outsideTheModule = new ArrayList<>();
            for (String directory : key.generatedSources()) {
                Path configured = key.root().resolve(directory).normalize();
                (configured.startsWith(key.root()) ? generatedRoots : outsideTheModule).add(configured);
            }
            walk(key.root(), generatedRoots, maintained, generated);
            outsideTheModule.forEach(directory -> walk(directory, Set.of(directory), new ConcurrentHashMap<>(),
                    generated));
            return new ModuleSources(maintained, generated);
        }

        private static void walk(Path root, Set<Path> generatedRoots,
                Map<String, List<Path>> maintained, Map<String, List<Path>> generated) {
            try {
                Files.walkFileTree(root, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                        new SimpleFileVisitor<>() {

                    private Map<String, List<Path>> into = maintained;
                    private Path generatedRoot;

                    @Override
                    public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                        if (generatedRoot == null && generatedRoots.contains(directory)) {
                            generatedRoot = directory;
                            into = generated;
                        }
                        if (directory.equals(root)) {
                            return FileVisitResult.CONTINUE;
                        }
                        if (directory.getFileName().toString().startsWith(".") || isNestedModule(directory)) {
                            return FileVisitResult.SKIP_SUBTREE;
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

                    /** An unreadable directory or a symlink loop is skipped, never a reason to fail. */
                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException e) {
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
