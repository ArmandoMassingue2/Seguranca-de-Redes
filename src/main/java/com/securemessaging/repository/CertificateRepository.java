package com.securemessaging.repository;

import com.securemessaging.model.entity.Certificate;
import com.securemessaging.model.entity.User;
import com.securemessaging.model.enums.CertificateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findByOwner(User owner);
    Optional<Certificate> findBySerialNumber(String serialNumber);
    List<Certificate> findByStatus(CertificateStatus status);
    
    @Query("SELECT c FROM Certificate c WHERE c.validTo < :now")
    List<Certificate> findExpiredCertificates(LocalDateTime now);
    
    @Query("SELECT c FROM Certificate c WHERE c.owner = :owner AND c.status = :status")
    List<Certificate> findByOwnerAndStatus(User owner, CertificateStatus status);
}