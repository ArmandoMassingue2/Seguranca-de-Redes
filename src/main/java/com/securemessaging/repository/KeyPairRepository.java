package com.securemessaging.repository;

import com.securemessaging.model.entity.KeyPair;
import com.securemessaging.model.entity.User;
import com.securemessaging.model.enums.EncryptionAlgorithm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KeyPairRepository extends JpaRepository<KeyPair, Long> {
    List<KeyPair> findByUserAndActive(User user, boolean active);
    Optional<KeyPair> findByUserAndAlgorithmAndActive(User user, EncryptionAlgorithm algorithm, boolean active);
    List<KeyPair> findByUser(User user);
}