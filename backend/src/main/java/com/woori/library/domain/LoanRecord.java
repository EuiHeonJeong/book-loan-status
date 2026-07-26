package com.woori.library.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "loan_record",
    uniqueConstraints = @UniqueConstraint(columnNames = {"library_account_id", "book_title", "loan_date"}))
@Getter
@Setter
@NoArgsConstructor
public class LoanRecord {

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

    @Column(name = "loan_date", nullable = false)
    private LocalDate loanDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt = Instant.now();

    public LoanRecord(LibraryAccount libraryAccount, String bookTitle, String branchName, LocalDate loanDate, LocalDate dueDate) {
        this.libraryAccount = libraryAccount;
        this.bookTitle = bookTitle;
        this.branchName = branchName;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
    }
}
