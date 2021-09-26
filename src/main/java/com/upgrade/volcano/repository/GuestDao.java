package com.upgrade.volcano.repository;

import com.upgrade.volcano.model.Guest;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface GuestDao extends CrudRepository<Guest, UUID> {
    Optional<Guest> findByEmail(String email);
}
