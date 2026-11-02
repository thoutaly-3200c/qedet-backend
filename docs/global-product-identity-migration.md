# Purchase-request global product migration

`purchase_request_items.product_id` remains the legacy Procurement identity.
The application now reads `purchase_request_items.global_product_id`, which
references `products.id`. The staging migration creates that column and the
`procurement_product_global_mappings` crosswalk without changing legacy IDs.

Before applying the final foreign-key cutover, populate one reviewed mapping
for every legacy product used by a purchase request. Each mapping must point to
an existing global product and an existing `organization_products` link for the
same organization. Do not copy UUIDs between tables or derive mappings from a
name, SKU, or barcode alone.

Use these checks after inserting reviewed mappings:

```sql
SELECT HEX(pri.id) AS purchase_request_item_id,
       HEX(pri.product_id) AS legacy_product_id,
       pri.product_name,
       pri.sku
FROM purchase_request_items pri
WHERE pri.product_id IS NOT NULL
  AND pri.global_product_id IS NULL;

SELECT HEX(map.legacy_product_id), HEX(map.global_product_id)
FROM procurement_product_global_mappings map
LEFT JOIN organization_products op
  ON op.organization_id = map.organization_id
 AND op.product_id = map.global_product_id
WHERE op.id IS NULL;
```

Both queries must return no rows before making `global_product_id` mandatory or
replacing the legacy `product_id` foreign key. Existing rows without a reviewed
mapping remain preserved, but receiving them is intentionally blocked.
