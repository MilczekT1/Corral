package io.github.milczekt1.corral.settings;

import static io.github.milczekt1.corral.settings.CorralSettings.BASE_PACKAGE;
import static io.github.milczekt1.corral.settings.CorralSettings.FILE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SettingsFileTest {

    private static final String SOURCE = "file:/somewhere/" + FILE;

    @Test
    void readsKnownKeys() {
        SettingsFile settings = SettingsFile.parse("""
                corral.base-package=com.acme
                corral.ignore-patterns.fail=false
                corral.exclude.corral.logging.no-system-err=CLI
                """, SOURCE);

        assertFalse(settings.isBroken(), settings::problem);
        assertEquals(Optional.of("com.acme"), settings.value(BASE_PACKAGE));
        assertFalse(settings.ignorePatternsFail());
        assertEquals(Map.of("corral.logging.no-system-err", "CLI"), settings.exclusionReasons());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "corral.ignore-patterns.fail=true", "corral.ignore-patterns.fail=no",
            "corral.ignore-patterns.fail=flase"})
    void ignorePatternsFailStaysArmedUnlessLiterallyFalse(String contents) {
        assertTrue(SettingsFile.parse(contents, SOURCE).ignorePatternsFail());
    }

    @Test
    void ignorePatternsFailIsCaseInsensitive() {
        assertFalse(SettingsFile.parse("corral.ignore-patterns.fail=FALSE", SOURCE).ignorePatternsFail());
    }

    @Test
    void anUnknownCorralKeyBreaksTheFile() {
        SettingsFile settings = SettingsFile.parse("corral.base-pakage=com.acme", SOURCE);

        assertTrue(settings.isBroken());
        assertTrue(settings.problem().contains("corral.base-pakage"), settings::problem);
        assertTrue(settings.problem().contains(SOURCE), settings::problem);
        assertTrue(settings.values().isEmpty());
    }

    @Test
    void aConsumerKeyIsAccepted() {
        SettingsFile settings = SettingsFile.parse("acme.service-package=x", SOURCE);

        assertFalse(settings.isBroken(), settings::problem);
        assertEquals(Optional.of("x"), settings.value("acme.service-package"));
    }

    @Test
    void aDuplicateKeyBreaksTheFile() {
        SettingsFile settings = SettingsFile.parse("""
                corral.exclude.corral.logging.no-system-err=one reason
                corral.exclude.corral.logging.no-system-err=another reason
                """, SOURCE);

        assertTrue(settings.isBroken());
        assertTrue(settings.problem().contains("corral.exclude.corral.logging.no-system-err"), settings::problem);
    }

    @Test
    void everyProblemIsReportedNotOnlyTheFirst() {
        SettingsFile settings = SettingsFile.parse("""
                corral.base-pakage=com.acme
                corral.base-package=com..acme
                """, SOURCE);

        assertTrue(settings.problem().contains("corral.base-pakage"), settings::problem);
        assertTrue(settings.problem().contains(BASE_PACKAGE), settings::problem);
    }

    @ParameterizedTest
    @ValueSource(strings = {"com.acme.", "com..acme", "com.acme-x", "1com", ".com"})
    void anInvalidBasePackageBreaksTheFile(String basePackage) {
        SettingsFile settings = SettingsFile.parse(BASE_PACKAGE + "=" + basePackage, SOURCE);

        assertTrue(settings.isBroken(), () -> basePackage + " was accepted");
        assertTrue(settings.problem().contains(BASE_PACKAGE), settings::problem);
    }

    @Test
    void aBlankBasePackageBreaksTheFile() {
        SettingsFile settings = SettingsFile.parse(BASE_PACKAGE + "=", SOURCE);

        assertTrue(settings.isBroken());
        assertTrue(settings.problem().contains(BASE_PACKAGE), settings::problem);
    }

    @Test
    void trailingWhitespaceIsStripped() {
        SettingsFile settings = SettingsFile.parse(BASE_PACKAGE + "=com.acme   ", SOURCE);

        assertFalse(settings.isBroken(), settings::problem);
        assertEquals(Optional.of("com.acme"), settings.value(BASE_PACKAGE));
    }

    @Test
    void aBlankExclusionReasonBreaksTheFile() {
        SettingsFile settings = SettingsFile.parse("corral.exclude.corral.logging.no-system-err=   ", SOURCE);

        assertTrue(settings.isBroken());
        assertTrue(settings.problem().contains("corral.logging.no-system-err"), settings::problem);
    }

    @Test
    void anExclusionKeyThatIsNotAnIdBreaksTheFile() {
        SettingsFile settings = SettingsFile.parse("corral.exclude.Not_An_Id=x", SOURCE);

        assertTrue(settings.isBroken());
        assertTrue(settings.problem().contains("Not_An_Id"), settings::problem);
    }

    @Test
    void separatorsInsideAReasonAreKept() {
        SettingsFile settings = SettingsFile.parse("corral.exclude.a.no-b=see ADR: x=y # not a comment", SOURCE);

        assertEquals(Map.of("a.no-b", "see ADR: x=y # not a comment"), settings.exclusionReasons());
    }

    @Test
    void nonAsciiReasonsSurvive(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve(FILE), "corral.exclude.a.no-b=ADR — 021, zażółć\n", UTF_8);

        try (URLClassLoader classLoader = classLoaderOver(root)) {
            SettingsFile settings = SettingsFile.load(classLoader);

            assertEquals(Map.of("a.no-b", "ADR — 021, zażółć"), settings.exclusionReasons());
        }
    }

    @Test
    void aLeadingByteOrderMarkIsIgnored() {
        SettingsFile settings = SettingsFile.parse("﻿" + BASE_PACKAGE + "=com.acme", SOURCE);

        assertEquals(Optional.of("com.acme"), settings.value(BASE_PACKAGE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "# nothing\n"})
    void anEmptyOrCommentOnlyFileIsFine(String contents) {
        SettingsFile settings = SettingsFile.parse(contents, SOURCE);

        assertFalse(settings.isBroken(), settings::problem);
        assertTrue(settings.values().isEmpty());
        assertNull(settings.classPathWarning());
    }

    @Test
    void noFileMeansNoSettingsAndNoProblem(@TempDir Path emptyRoot) throws IOException {
        try (URLClassLoader classLoader = classLoaderOver(emptyRoot)) {
            SettingsFile settings = SettingsFile.load(classLoader);

            assertFalse(settings.isBroken(), settings::problem);
            assertNull(settings.source());
            assertTrue(settings.values().isEmpty());
            assertNull(settings.classPathWarning());
        }
    }

    @Test
    void theFirstFileOnTheClassPathWins(@TempDir Path first, @TempDir Path second) throws IOException {
        Files.writeString(first.resolve(FILE), BASE_PACKAGE + "=com.mine\n");
        Files.writeString(second.resolve(FILE), BASE_PACKAGE + "=com.theirs\n");

        try (URLClassLoader classLoader = classLoaderOver(first, second)) {
            SettingsFile settings = SettingsFile.load(classLoader);

            assertEquals(Optional.of("com.mine"), settings.value(BASE_PACKAGE));
            assertTrue(settings.source().contains(first.toUri().getPath()), settings::source);
            assertEquals(1, settings.ignoredSources().size());
            assertTrue(settings.ignoredSources().get(0).contains(second.toUri().getPath()));
            String warning = settings.classPathWarning();
            assertTrue(warning.contains(first.toUri().getPath()), warning);
            assertTrue(warning.contains(second.toUri().getPath()), warning);
        }
    }

    @Test
    void aFileInsideAJarAlwaysWarns(@TempDir Path root) throws IOException {
        Path jar = root.resolve("shared-tests.jar");
        try (OutputStream out = Files.newOutputStream(jar); JarOutputStream jarOut = new JarOutputStream(out)) {
            jarOut.putNextEntry(new JarEntry(FILE));
            jarOut.write((BASE_PACKAGE + "=com.shared\n").getBytes(UTF_8));
            jarOut.closeEntry();
        }

        try (URLClassLoader classLoader = classLoaderOver(jar)) {
            SettingsFile settings = SettingsFile.load(classLoader);

            assertTrue(settings.ignoredSources().isEmpty());
            String warning = settings.classPathWarning();
            assertTrue(warning != null && warning.contains("jar:"), () -> "no jar warning: " + warning);
        }
    }

    /**
     * {@code load} runs in a static initialiser, where an escaping exception becomes an
     * {@code ExceptionInInitializerError} and every later call an {@code NoClassDefFoundError}.
     */
    @Test
    void anUncheckedFailureWhileReadingIsAProblemRatherThanEscaping() {
        ClassLoader throwsUnchecked = new ClassLoader(null) {
            @Override
            public Enumeration<URL> getResources(String name) {
                throw new IllegalStateException("a URL handler misbehaved");
            }
        };

        SettingsFile settings = assertDoesNotThrow(() -> SettingsFile.load(throwsUnchecked));

        assertTrue(settings.isBroken());
        assertTrue(settings.problem().contains(FILE), settings::problem);
    }

    @Test
    void anUnreadableClassPathIsAProblem() {
        ClassLoader unreadable = new ClassLoader(null) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                throw new IOException("classpath unreadable");
            }
        };

        SettingsFile settings = SettingsFile.load(unreadable);

        assertTrue(settings.isBroken());
        assertTrue(settings.problem().contains(FILE), settings::problem);
    }

    @Test
    void thisBuildCarriesNoSettingsFile() {
        SettingsFile current = CorralSettings.current();

        assertNull(current.source(), "this module must not commit a " + FILE + ", or every test here proves less");
        assertFalse(current.isBroken(), current::problem);
    }

    private static URLClassLoader classLoaderOver(Path... roots) throws IOException {
        URL[] urls = new URL[roots.length];
        for (int i = 0; i < roots.length; i++) {
            urls[i] = roots[i].toUri().toURL();
        }
        return new URLClassLoader(urls, null);
    }
}
