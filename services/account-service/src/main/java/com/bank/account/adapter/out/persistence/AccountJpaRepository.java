package com.bank.account.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {
}
