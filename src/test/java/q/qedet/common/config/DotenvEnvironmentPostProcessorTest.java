package q.qedet.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DotenvEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void parseReadsKeysAndIgnoresComments() throws Exception {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, """
                # comment
                QEDET_EMAIL_ENABLED=true
                RESEND_API_KEY="re_test"
                export QEDET_FRONTEND_BASE_URL=http://localhost:8443 # comment
                APP_URL="https://example.com/path#section" # trailing comment
                
                INVALID
                =novalue
                """);

        Map<String, Object> values = DotenvEnvironmentPostProcessor.parse(envFile);

        assertThat(values)
                .containsEntry("QEDET_EMAIL_ENABLED", "true")
                .containsEntry("RESEND_API_KEY", "re_test")
                .containsEntry("QEDET_FRONTEND_BASE_URL", "http://localhost:8443")
                .containsEntry("APP_URL", "https://example.com/path#section")
                .doesNotContainKey("");
    }
}
