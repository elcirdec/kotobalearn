package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "radical_animation")
@Getter @Setter
public class RadicalAnimation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ra_id")
    private Integer raId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rad_id", nullable = false)
    private Radical radical;

    @Column(name = "ra_order", nullable = false)
    private Integer raOrder;

    @Column(name = "ra_image_url", nullable = false)
    private String raImageUrl;
}