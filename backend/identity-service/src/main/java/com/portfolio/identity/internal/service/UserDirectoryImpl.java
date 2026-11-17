package com.portfolio.identity.internal.service;

import com.portfolio.identity.api.UserDirectory;
import com.portfolio.identity.internal.domain.UserAccount;
import com.portfolio.identity.internal.domain.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class UserDirectoryImpl implements UserDirectory {

    private final UserAccountRepository users;

    UserDirectoryImpl(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSummary> find(Long userId) {
        return users.findById(userId).map(UserDirectoryImpl::summary);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, UserSummary> findAll(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return users.findAllById(userIds).stream()
                .map(UserDirectoryImpl::summary)
                .collect(Collectors.toMap(s -> s.id(), Function.identity()));
    }

    private static UserSummary summary(UserAccount u) {
        return new UserSummary(u.getId(), u.getDisplayName(), u.getAvatarUrl());
    }
}
