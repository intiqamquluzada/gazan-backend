package az.qazan.backend.business.application;

import az.qazan.backend.auth.domain.RefreshTokenRepository;
import az.qazan.backend.business.api.dto.CustomerLookupResponse;
import az.qazan.backend.business.api.dto.StaffRequest;
import az.qazan.backend.business.api.dto.StaffResponse;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.companies.application.BusinessAccess;
import az.qazan.backend.companies.application.CompanyService;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.user.application.PhoneNumbers;
import az.qazan.backend.user.application.UserService;
import az.qazan.backend.user.domain.AppLocale;
import az.qazan.backend.user.domain.Role;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Cashier accounts of one business, and finding a customer at the till. */
@Service
@RequiredArgsConstructor
public class BusinessStaffService {

    private final CompanyService companies;
    private final BusinessAccess access;
    private final UserService userService;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;

    @Transactional(readOnly = true)
    public List<StaffResponse> list(UUID ownerId) {
        return users.findAllByStaffCompanyIdOrderByCreatedAtDesc(
                        companies.myCompany(ownerId).getId())
                .stream().map(StaffResponse::from).toList();
    }

    @Transactional
    public StaffResponse create(UUID ownerId, StaffRequest req) {
        Company company = companies.myCompany(ownerId);
        User cashier = userService.register(
                req.email(), req.password(), req.fullName(), req.phone(),
                null, Role.CASHIER, AppLocale.AZ);
        cashier.setStaffCompanyId(company.getId());
        return StaffResponse.from(cashier);
    }

    /** Stops access at once and keeps the account for the history it left. */
    @Transactional
    public void deactivate(UUID ownerId, UUID staffId) {
        UUID companyId = companies.myCompany(ownerId).getId();
        User cashier = users.findById(staffId)
                .filter(u -> u.getRole() == Role.CASHIER)
                .filter(u -> companyId.equals(u.getStaffCompanyId()))
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        cashier.setActive(false);
        refreshTokens.revokeAllForUser(cashier, Instant.now());
    }

    @Transactional(readOnly = true)
    public CustomerLookupResponse lookup(UUID callerId, String rawPhone) {
        access.companyOf(callerId);
        String phone = PhoneNumbers.normalize(rawPhone);
        if (phone == null) {
            throw new NotFoundException(ErrorCode.NOT_FOUND);
        }
        return users.findFirstByPhoneOrderByCreatedAtAsc(phone)
                .filter(u -> u.isActive() && u.getRole() == Role.CUSTOMER)
                .map(u -> new CustomerLookupResponse(u.getId(), u.getFullName(), u.getPhone()))
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
    }
}
