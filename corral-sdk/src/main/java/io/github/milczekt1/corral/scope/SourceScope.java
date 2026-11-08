package io.github.milczekt1.corral.scope;

import static com.tngtech.archunit.base.DescribedPredicate.describe;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.Source;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import io.github.milczekt1.corral.settings.CorralSettings;
import io.github.milczekt1.corral.settings.SettingsFile;
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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

/**
 * Separates the code a consumer maintains from the code a tool generated for them. A class is
 * generated when the source file it was compiled from lies only under its module's {@code target/},
 * {@code build/} or {@code out/}, or in a directory listed in {@value CorralSettings#GENERATED_SOURCES};
 * every other class is maintained, including one whose source file is found nowhere.
 *
 * <p>A class compiled without the {@code SourceFile} attribute is looked up by its top-level name, and
 * counts as generated when no such file exists either. That is the one way a maintained class drops
 * out of scope: compiled without debug info, from sources outside the module or sharing another class's
 * file. Classes from a jar, or from an output layout {@link OutputLayout} does not recognise, are
 * maintained.
 */
@UtilityClass
public class SourceScope {

    private static final Set<String> BUILD_DIRECTORIES = Set.of("target", "build", "out");

    private static final List<String> SOURCE_EXTENSIONS = List.of(".java", ".kt", ".groovy", ".scala");

    private static final Set<String> BUILD_FILES = Set.of("pom.xml", "build.gradle", "build.gradle.kts");

    private static final Map<ModuleKey, ModuleSources> MODULES = new ConcurrentHashMap<>();

    /** By the setting in effect; {@code JavaClass} has identity equality, so each import is its own key. */
    private static final Map<List<String>, Map<JavaClass, Boolean>> VERDICTS = new ConcurrentHashMap<>();

    private static final AtomicBoolean SKIPPED_PRINTED_THIS_RUN = new AtomicBoolean(false);

    /**
     * Classes compiled from a source file the consumer maintains. Rewording the description changes
     * no freeze-store key: it never reaches a violation line.
     */
    public static final DescribedPredicate<JavaClass> MAINTAINED = describe("maintained classes",
            javaClass -> isMaintained(javaClass, CorralSettings.current().generatedSources()));

    /**
     * {@code rule} evaluated on the {@link #MAINTAINED} classes it is given; the first evaluation this
     * run that skips any prints how many. Wrap inside {@code FreezingArchRule}, so a generated class
     * never reaches the store.
     */
    public static ArchRule maintainedOnly(ArchRule rule) {
        return maintainedOnly(rule, CorralSettings.current().generatedSources(), SKIPPED_PRINTED_THIS_RUN,
                FirstEvaluationWarning::printWarning);
    }

    static ArchRule maintainedOnly(
            ArchRule rule, List<String> generatedSources, AtomicBoolean printedThisRun, Consumer<String> sink) {
        return new MaintainedOnly(rule, List.copyOf(generatedSources), printedThisRun, sink);
    }

    /**
     * Whether both classes were compiled into the same module's output, generated or not; false when
     * either comes from a jar or from an output layout {@link OutputLayout} does not recognise. For a
     * condition that must tell project types from library types although {@link #maintainedOnly} has
     * removed generated classes from the set it is given.
     */
    public static boolean inSameModule(JavaClass first, JavaClass second) {
        Optional<Path> module = moduleRootOf(first);
        return module.isPresent() && module.equals(moduleRootOf(second));
    }

    private static Optional<Path> moduleRootOf(JavaClass javaClass) {
        return javaClass.getSource()
                .filter(source -> "file".equals(source.getUri().getScheme()))
                .flatMap(source -> outputRootOf(Path.of(source.getUri()), javaClass))
                .flatMap(OutputLayout::moduleRootOf);
    }

    static boolean isMaintained(JavaClass javaClass, List<String> generatedSources) {
        Map<JavaClass, Boolean> verdicts = VERDICTS.computeIfAbsent(List.copyOf(generatedSources),
                ignored -> Collections.synchronizedMap(new WeakHashMap<>()));
        Boolean verdict = verdicts.get(javaClass);
        if (verdict == null) {
            verdict = decide(javaClass, generatedSources);
            verdicts.put(javaClass, verdict);
        }
        return verdict;
    }

    private static boolean decide(JavaClass javaClass, List<String> generatedSources) {
        Optional<Path> moduleRoot = moduleRootOf(javaClass);
        if (moduleRoot.isEmpty()) {
            return true;
        }
        Source source = javaClass.getSource().orElseThrow();
        ModuleSources sources = MODULES.computeIfAbsent(new ModuleKey(moduleRoot.get(), List.copyOf(generatedSources)),
                ModuleSources::index);
        Optional<String> recorded = source.getFileName();
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
        String packageName = javaClass.getPackageName();
        int segments = packageName.isEmpty() ? 0 : packageName.split("\\.").length;
        for (int segment = 0; segment < segments && root != null; segment++) {
            root = root.getParent();
        }
        return Optional.ofNullable(root);
    }

    private static boolean isIn(Path file, Path packageDirectory) {
        return packageDirectory.toString().isEmpty() || file.getParent().endsWith(packageDirectory);
    }

    /** The empty path, which has one name element, for the default package. */
    private static Path packageDirectoryOf(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        return packageName.isEmpty() ? Path.of("") : Path.of("", packageName.split("\\."));
    }

    private record ModuleKey(Path root, List<String> generatedSources) {
    }

    @RequiredArgsConstructor
    private static final class MaintainedOnly implements ArchRule {

        private final ArchRule delegate;
        private final List<String> generatedSources;
        private final AtomicBoolean printedThisRun;
        private final Consumer<String> sink;

        @Override
        public void check(JavaClasses classes) {
            delegate.check(maintainedClasses(classes));
        }

        @Override
        public EvaluationResult evaluate(JavaClasses classes) {
            return delegate.evaluate(maintainedClasses(classes));
        }

        private JavaClasses maintainedClasses(JavaClasses classes) {
            JavaClasses kept = classes.that(describe("maintained classes",
                    javaClass -> isMaintained(javaClass, generatedSources)));
            int skipped = classes.size() - kept.size();
            if (skipped > 0 && printedThisRun.compareAndSet(false, true)) {
                sink.accept("checking maintained code only; skipped " + skipped + " generated class"
                        + (skipped == 1 ? "" : "es") + " in this scan: compiled from sources under target/,"
                        + " build/, out/ or " + CorralSettings.GENERATED_SOURCES + ", or recording no source file"
                        + " that could be found.");
            }
            return kept;
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }

        @Override
        public ArchRule as(String newDescription) {
            return new MaintainedOnly(delegate.as(newDescription), generatedSources, printedThisRun, sink);
        }

        @Override
        public ArchRule because(String reason) {
            return new MaintainedOnly(delegate.because(reason), generatedSources, printedThisRun, sink);
        }

        @Override
        public ArchRule allowEmptyShould(boolean allowEmptyShould) {
            return new MaintainedOnly(delegate.allowEmptyShould(allowEmptyShould), generatedSources,
                    printedThisRun, sink);
        }
    }

    /** The module's source files, by file name. */
    private record ModuleSources(Map<String, List<Path>> maintained, Map<String, List<Path>> generated) {

        static ModuleSources index(ModuleKey key) {
            Map<String, List<Path>> maintained = new ConcurrentHashMap<>();
            Map<String, List<Path>> generated = new ConcurrentHashMap<>();
            Set<Path> generatedRoots = new HashSet<>();
            BUILD_DIRECTORIES.forEach(name -> generatedRoots.add(key.root().resolve(name)));
            List<Path> outsideTheModule = new ArrayList<>();
            for (String directory : key.generatedSources()) {
                Path configured = key.root().resolve(directory).normalize();
                boolean insideTheModule = configured.startsWith(key.root());
                if (insideTheModule && SettingsFile.coversSourceRoot(segmentsOf(key.root().relativize(configured)))) {
                    throw new IllegalStateException(CorralSettings.GENERATED_SOURCES + " entry '" + directory
                            + "' resolves to " + configured + ", a source root of this module, and would skip every"
                            + " maintained class under it. List the directory the generator writes to.");
                }
                (insideTheModule ? generatedRoots : outsideTheModule).add(configured);
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
                            return FileVisitResult.CONTINUE;
                        }
                        if (directory.equals(root)) {
                            return FileVisitResult.CONTINUE;
                        }
                        String name = directory.getFileName().toString();
                        if (name.startsWith(".") || name.equals("node_modules") || isNestedModule(directory)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                        String name = file.getFileName().toString();
                        if (SOURCE_EXTENSIONS.stream().anyMatch(name::endsWith)) {
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

        private static List<String> segmentsOf(Path relative) {
            List<String> segments = new ArrayList<>();
            relative.forEach(segment -> segments.add(segment.toString()));
            return segments;
        }

        private static boolean isNestedModule(Path directory) {
            return BUILD_FILES.stream().anyMatch(name -> Files.isRegularFile(directory.resolve(name)));
        }
    }
}
