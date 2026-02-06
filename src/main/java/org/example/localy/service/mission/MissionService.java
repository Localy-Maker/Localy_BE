package org.example.localy.service.mission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.MissionErrorCode;
import org.example.localy.dto.mission.MissionArchiveDto;
import org.example.localy.dto.mission.MissionDto;
import org.example.localy.dto.place.RecommendDto;
import org.example.localy.entity.Users;
import org.example.localy.entity.place.Mission;
import org.example.localy.entity.place.MissionArchive;
import org.example.localy.entity.place.Place;
import org.example.localy.entity.place.PlaceImage;
import org.example.localy.repository.UserRepository;
import org.example.localy.repository.place.MissionArchiveRepository;
import org.example.localy.repository.place.MissionRepository;
import org.example.localy.repository.place.PlaceImageRepository;
import org.example.localy.service.Chat.GPTService;
import org.example.localy.util.DistanceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.localy.repository.place.PlaceRepository;
import org.example.localy.service.place.EmotionDataService;
import org.example.localy.service.place.PlaceRecommendService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MissionService {

    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final PlaceImageRepository placeImageRepository;
    private final GPTService gptService;
    private final PlaceRecommendService recommendService;
    private final EmotionDataService emotionDataService;
    private final PlaceRepository placeRepository;
    private final MissionArchiveRepository missionArchiveRepository; // 신규 추가

    private static final double VERIFICATION_RADIUS_KM = 0.05;
    private static final long NEW_TAG_HOURS = 48;
    private static final int BASIC_MISSION_POINTS = 10;
    private static final int PREMIUM_MISSION_POINTS = 30; // 프리미엄 차등 포인트
    private static final int BASIC_MAX_MISSIONS = 2;
    private static final int PREMIUM_MAX_MISSIONS = 3; // 프리미엄 최대 3개
    private static final long ACTIVE_MISSION_HOURS = 24;

    @Transactional
    public List<RecommendDto.MissionItem> createMissionsForRecommendedPlaces(
            Users user, List<Place> recommendedPlaces, String emotionKeyword) {

        log.info("미션 생성 시작: userId={}, emotionKeyword={}, places={}",
                user.getId(), emotionKeyword, recommendedPlaces.size());

        LocalDateTime now = LocalDateTime.now();
        List<Mission> activeMissions = missionRepository.findActiveByUser(user, now);

        // 등급별 제한 및 포인트 설정
        int maxLimit = user.isPremium() ? PREMIUM_MAX_MISSIONS : BASIC_MAX_MISSIONS;
        int points = user.isPremium() ? PREMIUM_MISSION_POINTS : BASIC_MISSION_POINTS;

        List<Mission> newMissions = new java.util.ArrayList<>();

        for (Place place : recommendedPlaces) {
            if (newMissions.size() >= maxLimit) {
                break;
            }

            boolean alreadyHasMission = activeMissions.stream()
                    .anyMatch(m -> m.getPlace().getId().equals(place.getId())
                            && m.getEmotion().equalsIgnoreCase(emotionKeyword)
                            && !m.getIsCompleted());

            if (alreadyHasMission) continue;

            // 프리미엄용
            String promptKeyword = emotionKeyword;
            if (user.isPremium()) {
                promptKeyword += " (Premium Grade: 사용자가 더 높은 성취감을 느낄 수 있도록 난이도가 높은 도전적인 미션을 1개 제안해줘. 보상은 30포인트 가치)";
            }

            GPTService.MissionCreationResult missionContent =
                    gptService.createMissionContent(place.getTitle(), place.getCategory(), promptKeyword);

            Mission newMission = Mission.builder()
                    .user(user)
                    .place(place)
                    .title(missionContent.getTitle())
                    .description(missionContent.getDescription())
                    .points(points)
                    .emotion(emotionKeyword)
                    .isCompleted(false)
                    .createdAt(now)
                    .expiresAt(now.plusHours(ACTIVE_MISSION_HOURS))
                    .build();

            newMissions.add(newMission);
        }

        missionRepository.saveAll(newMissions);
        return newMissions.stream()
                .map(m -> RecommendDto.MissionItem.builder()
                        .placeId(m.getPlace().getId())
                        .missionTitle(m.getTitle())
                        .missionDescription(m.getDescription())
                        .points(m.getPoints())
                        .expiresAt(m.getExpiresAt())
                        .build())
                .collect(Collectors.toList());
    }

    // 프리미엄 구독
    @Transactional
    public void purchasePremium(Users user, String planType) {
        int cost = planType.equals("7DAYS") ? 50 : 200;
        int days = planType.equals("7DAYS") ? 7 : 30;

        if (user.getPoints() < cost) {
            throw new CustomException(MissionErrorCode.POINT_DEDUCTION_FAILED);
        }

        user.deductPoints(cost);
        user.setMembershipLevel(Users.MembershipLevel.PREMIUM);

        // 기존 만료일이 현재보다 미래라면 그 날짜부터 연장, 아니면 현재부터 설정
        LocalDateTime baseDate = user.isPremium() ? user.getPremiumExpiryDate() : LocalDateTime.now();
        user.setPremiumExpiryDate(baseDate.plusDays(days));

        userRepository.save(user);
    }

    //캘린더 아카이빙 (날짜 검증 및 저장)
    @Transactional
    public MissionArchive archiveMission(Users user, Long missionId, String imageUrl, LocalDate targetDate, LocalDate photoStoredDate) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(MissionErrorCode.MISSION_NOT_FOUND));

        if (!mission.getUser().getId().equals(user.getId())) {
            throw new CustomException(MissionErrorCode.MISSION_NOT_OWNER);
        }

        if (!targetDate.equals(photoStoredDate)) {
            throw new CustomException(MissionErrorCode.DATE_MISMATCH);
        }

        LocalDate missionStartDate = mission.getCreatedAt().toLocalDate();
        LocalDate missionEndDate = mission.getExpiresAt().toLocalDate();

        if (photoStoredDate.isBefore(missionStartDate) || photoStoredDate.isAfter(missionEndDate)) {
            throw new CustomException(MissionErrorCode.DATE_OUT_OF_RANGE);
        }

        MissionArchive archive = MissionArchive.builder()
                .user(user)
                .imageUrl(imageUrl)
                .archivedDate(targetDate)
                .build();

        return missionArchiveRepository.save(archive);
    }

    @Transactional(readOnly = true)
    public List<MissionArchive> getMonthlyArchives(Users user, int year, int month) {
        // 특정 월의 시작일과 종료일 계산
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return missionArchiveRepository.findByUserAndArchivedDateBetween(user, start, end);
    }

    @Transactional
    public MissionDto.MissionHomeResponse getMissionHome(Users user, Double userLat, Double userLon) {
        LocalDateTime now = LocalDateTime.now();

        if (userLat != null && userLon != null) {
            try {
                processMissionGenerationAndAccumulation(user, userLat, userLon);
            } catch (Exception e) {
                log.error("미션 생성 중 오류 발생: userId={}", user.getId(), e);
            }
        }

        List<Mission> activeMissions = missionRepository.findActiveByUser(user, now);

        return MissionDto.MissionHomeResponse.builder()
                .pointInfo(MissionDto.PointInfo.builder()
                        .totalPoints(user.getPoints())
                        .availablePoints(user.getPoints())
                        .assignedMissions((int) missionRepository.countActiveByUser(user, now))
                        .build())
                .availableMissions(activeMissions.stream()
                        .filter(m -> !m.getIsCompleted())
                        .map(m -> convertToMissionItem(m, now))
                        .collect(Collectors.toList()))
                .completedMissions(activeMissions.stream()
                        .filter(Mission::getIsCompleted)
                        .map(m -> convertToMissionItem(m, now))
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public MissionArchiveDto.MonthlySummaryResponse getMonthlySummary(Users user, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // 해당 월의 모든 아카이브 데이터를 가져옴
        List<MissionArchive> monthlyArchives = missionArchiveRepository.findByUserAndArchivedDateBetween(user, start, end);

        // 날짜별로 그룹화하여 썸ne일(isThumbnail = true)인 사진만 추출하여 요약 생성
        List<MissionArchiveDto.ArchiveSummary> summaryList = monthlyArchives.stream()
                .filter(MissionArchive::getIsThumbnail)
                .map(a -> MissionArchiveDto.ArchiveSummary.builder()
                        .date(a.getArchivedDate())
                        .thumbnailImageUrl(a.getImageUrl())
                        .hasPhoto(true)
                        .build())
                .collect(Collectors.toList());

        return MissionArchiveDto.MonthlySummaryResponse.builder()
                .userId(user.getId())
                .year(year)
                .month(month)
                .archives(summaryList)
                .build();
    }

    @Transactional
    public MissionDto.MissionHomeResponse getMissionHome(Users user) {
        return getMissionHome(user, null, null);
    }

    @Transactional(readOnly = true)
    public MissionDto.MissionDetailResponse getMissionDetail(
            Users user, Long missionId, Double userLat, Double userLon) {

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(MissionErrorCode.MISSION_NOT_FOUND));

        if (!mission.getUser().getId().equals(user.getId())) {
            throw new CustomException(MissionErrorCode.MISSION_NOT_OWNER);
        }

        Place place = mission.getPlace();
        Double distance = (userLat != null && userLon != null) ?
                DistanceCalculator.calculateDistance(userLat, userLon, place.getLatitude(), place.getLongitude()) : null;

        boolean canVerify = distance != null && distance <= VERIFICATION_RADIUS_KM && !mission.getIsCompleted() && !mission.isExpired();

        List<String> imageUrls = placeImageRepository.findByPlaceOrderByDisplayOrder(place).stream()
                .limit(3).map(PlaceImage::getImageUrl).collect(Collectors.toList());

        return MissionDto.MissionDetailResponse.builder()
                .missionId(mission.getId())
                .missionTitle(mission.getTitle())
                .missionDescription(mission.getDescription())
                .expiresAt(mission.getExpiresAt())
                .points(mission.getPoints())
                .placeInfo(MissionDto.PlaceInfo.builder()
                        .placeId(place.getId())
                        .placeName(place.getTitle())
                        .category(place.getCategory())
                        .address(place.getAddress())
                        .latitude(place.getLatitude())
                        .longitude(place.getLongitude())
                        .openingHours(place.getOpeningHours())
                        .shortDescription(place.getShortDescription())
                        .images(imageUrls.isEmpty() ? List.of(place.getThumbnailImage()) : imageUrls)
                        .kakaoMapUrl(generateKakaoMapUrl(place))
                        .build())
                .canVerify(canVerify)
                .distance(distance != null ? DistanceCalculator.roundDistance(distance) : null)
                .build();
    }

    @Transactional
    public void processMissionGenerationAndAccumulation(Users user, Double userLat, Double userLon) {
        LocalDateTime now = LocalDateTime.now();
        List<Mission> activeMissions = missionRepository.findActiveByUser(user, now).stream()
                .filter(m -> !m.getIsCompleted() && ChronoUnit.HOURS.between(m.getCreatedAt(), now) < ACTIVE_MISSION_HOURS)
                .collect(Collectors.toList());

        String currentEmotion = emotionDataService.getCurrentEmotion(user).getDominantEmotion();

        if (activeMissions.isEmpty() || !activeMissions.get(0).getEmotion().equalsIgnoreCase(currentEmotion)) {
            RecommendDto.RecommendResponse recommendation = recommendService.recommendPlaces(user, userLat, userLon);
            List<Place> places = recommendation.getRecommendedPlaces().stream()
                    .map(rec -> placeRepository.findById(rec.getPlaceId()).orElse(null))
                    .filter(Objects::nonNull).toList();

            if (!places.isEmpty()) {
                createMissionsForRecommendedPlaces(user, places, currentEmotion);
            }
        }
    }

    @Transactional
    public MissionDto.VerifyResponse verifyMission(Users user, Long missionId, MissionDto.VerifyRequest request) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(MissionErrorCode.MISSION_NOT_FOUND));

        if (!mission.getUser().getId().equals(user.getId())) throw new CustomException(MissionErrorCode.MISSION_NOT_OWNER);
        if (mission.getIsCompleted()) throw new CustomException(MissionErrorCode.MISSION_ALREADY_COMPLETED);
        if (mission.isExpired()) throw new CustomException(MissionErrorCode.MISSION_EXPIRED);

        double distance = DistanceCalculator.calculateDistance(request.getLatitude(), request.getLongitude(),
                mission.getPlace().getLatitude(), mission.getPlace().getLongitude());

        if (distance > VERIFICATION_RADIUS_KM) throw new CustomException(MissionErrorCode.LOCATION_TOO_FAR);

        mission.complete();
        user.addPoints(mission.getPoints());
        userRepository.save(user);
        missionRepository.save(mission);

        return MissionDto.VerifyResponse.builder()
                .success(true)
                .missionTitle(mission.getTitle())
                .earnedPoints(mission.getPoints())
                .totalPoints(user.getPoints())
                .build();
    }

    private MissionDto.MissionItem convertToMissionItem(Mission mission, LocalDateTime now) {
        return MissionDto.MissionItem.builder()
                .missionId(mission.getId())
                .missionTitle(mission.getTitle())
                .expiresAt(mission.getExpiresAt())
                .points(mission.getPoints())
                .isCompleted(mission.getIsCompleted())
                .isNew(ChronoUnit.HOURS.between(mission.getCreatedAt(), now) <= NEW_TAG_HOURS && !mission.getIsCompleted())
                .emotion(mission.getEmotion())
                .build();
    }

    private String generateKakaoMapUrl(Place place) {
        return String.format("https://map.kakao.com/link/map/%s,%s,%s", place.getTitle(), place.getLatitude(), place.getLongitude());
    }
}