package q.qedet.product.service;

import q.qedet.common.exception.BadRequestException;
import q.qedet.product.entity.ProductBarcodeType;

import java.util.Locale;

public final class BarcodeNormalizer {

    private BarcodeNormalizer() {
    }

    public static String normalize(String rawBarcode) {
        if (rawBarcode == null) {
            return "";
        }
        String normalized = rawBarcode.trim();
        normalized = normalized.replaceAll("[\\s\\p{Z}\\p{Cntrl}]+", "");
        normalized = normalized.replaceAll("[^A-Za-z0-9]", "");
        return normalized.toUpperCase(Locale.ROOT);
    }

    public static void validate(String rawBarcode, ProductBarcodeType barcodeType) {
        String normalized = normalize(rawBarcode);
        if (normalized.isBlank()) {
            throw new BadRequestException("Barcode is required");
        }
        if (barcodeType == null) {
            barcodeType = ProductBarcodeType.OTHER;
        }
        switch (barcodeType) {
            case EAN_13, EAN_8, UPC_A, UPC_E, GTIN -> {
                if (!normalized.matches("^[0-9]+$")) {
                    throw new BadRequestException("Barcode type requires numeric characters only");
                }
            }
            case CODE_128, QR, INTERNAL, OTHER -> {
                if (!normalized.matches("^[A-Za-z0-9]+$")) {
                    throw new BadRequestException("Barcode contains unsupported characters");
                }
            }
            default -> throw new BadRequestException("Unsupported barcode type");
        }
    }
}
