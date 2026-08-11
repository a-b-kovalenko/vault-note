package com.andrii.vaultnote.notes.infrastructure.persistence.repository;

import com.andrii.vaultnote.notes.infrastructure.persistence.entity.NoteEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoteJpaRepository extends JpaRepository<NoteEntity, Long> {

  Optional<NoteEntity> findByIdAndOwner_Id(Long id, Long ownerId);

  Page<NoteEntity> findByOwner_Id(Long ownerId, Pageable pageable);
}
