package q.qedet.organization.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyLocationCleanupMigrationTest {
    @Test
    void aborts_when_a_business_reference_cannot_be_moved_and_never_disables_foreign_keys() throws IOException {
        try (var input = getClass().getResourceAsStream(
                "/db/migration/V20261029_01__reset_disposable_legacy_locations.sql")) {
            assertThat(input).isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql).contains("Legacy purchase-order delivery locations require explicit migration");
            assertThat(sql).contains("A receiving zone assignment could not be moved to a canonical zone");
            assertThat(sql).contains("An inventory balance could not be moved to a canonical zone");
            assertThat(sql).doesNotContain("FOREIGN_KEY_CHECKS");
        }
    }
}
