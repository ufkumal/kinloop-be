package com.kinloop.backend.repository;

import com.kinloop.backend.entity.ConsentDocument;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentDocumentRepository extends JpaRepository<ConsentDocument, Long> {
    List<ConsentDocument> findByActiveTrueOrderByDisplayOrderAsc();
}
