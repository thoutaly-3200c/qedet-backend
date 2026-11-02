package q.qedet.sales.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.finance.entity.CashBox;
import java.math.BigDecimal;
@Entity
@Table(name = "sales_pos_sale_payments")
@Getter
@Setter
@NoArgsConstructor
public class PosSalePayment extends TimestampedEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false, unique = true)
    private PosSale sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_box_id", nullable = false)
    private CashBox cashBox;

    @Column(nullable = false, length = 16)
    private String method;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountReceived;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal changeAmount;

    @Column(length = 160)
    private String reference;
}