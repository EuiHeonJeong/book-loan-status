package com.woori.library.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "family_member")
@Getter
@Setter
@NoArgsConstructor
public class FamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "is_self", nullable = false)
    private boolean isSelf;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "familyMember", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LibraryAccount> libraryAccounts = new ArrayList<>();

    public FamilyMember(Long ownerUserId, String name, boolean isSelf) {
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.isSelf = isSelf;
    }
}
