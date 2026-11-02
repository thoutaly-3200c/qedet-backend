package q.qedet.warehouse.controller;

import org.junit.jupiter.api.Test;
import q.qedet.warehouse.dto.WarehouseDtos;
import q.qedet.warehouse.service.WarehouseService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryControllerTest {
    @Test
    void exposesInventoryResponsesAtTheWarehouseController() {
        WarehouseService service = mock(WarehouseService.class);
        WarehouseDtos.InventoryResponse row = new WarehouseDtos.InventoryResponse(UUID.randomUUID(), UUID.randomUUID(), "Widget", "W-1",
                UUID.randomUUID(), "DEFAULT", "Default", UUID.randomUUID(), "WAREHOUSE", "Warehouse",
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE);
        when(service.inventory()).thenReturn(List.of(row));

        assertThat(new InventoryController(service).inventory()).containsExactly(row);
    }
}
