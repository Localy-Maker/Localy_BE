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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
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
    private final ExecutorService externalApiExecutor;

    private static final double VERIFICATION_RADIUS_KM = 0.05; // 50m
    private static final long NEW_TAG_HOURS = 48; // 48시간 이내 생성된 미션
    private static final int DEFAULT_MISSION_POINTS = 10;
    private static final int MAX_MISSIONS_PER_REQUEST = 2;
    private static final int PREMIUM_MISSION_POINTS = 30;
    private static final int PREMIUM_MAX_MISSIONS = 3;
    private static final long ACTIVE_MISSION_HOURS = 24; // 활성 미션 기준: 24시간

    @Transactional
    public List<RecommendDto.MissionItem> createMissionsForRecommendedPlaces(
            Users user, List<Place> recommendedPlaces, String emotionKeyword) {

        log.info("미션 생성 시작: userId={}, emotionKeyword={}, places={}",
                user.getId(), emotionKeyword, recommendedPlaces.size());

        LocalDateTime now = LocalDateTime.now();

        int maxMissions = user.isPremium() ? PREMIUM_MAX_MISSIONS : MAX_MISSIONS_PER_REQUEST;
        int missionPoints = user.isPremium() ? PREMIUM_MISSION_POINTS : DEFAULT_MISSION_POINTS;

        // 프리미엄용 프롬프트 (장소와 무관하게 동일하므로 루프 밖에서 한 번만 계산)
        final String promptKeyword = user.isPremium()
                ? emotionKeyword + " (Premium Grade: 사용자가 더 높은 성취감을 느낄 수 있도록 난이도가 높은 도전적인 미션을 1개 제안해줘. 보상은 30포인트 가치)"
                : emotionKeyword;

        // 활성 미션이 없을 때만 호출된다고 가정하므로, 여기서는 후보 목록 내에서
        // 서로 다른 장소를 최대 maxMissions개(장소당 미션 1개) 골라내기만 하면 된다.
        List<Place> eligiblePlaces = new ArrayList<>();
        Set<Long> usedPlaceIds = new HashSet<>();
        for (Place place : recommendedPlaces) {
            if (eligiblePlaces.size() >= maxMissions) {
                break;
            }
            if (!usedPlaceIds.add(place.getId())) {
                continue; // 이미 후보로 선택된 장소는 중복 제외
            }

            eligiblePlaces.add(place);
        }

        // 장소별 GPT 미션 문구 생성은 서로 독립적인 호출이라 병렬로 처리
        List<CompletableFuture<Mission>> missionFutures = eligiblePlaces.stream()
                .map(place -> CompletableFuture.supplyAsync(() -> {
                    try {
                        GPTService.MissionCreationResult missionContent =
                                gptService.createMissionContent(place.getTitle(), place.getCategory(), promptKeyword);

                        return Mission.builder()
                                .user(user)
                                .place(place)
                                .title(missionContent.getTitle())
                                .description(missionContent.getDescription())
                                .points(missionPoints)
                                .emotion(emotionKeyword)
                                .isCompleted(false)
                                .createdAt(now)
                                .expiresAt(now.plusHours(ACTIVE_MISSION_HOURS))
                                .build();
                    } catch (Exception e) {
                        // 장소 하나의 GPT 호출 실패로 나머지 장소의 미션 생성까지 다 날아가지 않도록 격리
                        log.error("미션 문구 생성 실패: placeId={}", place.getId(), e);
                        return null;
                    }
                }, externalApiExecutor))
                .collect(Collectors.toList());

        List<Mission> newMissions = missionFutures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

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

        // 활성 미션이 하나도 없을 때만 새로 생성한다 (감정이 바뀌어도 기존 활성 미션이 있으면 그대로 둔다)
        if (!activeMissions.isEmpty()) {
            return;
        }

        String currentEmotion = emotionDataService.getCurrentEmotion(user).getDominantEmotion();
        RecommendDto.RecommendResponse recommendation = recommendService.recommendPlaces(user, userLat, userLon);
        List<Place> places = recommendation.getRecommendedPlaces().stream()
                .map(rec -> placeRepository.findById(rec.getPlaceId()).orElse(null))
                .filter(Objects::nonNull).toList();

        if (!places.isEmpty()) {
            createMissionsForRecommendedPlaces(user, places, currentEmotion);
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

    @Transactional
    public void generateMissionAtDetailPage(Users user, Place place, String emotionKeyword) {
        LocalDateTime now = LocalDateTime.now();

        // 활성 미션이 하나라도 있으면 새로 생성하지 않고 기존 미션을 유지한다
        List<Mission> activeMissions = missionRepository.findActiveByUser(user, now);
        if (!activeMissions.isEmpty()) {
            log.info("이미 활성 미션이 있어 새로 생성하지 않습니다: userId={}", user.getId());
            return;
        }

        // 지금 보고 있는 장소를 첫 후보로 하고, 그 장소 주변의 다른 장소들로 나머지를 채운다
        // (장소당 미션 1개, 서로 다른 장소끼리 생성되도록 createMissionsForRecommendedPlaces에 위임)
        List<Place> candidatePlaces = new ArrayList<>();
        candidatePlaces.add(place);

        if (place.getLatitude() != null && place.getLongitude() != null) {
            try {
                RecommendDto.RecommendResponse recommendation =
                        recommendService.recommendPlaces(user, place.getLatitude(), place.getLongitude());
                recommendation.getRecommendedPlaces().stream()
                        .map(rec -> placeRepository.findById(rec.getPlaceId()).orElse(null))
                        .filter(Objects::nonNull)
                        .filter(p -> !p.getId().equals(place.getId()))
                        .forEach(candidatePlaces::add);
            } catch (Exception e) {
                log.warn("상세페이지 미션 생성 중 주변 장소 추천 실패, 현재 장소만으로 진행합니다: placeId={}", place.getId(), e);
            }
        }

        createMissionsForRecommendedPlaces(user, candidatePlaces, emotionKeyword);
    }
}