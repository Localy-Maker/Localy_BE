package org.example.localy.service.premium;

import lombok.RequiredArgsConstructor;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.AuthErrorCode;
import org.example.localy.common.exception.errorCode.PremiumErrorCode;
import org.example.localy.dto.premium.PremiumDto;
import org.example.localy.entity.Users;
import org.example.localy.entity.premium.PremiumPlan;
import org.example.localy.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PremiumService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PremiumDto.PlansResponse getPlans(Users user) {
        PremiumDto.CurrentSubscriptionDto subscription = null;
        if (user.isPremium()) {
            subscription = PremiumDto.CurrentSubscriptionDto.builder()
                    .expiresAt(user.getPremiumExpiresAt())
                    .remainingDays(calcRemainingDays(user.getPremiumExpiresAt()))
                    .build();
        }

        List<PremiumDto.PlanItem> plans = Arrays.stream(PremiumPlan.values())
                .map(plan -> PremiumDto.PlanItem.builder()
                        .code(plan.getCode())
                        .name(plan.getDisplayName())
                        .durationDays(plan.getDurationDays())
                        .price(plan.getPrice())
                        .weeklyPrice(calcWeeklyPrice(plan))
                        .build())
                .collect(Collectors.toList());

        return PremiumDto.PlansResponse.builder()
                .currentPoint(user.getPoints())
                .isPremium(user.isPremium())
                .currentSubscription(subscription)
                .plans(plans)
                .build();
    }

    @Transactional(readOnly = true)
    public PremiumDto.StatusResponse getStatus(Users user) {
        PremiumDto.CurrentSubscriptionDto subscription = null;
        if (user.isPremium()) {
            subscription = PremiumDto.CurrentSubscriptionDto.builder()
                    .expiresAt(user.getPremiumExpiresAt())
                    .remainingDays(calcRemainingDays(user.getPremiumExpiresAt()))
                    .build();
        }

        return PremiumDto.StatusResponse.builder()
                .isPremium(user.isPremium())
                .currentSubscription(subscription)
                .build();
    }

    @Transactional
    public PremiumDto.SubscribeResponse subscribe(Users user, String planCode) {
        PremiumPlan plan = PremiumPlan.fromCode(planCode);

        Users lockedUser = userRepository.findByIdWithLock(user.getId())
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        if (lockedUser.getPoints() < plan.getPrice()) {
            throw new CustomException(PremiumErrorCode.INSUFFICIENT_POINTS);
        }

        lockedUser.deductPoints(plan.getPrice());
        lockedUser.extendPremium(plan.getDurationDays());
        userRepository.save(lockedUser);

        return PremiumDto.SubscribeResponse.builder()
                .currentPoint(lockedUser.getPoints())
                .expiresAt(lockedUser.getPremiumExpiresAt())
                .remainingDays(calcRemainingDays(lockedUser.getPremiumExpiresAt()))
                .build();
    }

    private int calcRemainingDays(LocalDateTime expiresAt) {
        return (int) ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt);
    }

    private Integer calcWeeklyPrice(PremiumPlan plan) {
        if (plan.getDurationDays() <= 7) return null;
        return (int) ((double) plan.getPrice() / plan.getDurationDays() * 7);
    }
}
