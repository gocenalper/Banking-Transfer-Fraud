# ADR 0001 — Baştan mikroservis, monorepo, servis başına hexagonal mimari

**Tarih:** 2026-08-14 · **Durum:** Kabul edildi (modüler monolit alternatifinin yerine)

## Bağlam
Projenin ana amacı dağıtık sistemlerin zorlu problemlerini (saga, partial failure,
idempotency, eventual consistency) bizzat yaşayarak öğrenmek. Modüler monolit bu
problemleri erteler; network sınırı olmadan bu kaslar çalışmaz.

## Karar
- Her bounded context ayrı deployable mikroservis: `account`, `ledger`, `transfer`,
  `notification` (Java/Spring Boot) + `fraud` (Python/FastAPI).
- **Monorepo**: tek repo, tek Maven reactor — kod paylaşımı ve refactor kolaylığı;
  deployment yine servis başına bağımsız.
- **Database-per-service**: lokalde tek PG konteyneri ama servis başına ayrı veritabanı
  (`account_db`, `ledger_db`, `transfer_db`); sınır prod ile aynı.
- `common` kütüphanesi yalnızca sözleşme taşır (Money, ID'ler, event şemaları) —
  iş mantığı, entity veya Spring bağımlılığı yasak.
- Servis içi mimari hexagonal; katmanlar servis başına ArchUnit testiyle zorlanır.
- Servisler arası iletişim: başlangıçta senkron REST (transfer → account/ledger) +
  Kafka event'leri; saga orchestration transfer-service'te.

## Bilinçli kabul edilen bedeller
- Çift kayıt invariant'ı artık servisler arası **eventual consistency** ile korunur;
  reconciliation job zorunlu hale gelir.
- Transactional outbox tek yerde değil, yazan her serviste gerekir.
- Operasyonel yük: servis başına migration, health check, deployment.
