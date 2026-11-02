package q.qedet.warehouse.dto;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReceivingDtosTest {

    @Test
    void exposesOnlyTheTwoQrResolutionStatusesUsedByTheReceivingUi() {
        assertThat(ReceivingDtos.SupplierQrStatus.values())
                .containsExactly(ReceivingDtos.SupplierQrStatus.REGISTRATION_REQUIRED,
                        ReceivingDtos.SupplierQrStatus.MATCHES_PRODUCT);
    }

    @Test
    void conflictResponseCarriesExistingAndProcurementProductContext() {
        UUID itemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ReceivingDtos.SupplierQrResolveResponse response = new ReceivingDtos.SupplierQrResolveResponse(
                "MATCHES_PRODUCT", "SUPPLIER-1", itemId, productId, "Catalog widget", "STANDARD",
                null, UUID.randomUUID(), productId, "PRODUCT", "Supplier widget", "SUP-1",
                "STANDARD_UNIT", "ea", true);

        assertThat(response.decisionRequired()).isTrue();
        assertThat(response.existingTargetId()).isEqualTo(productId);
        assertThat(response.procurementProductName()).isEqualTo("Supplier widget");
    }
}
