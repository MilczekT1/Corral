package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.mock.web.MockMultipartFile;

/** MUST FLAG {@code org.springframework.mock..}: a spring-test mock wrapping a real file. */
public class ReportUploader {

    MockMultipartFile asUpload(Path report) throws Exception {
        return new MockMultipartFile("report", Files.readAllBytes(report));
    }
}
