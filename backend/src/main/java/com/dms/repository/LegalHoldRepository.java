package com.dms.repository;

import com.dms.domain.LegalHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LegalHoldRepository extends JpaRepository<LegalHold, UUID> {
}
