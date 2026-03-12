package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "utilisateur")
@Getter @Setter
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usr_id")
    private Integer usrId;

    @Column(name = "usr_pseudo", nullable = false, unique = true, length = 50)
    private String usrPseudo;

    @Column(name = "usr_email", nullable = false, unique = true, length = 150)
    private String usrEmail;

    @Column(name = "usr_mdp", nullable = false)
    private String usrMdp;

    @Column(name = "usr_role", nullable = false, length = 20)
    private String usrRole;

    @Column(name = "usr_created_at", nullable = false, updatable = false)
    private LocalDateTime usrCreatedAt;

    @PrePersist
    protected void onCreate() {
        this.usrCreatedAt = LocalDateTime.now();
    }
}