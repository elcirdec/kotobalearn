package com.kotobalearn.backend.repository;

import com.kotobalearn.backend.model.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProgressRepository extends JpaRepository<Progress, Integer> {

    Optional<Progress> findByUtilisateur_UsrIdAndProgElementTypeAndProgElementId(
        Integer userId, String elementType, Integer elementId
    );

    List<Progress> findByUtilisateur_UsrIdAndProgElementType(
        Integer userId, String elementType
    );

    // Éléments à réviser maintenant (pour la répétition espacée)
    List<Progress> findByUtilisateur_UsrIdAndProgNextReviewBefore(
        Integer userId, LocalDateTime now
    );
}