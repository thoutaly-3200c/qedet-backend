# Product QR and receiving integration

The current warehouse flow receives `procurement.Product`, while the global
catalog uses `product.Product` and `OrganizationProduct`; those models have no
mapping. Receiving therefore cannot safely create a global-product identity or
reuse a supplier product QR across organizations without first adding an
explicit catalog mapping. This change does not infer such a mapping or create
another product.

The existing QR table, status lifecycle, audit events, scan events, and
globally unique code are reused. Product barcodes cannot preserve supplier QR
payloads because their normalizer removes punctuation and uppercases values.
Supplier serialized-unit QR identifiers are consequently stored verbatim on
the receiving serial and used as the QR code identifier at completion.

`STANDARD`, `WEIGHTED`, and `BUNDLE` receiving now resolves a product-level QR
identity for every accepted receipt, keyed by the existing receiving catalog
product and organization. No bundle expansion is introduced. `WEIGHTED`
continues to use the schema's existing decimal quantities. For `SERIALIZED`
items, the optional `serialIdentities` input records each serial, its optional
supplier QR identifier, and whether it was accepted. It is required whenever
a serialized receipt contains rejected units; the legacy `serialNumbers`
input remains valid only when all units are accepted.

QR identities are created only when a receiving operation completes, in the
same transaction as inventory application. The QR target uniqueness constraint
and resolve-or-create path make completion retries idempotent. Rejected serials
receive no inventory QR identity.
