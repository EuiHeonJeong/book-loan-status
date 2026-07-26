package com.woori.library.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "library_account", uniqueConstraints = @UniqueConstraint(columnNames = {"family_member_id"}))
@Getter
@Setter
@NoArgsConstructor
public class LibraryAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_member_id", nullable = false)
    private FamilyMember familyMember;

    @Column(name = "login_id", nullable = false, length = 20)
    private String loginId;

    /** AES-256-GCM 암호문(base64: iv+ciphertext+tag). 평문 비밀번호는 절대 저장하지 않는다. */
    @Column(name = "encrypted_password", nullable = false, columnDefinition = "text")
    private String encryptedPassword;

    @Column(name = "last_login_ok")
    private Boolean lastLoginOk;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    public LibraryAccount(FamilyMember familyMember, String loginId, String encryptedPassword) {
        this.familyMember = familyMember;
        this.loginId = loginId;
        this.encryptedPassword = encryptedPassword;
    }
}
