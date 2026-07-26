package com.woori.library.controller;

import com.woori.library.config.AppOAuth2User;
import com.woori.library.domain.FamilyMember;
import com.woori.library.domain.LibraryAccount;
import com.woori.library.dto.CreateLibraryAccountRequest;
import com.woori.library.dto.CreateMemberRequest;
import com.woori.library.dto.LibraryAccountResponse;
import com.woori.library.dto.MemberResponse;
import com.woori.library.dto.UpdateLibraryAccountRequest;
import com.woori.library.dto.UpdateMemberRequest;
import com.woori.library.repository.FamilyMemberRepository;
import com.woori.library.repository.LibraryAccountRepository;
import com.woori.library.service.crawler.IsslAuthException;
import com.woori.library.service.crawler.IsslLoginService;
import com.woori.library.service.crawler.IsslSession;
import com.woori.library.service.crypto.AesGcmCipherService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * open-in-view=false라 컨트롤러 메서드 실행 동안 트랜잭션이 열려있어야 지연 로딩 연관관계
 * (FamilyMember.libraryAccounts, LibraryAccount.familyMember)에 접근할 수 있다 — 클래스 전체에 적용.
 */
@Transactional
@RestController
@RequestMapping("/api")
public class MemberController {

    private static final Logger log = LoggerFactory.getLogger(MemberController.class);

    private final FamilyMemberRepository familyMemberRepository;
    private final LibraryAccountRepository libraryAccountRepository;
    private final AesGcmCipherService cipherService;
    private final IsslLoginService isslLoginService;

    public MemberController(
        FamilyMemberRepository familyMemberRepository,
        LibraryAccountRepository libraryAccountRepository,
        AesGcmCipherService cipherService,
        IsslLoginService isslLoginService) {
        this.familyMemberRepository = familyMemberRepository;
        this.libraryAccountRepository = libraryAccountRepository;
        this.cipherService = cipherService;
        this.isslLoginService = isslLoginService;
    }

    @GetMapping("/members")
    public List<MemberResponse> listMembers(@AuthenticationPrincipal AppOAuth2User principal) {
        return familyMemberRepository.findByOwnerUserId(principal.getAppUserId()).stream()
            .map(this::toResponse)
            .toList();
    }

    @PostMapping("/members")
    public MemberResponse addMember(
        @AuthenticationPrincipal AppOAuth2User principal, @Valid @RequestBody CreateMemberRequest request) {
        FamilyMember member = familyMemberRepository.save(new FamilyMember(principal.getAppUserId(), request.name(), false));
        return toResponse(member);
    }

    @PutMapping("/members/{id}")
    public MemberResponse updateMember(
        @AuthenticationPrincipal AppOAuth2User principal,
        @PathVariable Long id,
        @Valid @RequestBody UpdateMemberRequest request) {
        FamilyMember member = ownedMember(principal.getAppUserId(), id);
        member.setName(request.name());
        familyMemberRepository.save(member);
        return toResponse(member);
    }

    @DeleteMapping("/members/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal AppOAuth2User principal, @PathVariable Long id) {
        FamilyMember member = ownedMember(principal.getAppUserId(), id);
        if (member.isSelf()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인 카드는 삭제할 수 없습니다.");
        }
        familyMemberRepository.delete(member);
    }

    /** 도서관 계정 등록 — 저장 전에 실제 issl.go.kr 로그인으로 자격증명을 검증한다(docs/spec.md §6). */
    @PostMapping("/members/{id}/library-accounts")
    public LibraryAccountResponse addLibraryAccount(
        @AuthenticationPrincipal AppOAuth2User principal,
        @PathVariable Long id,
        @Valid @RequestBody CreateLibraryAccountRequest request) {
        FamilyMember member = ownedMember(principal.getAppUserId(), id);

        verifyLibraryLogin(request.loginId(), request.password());

        LibraryAccount account = new LibraryAccount(member, request.loginId(), cipherService.encrypt(request.password()));
        account.setLastLoginOk(true);
        libraryAccountRepository.save(account);
        return new LibraryAccountResponse(account.getId(), account.getLoginId());
    }

    @PutMapping("/library-accounts/{id}")
    public LibraryAccountResponse updateLibraryAccount(
        @AuthenticationPrincipal AppOAuth2User principal,
        @PathVariable Long id,
        @RequestBody UpdateLibraryAccountRequest request) {
        LibraryAccount account = ownedLibraryAccount(principal.getAppUserId(), id);

        String loginId = request.loginId() != null ? request.loginId() : account.getLoginId();
        if (request.password() != null && !request.password().isBlank()) {
            verifyLibraryLogin(loginId, request.password());
            account.setEncryptedPassword(cipherService.encrypt(request.password()));
            account.setLastLoginOk(true);
        }
        account.setLoginId(loginId);
        libraryAccountRepository.save(account);
        return new LibraryAccountResponse(account.getId(), account.getLoginId());
    }

    @DeleteMapping("/library-accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLibraryAccount(@AuthenticationPrincipal AppOAuth2User principal, @PathVariable Long id) {
        libraryAccountRepository.delete(ownedLibraryAccount(principal.getAppUserId(), id));
    }

    private void verifyLibraryLogin(String loginId, String password) {
        try (IsslSession session = isslLoginService.login(loginId, password)) {
            // 로그인 성공 여부만 확인하고 세션은 바로 닫는다 (대출이력 조회는 /api/loans/sync에서 별도 수행).
        } catch (IsslAuthException e) {
            log.warn("issl.go.kr 로그인 검증 실패 (loginId={}): {}", loginId, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            // Playwright 타임아웃/네트워크 오류 등 IsslAuthException이 아닌 예외는 그냥 두면 500으로 새어나가
            // 프론트에 아무 정보 없는 일반 에러만 보인다 — 원인을 로그에 남기고 같은 방식으로 감싼다.
            log.error("issl.go.kr 로그인 시도 중 예상 못한 오류 (loginId={})", loginId, e);
            throw new IsslAuthException(
                "도서관 계정 로그인 확인 중 오류가 발생했습니다(" + e.getClass().getSimpleName() + "). 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * Spring Boot 기본 에러 응답은 body에 message 필드를 안정적으로 안 실어줘서(버전/설정에 따라 달라짐),
     * ResponseStatusException에 기대지 않고 프론트가 확실히 읽을 수 있는 형태로 직접 응답을 만든다.
     */
    @ExceptionHandler(IsslAuthException.class)
    public ResponseEntity<Map<String, String>> handleIsslAuthException(IsslAuthException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("message", e.getMessage()));
    }

    private FamilyMember ownedMember(Long ownerUserId, Long memberId) {
        FamilyMember member = familyMemberRepository.findById(memberId).orElseThrow(NoSuchElementException::new);
        if (!member.getOwnerUserId().equals(ownerUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return member;
    }

    private LibraryAccount ownedLibraryAccount(Long ownerUserId, Long accountId) {
        LibraryAccount account = libraryAccountRepository.findById(accountId).orElseThrow(NoSuchElementException::new);
        if (!account.getFamilyMember().getOwnerUserId().equals(ownerUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return account;
    }

    private MemberResponse toResponse(FamilyMember member) {
        List<LibraryAccountResponse> accounts = member.getLibraryAccounts().stream()
            .map(a -> new LibraryAccountResponse(a.getId(), a.getLoginId()))
            .toList();
        return new MemberResponse(member.getId(), member.getName(), member.isSelf(), accounts);
    }
}
