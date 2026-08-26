package com.woori.library.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "mutual_loan_history_record",
    uniqueConstraints = @UniqueConstraint(columnNames = {"library_account_id", "book_title", "applied_at"}))
@Getter
@Setter
@NoArgsConstructor
public class MutualLoanHistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_account_id", nullable = false)
    private LibraryAccount libraryAccount;

    @Column(name = "book_title", nullable = false, length = 300)
    private String bookTitle;

    @Column(name = "applied_at", nullable = false)
    private LocalDate appliedAt;

    @Column(name = "branch_name", nullable = false, length = 20)
    private String branchName;

    @Column(name = "pickup_branch_name", nullable = false, length = 20)
    private String pickupBranchName;

    @Column(name = "status_text", nullable = false, length = 50)
    private String statusText;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt = Instant.now();

    public MutualLoanHistoryRecord(
        LibraryAccount libraryAccount,
        String bookTitle,
        LocalDate appliedAt,
        String branchName,
        String pickupBranchName,
        String statusText) {
        this.libraryAccount = libraryAccount;
        this.bookTitle = bookTitle;
        this.appliedAt = appliedAt;
        this.branchName = branchName;
        this.pickupBranchName = pickupBranchName;
        this.statusText = statusText;
    }
}
