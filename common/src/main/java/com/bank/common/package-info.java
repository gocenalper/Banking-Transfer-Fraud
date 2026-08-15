/**
 * Shared kernel (DDD ch. 14): the CONTRACTS every service speaks — money, identifiers and
 * event schemas. Subpackages are named after domain concepts (money, identity, event).
 *
 * <p>Rule: no business logic, entities, repositories or framework dependencies here.
 * In a microservice system a shared library is a source of coupling; only contracts are shared.
 */
package com.bank.common;
