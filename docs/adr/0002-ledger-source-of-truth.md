# ADR 0002 — Ledger source of truth; bakiye türetilmiş projection; rezervasyon modeli

**Tarih:** 2026-08-14 · **Durum:** Kabul edildi

## Bağlam
Mikroservis kararından (ADR 0001) sonra ledger (`ledger_db`) ile hesap bakiyesi
(`account_db`) farklı servislerde ve farklı veritabanlarında yaşıyor. "Paranın gerçeği
nerede?" sorusu netleşmeliydi. Değerlendirilen seçenekler: (A) bakiye gerçek + ledger
audit log, (B) ledger gerçek + bakiye projection, (C) tam event sourcing.

## Karar: B + rezervasyon modeli
- **Gerçekleşmiş para hareketinin tarihsel gerçeği ledger'dır:** append-only çift kayıt
  (debit −, credit +, transfer başına toplam sıfır). Kayıt güncellenmez/silinmez;
  düzeltme ters kayıtla yapılır.
- **`account.balance` türetilmiş bir projection'dır:** hızlı okuma ve yetersiz bakiye
  kontrolü için tutulur; bozulursa ledger'dan yeniden inşa edilebilir.
- **Anlık "kullanılabilir bakiye" yetkilisi account-service'tir (rezervasyon modeli):**
  transfer başlarken account-service kendi DB'sinde atomik olarak available balance'tan
  düşüp bir hold yaratır; saga tamamlanınca hold ya ledger kaydına dönüşür ya iptal edilir.
  Yani soru başına bir source of truth: available → account, booked → ledger.
- **Reconciliation zorunludur:** `account.balance` ↔ ledger SUM'ını periyodik karşılaştırıp
  sapma raporlayan bir süreç (iki DB eventual consistent olduğu için opsiyonel değil).

## Reddedilenler
- **A:** ledger'a yazılamayan hareket = kayıp denetim izi; bankacılıkta kabul edilemez.
- **C:** event versiyonlama/snapshot/replay yükü, saga + MLOps öğrenme hedefleriyle
  yarışır; ledger zaten append-only gerçek + türetilmiş görünüm fikrini öğretiyor.

## Açık konular (ileride tartışılacak)
- Hold'lar ledger'a da yazılmalı mı (available vs booked balance ayrımının modellenmesi)?
- Reconciliation sapma bulunca otomatik düzeltme mi, alarm + insan kararı mı?
- account-service içinde sınırlı bir event sourcing denemesi yapılacak mı?
