package q.qedet.sales.controller;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import q.qedet.sales.dto.SalesDtos;
import q.qedet.sales.service.SalesService;
import q.qedet.sales.service.SalesReceivingService;
import q.qedet.sales.service.CashierService;
import q.qedet.sales.service.PosCheckoutService;
import java.util.*;
@RestController @RequestMapping("/api/v1/sales")
public class SalesController {
    private final SalesService service;
    private final SalesReceivingService receiving;
    private final CashierService cashier;
    private final PosCheckoutService checkout;
    public SalesController(SalesService service, SalesReceivingService receiving, CashierService cashier, PosCheckoutService checkout){this.service=service;this.receiving=receiving;this.cashier=cashier;this.checkout=checkout;}
    @GetMapping("/cashier/assignment")
    public SalesDtos.CashierAssignment cashierAssignment(){return cashier.assignment();}
    @PutMapping("/cashier/assignments/{cashierId}")
    public SalesDtos.PosAssignmentResponse assignCashier(@PathVariable UUID cashierId, @Valid @RequestBody SalesDtos.PosAssignmentRequest request){return cashier.assign(cashierId,request);}
    @PostMapping("/cashier/scan")
    public SalesDtos.CashierScanResponse cashierScan(@Valid @RequestBody SalesDtos.CashierScanRequest request){return cashier.scan(request);}
    @PostMapping("/cashier/quote") public SalesDtos.PosQuoteResponse cashierQuote(@Valid @RequestBody SalesDtos.PosQuoteRequest request){return checkout.quote(request);}
    @PostMapping("/cashier/sales") public SalesDtos.PosSaleResponse cashierSale(@RequestHeader("Idempotency-Key") String key, @Valid @RequestBody SalesDtos.PosSaleRequest request){return checkout.sale(key,request);}
    @GetMapping("/cashier/sales/{saleId}/receipt") public SalesDtos.PosReceiptResponse cashierReceipt(@PathVariable UUID saleId){return checkout.receipt(saleId);}
    @GetMapping("/sales-receiving-requests") public List<Map<String,Object>> pendingReceiving(){return receiving.pending();}
    @GetMapping("/sales-receiving-requests/{id}") public Map<String,Object> receivingRequest(@PathVariable UUID id){return receiving.get(id);}
    @PostMapping("/sales-receiving-requests/{id}/accept") public Map<String,Object> acceptReceivingRequest(@PathVariable UUID id,@Valid @RequestBody SalesDtos.ReceivingConfirmation body){return receiving.acceptReceivingRequest(id,body);}
    @GetMapping("/sales-stock-requests") public List<Map<String,Object>> requests(){return service.listRequests();}
    @GetMapping("/sales-stock-requests/{id}") public Map<String,Object> request(@PathVariable UUID id){return service.getRequest(id);}
    @PostMapping("/sales-stock-requests") public Map<String,Object> createRequest(@Valid @RequestBody SalesDtos.StockRequestCreate x){return service.createRequest(x);}
    @PostMapping("/sales-stock-requests/{id}/submit") public Map<String,Object> submitRequest(@PathVariable UUID id){return service.submitRequest(id);}
    @GetMapping("/warehouses/{id}/available-products") public List<Map<String,Object>> available(@PathVariable UUID id){return service.availableWarehouseProducts(id);}
    @PostMapping("/sales-receiving/place") public Map<String,Object> place(@Valid @RequestBody SalesDtos.PlaceStock x){return service.place(x);}
    @GetMapping("/sales-halls") public List<Map<String,Object>> halls(){return service.halls();}
    @GetMapping("/sales-hall-inventory") public List<Map<String,Object>> inventory(@RequestParam UUID salesHallId,@RequestParam(required=false) String search){return service.inventory(salesHallId,search);}
    @GetMapping("/sales-prices")
    public List<Map<String, Object>> prices() {
        return service.listPrices();
    }

    @PutMapping("/sales-prices")
    public Map<String, Object> updatePrice(
            @Valid @RequestBody SalesDtos.PriceUpdate x) {
        return service.updatePrice(x);
    }

    @GetMapping("/sales-prices/{id}/history")
    public List<Map<String, Object>> history(@PathVariable UUID id) {
        return service.priceHistory(id);
    }
    @GetMapping("/sales-packages") public List<Map<String,Object>> packages(@RequestParam UUID salesHallId){return service.listPackages(salesHallId);}
    @GetMapping("/sales-packages/{id}") public Map<String,Object> packageById(@PathVariable UUID id){return service.getPackage(id);}
    @PostMapping("/sales-packages") public Map<String,Object> createPackage(@Valid @RequestBody SalesDtos.PackageCreate x){return service.createPackage(x);}
    @GetMapping("/sales-returns") public List<Map<String,Object>> returns(){return service.listReturns();}
    @GetMapping("/sales-returns/{id}") public Map<String,Object> salesReturn(@PathVariable UUID id){return service.getReturn(id);}
    @PostMapping("/sales-returns") public Map<String,Object> createReturn(@Valid @RequestBody SalesDtos.ReturnCreate x){return service.createReturn(x);}
    @PostMapping("/sales-returns/{id}/submit") public Map<String,Object> submitReturn(@PathVariable UUID id){return service.submitReturn(id);}
    @PostMapping("/sales-returns/{id}/receive") public Map<String,Object> receiveReturn(@PathVariable UUID id){return service.receiveReturn(id);}
}
