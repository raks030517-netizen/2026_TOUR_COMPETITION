package com.roamate.backend.domain.place;

import com.roamate.backend.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "places", uniqueConstraints = @UniqueConstraint(columnNames = "content_id"))
public class Place extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id", nullable = false)
    private String contentId;

    @Column(nullable = false)
    private String name;

    private String category;

    private String address;

    private Double latitude;

    private Double longitude;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Builder
    public Place(String contentId, String name, String category, String address, Double latitude, Double longitude,
                 String imageUrl) {
        this.contentId = contentId;
        this.name = name;
        this.category = category;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
    }
}
