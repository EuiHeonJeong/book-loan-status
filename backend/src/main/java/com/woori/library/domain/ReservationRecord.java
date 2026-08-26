package com.woori.library.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "reservation_record",
    uniqueConstraints = @UniqueConstraint(columnNames = {"library_account_id", "book_title", "reserved_at"}))
@Getter
@Setter
@NoArgsConstructor
public class ReservationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_account_id", nullable = false)
    private LibraryAccount libraryAccount;

    @Column(name = "book_title", nullable = false, length = 300)
    private String bookTitle;

    @Column(name = "branch_name", nullable = false, length = 20)
    private String branchName;

    @Column(name = "reserved_at", nullable = false)
    private LocalDate reservedAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "status_text", nullable = false, length = 50)
    private String statusText;

    @Column(name = "ready_notified_at")
    private Instant readyNotifiedAt;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt = Instant.now();

    public ReservationRecord(
        LibraryAccount libraryAccount,
        String bookTitle,
        String branchName,
        LocalDate reservedAt,
        LocalDate expiresAt,
        Integer rank,
        String statusText) {
        this.libraryAccount = libraryAccount;
        this.bookTitle = bookTitle;
        this.branchName = branchName;
        this.reservedAt = reservedAt;
        this.expiresAt = expiresAt;
        this.rank = rank;
        this.statusText = statusText;
    }
}
