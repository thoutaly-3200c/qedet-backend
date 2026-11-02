package q.qedet.warehouse.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import q.qedet.warehouse.dto.WarehouseDtos;
import q.qedet.warehouse.service.WarehouseService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/warehouse")
public class InventoryController {
    private final WarehouseService warehouseService;

    public InventoryController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping("/inventory")
    public List<WarehouseDtos.InventoryResponse> inventory() {
        return warehouseService.inventory();
    }
}
