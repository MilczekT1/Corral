package io.github.milczekt1.archrules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class BuildEnvironmentTest {

    /**
     * Guards against maven-compiler-plugin silently ignoring {@code maven.compiler.release}.
     * Class file major version 69 == Java 25.
     */
    @Test
    void compilesToJava25Bytecode() throws IOException {
        String resource = "/" + BuildEnvironmentTest.class.getName().replace('.', '/') + ".class";
        try (InputStream in = BuildEnvironmentTest.class.getResourceAsStream(resource);
             DataInputStream data = new DataInputStream(in)) {
            assertEquals(0xCAFEBABE, data.readInt(), "not a class file");
            data.readUnsignedShort(); // minor version
            assertEquals(69, data.readUnsignedShort(), "expected Java 25 (major 69) bytecode");
        }
    }

    @Test
    void archUnitCanImportJava25Bytecode() {
        var classes = new ClassFileImporter().importPackages("io.github.milczekt1.archrules");
        assertTrue(classes.size() > 0, "ArchUnit imported no classes");
    }
}
