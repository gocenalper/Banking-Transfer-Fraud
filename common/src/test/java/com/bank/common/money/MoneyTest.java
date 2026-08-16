package com.bank.common.money;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Plain JUnit — no Spring context: Money is pure domain, so its tests need no framework.
class MoneyTest {

    private static final Currency TRY = Currency.getInstance("TRY");
    private static final Currency USD = Currency.getInstance("USD");

    // --- construction invariants ---

    @Test
    void normalizesScaleSoEqualAmountsAreEqual() {
        // BigDecimal("10") and BigDecimal("10.00") are NOT equal (different scale);
        // constructor normalization makes the value objects equal anyway.
        Money a = Money.of(new BigDecimal("10"), TRY);
        Money b = Money.of(new BigDecimal("10.00"), TRY);

        assertThat(a).isEqualTo(b);
        assertThat(a.amount().scale()).isEqualTo(2);
    }

    @Test
    void rejectsSubMinorUnitPrecision() {
        // There is no such amount as 10.001 TRY; rounding silently would create/destroy money.
        assertThatThrownBy(() -> Money.of(new BigDecimal("10.001"), TRY))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void rejectsNullAmountWithClearMessage() {
        assertThatThrownBy(() -> Money.of(null, TRY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
    }

    // --- currency safety ---

    @Test
    void rejectsCrossCurrencyArithmetic() {
        Money lira = Money.of(new BigDecimal("10.00"), TRY);
        Money dollar = Money.of(new BigDecimal("10.00"), USD);

        assertThatThrownBy(() -> lira.add(dollar))
                .isInstanceOf(CurrencyMismatchException.class)
                .hasMessageContaining("TRY")
                .hasMessageContaining("USD");
    }

    // --- arithmetic behaviour ---

    @Test
    void addsAndSubtractsImmutably() {
        Money ten = Money.of(new BigDecimal("10.00"), TRY);
        Money three = Money.of(new BigDecimal("3.00"), TRY);

        Money sum = ten.add(three);

        assertThat(sum).isEqualTo(Money.of(new BigDecimal("13.00"), TRY));
        // the operands are untouched — value objects never mutate
        assertThat(ten).isEqualTo(Money.of(new BigDecimal("10.00"), TRY));
        assertThat(sum.subtract(three)).isEqualTo(ten);
    }

    @Test
    void signPredicatesMatchTheSignOfTheAmount() {
        assertThat(Money.of(new BigDecimal("0.01"), TRY).isPositive()).isTrue();
        assertThat(Money.of(new BigDecimal("-0.01"), TRY).isNegative()).isTrue();
        assertThat(Money.zero(TRY).isZero()).isTrue();
    }

    // --- algebraic properties: jqwik tries 1000 random samples to falsify each rule ---

    @Property
    void additionIsCommutative(@ForAll("money") Money a, @ForAll("money") Money b) {
        assertThat(a.add(b)).isEqualTo(b.add(a));
    }

    @Property
    void subtractThenAddIsIdentity(@ForAll("money") Money a, @ForAll("money") Money b) {
        assertThat(a.subtract(b).add(b)).isEqualTo(a);
    }

    @Property
    void negateIsItsOwnInverse(@ForAll("money") Money a) {
        assertThat(a.negate().negate()).isEqualTo(a);
    }

    @Provide
    Arbitrary<Money> money() {
        // Generate amounts in minor units (kuruş) so every sample already satisfies
        // the scale invariant: BigDecimal.valueOf(1050, 2) == 10.50
        return Arbitraries.longs().between(-1_000_000_00L, 1_000_000_00L)
                .map(minor -> Money.of(BigDecimal.valueOf(minor, 2), TRY));
    }
}
