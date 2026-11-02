package q.qedet.finance.controller;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceControllerCashBoxApiTest {
    @Test
    void doesNotExposeManualCashboxCreation() {
        assertThat(FinanceController.class.getDeclaredMethods())
                .extracting(Method::getName)
                .doesNotContain("createCashBox");
    }
}
