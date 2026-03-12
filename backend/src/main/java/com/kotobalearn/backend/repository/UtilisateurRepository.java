package com.kotobalearn.backend.repository;

import com.kotobalearn.backend.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {

    Optional<Utilisateur> findByUsrEmail(String email);

    Optional<Utilisateur> findByUsrPseudo(String pseudo);

    boolean existsByUsrEmail(String email);
}