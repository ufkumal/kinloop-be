package com.kinloop.backend.repository;

import com.kinloop.backend.entity.ConsentDocument;
import com.kinloop.backend.entity.enums.ConsentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentDocumentRepository extends JpaRepository<ConsentDocument, Long> {
    List<ConsentDocument> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<ConsentDocument> findFirstByTypeAndActiveTrue(ConsentType type);
}
