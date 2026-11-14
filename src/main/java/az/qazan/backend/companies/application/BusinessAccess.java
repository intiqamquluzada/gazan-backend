package az.qazan.backend.companies.application;

import az.qazan.backend.common.exception.AppException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import az.qazan.backend.user.domain.Role;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Answers "which business does this person work for?" for owners and cashiers. */
@Service
@RequiredArgsConstructor
public class BusinessAccess {

    private final CompanyRepository companies;
    private final UserRepository users;

    /** The owner's company, or the company a cashier is attached to. */
    @Transactional(readOnly = true)
    public Company companyOf(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() == Role.CASHIER) {
            if (user.getStaffCompanyId() == null) {
                throw new AppException(ErrorCode.FORBIDDEN);
            }
            return companies.findById(user.getStaffCompanyId())
                    .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        }
        return companies.findByOwnerId(userId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public boolean isStaff(Company company, UUID userId) {
        if (company.getOwner() != null && company.getOwner().getId().equals(userId)) {
            return true;
        }
        return users.findById(userId)
                .filter(u -> u.getRole() == Role.CASHIER && u.isActive())
                .filter(u -> company.getId().equals(u.getStaffCompanyId()))
                .isPresent();
    }

    /** Owner or cashier of this company; anyone else gets 403. */
    public void requireStaff(Company company, UUID userId) {
        if (!isStaff(company, userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }
}
