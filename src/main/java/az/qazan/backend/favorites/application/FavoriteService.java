package az.qazan.backend.favorites.application;

import az.qazan.backend.favorites.domain.Favorite;
import az.qazan.backend.favorites.domain.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository repository;

    @Transactional(readOnly = true)
    public List<UUID> listCompanyIds(UUID userId) {
        return repository.findCompanyIdsByUserId(userId);
    }

    @Transactional
    public void add(UUID userId, UUID companyId) {
        if (!repository.existsByUserIdAndCompanyId(userId, companyId)) {
            repository.save(Favorite.builder()
                    .userId(userId)
                    .companyId(companyId)
                    .build());
        }
    }

    @Transactional
    public void remove(UUID userId, UUID companyId) {
        repository.deleteByUserIdAndCompanyId(userId, companyId);
    }
}
