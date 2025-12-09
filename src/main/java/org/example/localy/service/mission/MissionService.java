package org.example.localy.service.mission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.MissionErrorCode;
import org.example.localy.constant.EmotionConstants;
import org.example.localy.dto.mission.MissionDto;
import org.example.localy.dto.place.RecommendDto;
import org.example.localy.entity.Users;
import org.example.localy.entity.place.Mission;
import org.example.localy.entity.place.Place;
import org.example.localy.entity.place.PlaceImage;
import org.example.localy.repository.UserRepository;
import org.example.localy.repository.place.MissionRepository;
import org.example.localy.repository.place.PlaceImageRepository;
import org.example.localy.service.Chat.GPTService;
import org.example.localy.util.DistanceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.localy.repository.place.PlaceRepository;
import org.example.localy.service.place.EmotionDataService;
import org.example.localy.service.place.PlaceRecommendService;

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

    private static final double VERIFICATION_RADIUS_KM = 0.05; // 50m
    private static final long NEW_TAG_HOURS = 48; // 48시간 이내 생성된 미션
    private static final int DEFAULT_MISSION_POINTS = 10;
    private static final int MAX_MISSIONS_PER_REQUEST = 2;
    private static final long ACTIVE_MISSION_HOURS = 24; // 활성 미션 기준: 24시간

    @Transactional
    public List<RecommendDto.MissionItem> createMissionsForRecommendedPlaces(
            Users user, List<Place> recommendedPlaces, String emotionKeyword) {

        log.info("미션 생성 시작: userId={}, emotionKeyword={}, places={}",
                user.getId(), emotionKeyword, recommendedPlaces.size());

        LocalDateTime now = LocalDateTime.now();
        List<Mission> activeMissions = missionRepository.findActiveByUser(user, now);

        List<Mission> newMissions = new java.util.ArrayList<>();

        for (Place place : recommendedPlaces) {
            if (newMissions.size() >= MAX_MISSIONS_PER_REQUEST) {
                break;
            }

            // 동일한 감정 + 장소 조합의 미완료 미션이 있는지 확인
            boolean alreadyHasMission = activeMissions.stream()
                    .anyMatch(m -> m.getPlace().getId().equals(place.getId())
                            && m.getEmotion().equalsIgnoreCase(emotionKeyword)
                            && !m.getIsCompleted());

            if (alreadyHasMission) {
                log.info("동일 감정+장소 미션이 이미 존재하여 건너뜁니다: placeId={}, emotion={}",
                        place.getId(), emotionKeyword);
                continue;
            }

            GPTService.MissionCreationResult missionContent =
                    gptService.createMissionContent(
                            place.getTitle(), place.getCategory(), emotionKeyword);

            Mission newMission = Mission.builder()
                    .user(user)
                    .place(place)
                    .title(missionContent.getTitle())
                    .description(missionContent.getDescription())
                    .points(DEFAULT_MISSION_POINTS)
                    .emotion(emotionKeyword)
                    .isCompleted(false)
                    .createdAt(now)
                    .expiresAt(now.plusHours(24))
                    .build();

            newMissions.add(newMission);
        }

        missionRepository.saveAll(newMissions);
        log.info("미션 생성 완료. 총 {}개의 미션이 새로 생성되었습니다.", newMissions.size());

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

    // 미션 홈 조회 - 위치 정보 없이 호출 (오버로딩)
    @Transactional
    public MissionDto.MissionHomeResponse getMissionHome(Users user) {
        return getMissionHome(user, null, null);
    }

    // 미션 홈 조회 - 여기서 미션 생성 로직 호출
    @Transactional
    public MissionDto.MissionHomeResponse getMissionHome(Users user, Double userLat, Double userLon) {
        LocalDateTime now = LocalDateTime.now();

        // 위치 정보가 있으면 미션 생성/누적 로직 실행
        if (userLat != null && userLon != null) {
            try {
                processMissionGenerationAndAccumulation(user, userLat, userLon);
            } catch (Exception e) {
                log.error("미션 생성 중 오류 발생: userId={}", user.getId(), e);
                // 오류가 발생해도 기존 미션 목록은 반환
            }
        } else {
            log.warn("미션 홈 조회 시 위치 정보가 누락되어 미션 생성을 건너뜁니다.");
        }

        // 포인트 정보
        MissionDto.PointInfo pointInfo = MissionDto.PointInfo.builder()
                .totalPoints(user.getPoints())
                .availablePoints(user.getPoints())
                .assignedMissions((int) missionRepository.countActiveByUser(user, now))
                .build();

        // 참여 가능한 미션 (만료되지 않고, 완료하지 않은 미션)
        List<Mission> activeMissions = missionRepository.findActiveByUser(user, now);
        List<MissionDto.MissionItem> availableMissions = activeMissions.stream()
                .filter(m -> !m.getIsCompleted())
                .map(m -> convertToMissionItem(m, now))
                .collect(Collectors.toList());

        // 참여 완료한 미션
        List<MissionDto.MissionItem> completedMissions = activeMissions.stream()
                .filter(Mission::getIsCompleted)
                .map(m -> convertToMissionItem(m, now))
                .collect(Collectors.toList());

        return MissionDto.MissionHomeResponse.builder()
                .pointInfo(pointInfo)
                .availableMissions(availableMissions)
                .completedMissions(completedMissions)
                .build();
    }

    // 미션 상세 조회 - 여기서는 미션 생성 로직 제거
    @Transactional(readOnly = true)
    public MissionDto.MissionDetailResponse getMissionDetail(
            Users user, Long missionId, Double userLat, Double userLon) {

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(MissionErrorCode.MISSION_NOT_FOUND));

        if (!mission.getUser().getId().equals(user.getId())) {
            throw new CustomException(MissionErrorCode.MISSION_NOT_OWNER);
        }

        Place place = mission.getPlace();

        Double distance = null;
        Boolean canVerify = false;

        if (userLat != null && userLon != null) {
            distance = DistanceCalculator.calculateDistance(
                    userLat, userLon, place.getLatitude(), place.getLongitude());
            canVerify = distance <= VERIFICATION_RADIUS_KM && !mission.getIsCompleted() && !mission.isExpired();
        }

        List<PlaceImage> images = placeImageRepository.findByPlaceOrderByDisplayOrder(place);
        List<String> imageUrls = images.stream()
                .limit(3)
                .map(PlaceImage::getImageUrl)
                .collect(Collectors.toList());

        String kakaoMapUrl = generateKakaoMapUrl(place);

        MissionDto.PlaceInfo placeInfo = MissionDto.PlaceInfo.builder()
                .placeId(place.getId())
                .placeName(place.getTitle())
                .category(place.getCategory())
                .address(place.getAddress())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .openingHours(place.getOpeningHours())
                .shortDescription(place.getShortDescription())
                .images(imageUrls.isEmpty() ? List.of(place.getThumbnailImage()) : imageUrls)
                .kakaoMapUrl(kakaoMapUrl)
                .build();

        return MissionDto.MissionDetailResponse.builder()
                .missionId(mission.getId())
                .missionTitle(mission.getTitle())
                .missionDescription(mission.getDescription())
                .expiresAt(mission.getExpiresAt())
                .points(mission.getPoints())
                .placeInfo(placeInfo)
                .canVerify(canVerify)
                .distance(distance != null ? DistanceCalculator.roundDistance(distance) : null)
                .build();
    }

    @Transactional
    public void processMissionGenerationAndAccumulation(Users user, Double userLat, Double userLon) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 현재 활성 미션 조회 (생성 후 24시간 이내, 미완료)
        List<Mission> activeMissions = missionRepository.findActiveByUser(user, now).stream()
                .filter(m -> !m.getIsCompleted())
                .filter(m -> ChronoUnit.HOURS.between(m.getCreatedAt(), now) < ACTIVE_MISSION_HOURS)
                .collect(Collectors.toList());

        // 2. 현재 감정 데이터 조회
        RecommendDto.EmotionData currentEmotionData = emotionDataService.getCurrentEmotion(user);
        String currentDominantEmotionKeyword = currentEmotionData.getDominantEmotion();

        log.info("미션 생성 체크: userId={}, 활성미션수={}, 현재감정={}",
                user.getId(), activeMissions.size(), currentDominantEmotionKeyword);

        // 3. 미션 생성 조건 판단
        boolean shouldCreateMission = false;

        if (activeMissions.isEmpty()) {
            // 활성 미션이 없으면 무조건 생성
            log.info("활성 미션이 없어 새 미션을 생성합니다.");
            shouldCreateMission = true;
        } else {
            // 활성 미션이 있으면 감정 변화 체크
            String existingMissionEmotionKeyword = activeMissions.get(0).getEmotion();

            if (!existingMissionEmotionKeyword.equalsIgnoreCase(currentDominantEmotionKeyword)) {
                log.info("감정 변화 감지: 기존={}, 현재={} -> 누적 생성",
                        existingMissionEmotionKeyword, currentDominantEmotionKeyword);
                shouldCreateMission = true;
            } else {
                log.info("감정 변화 없음. 기존 미션 유지. emotion={}", existingMissionEmotionKeyword);
                shouldCreateMission = false;
            }
        }

        // 4. 미션 생성 실행
        if (shouldCreateMission) {
            try {
                RecommendDto.RecommendResponse recommendation =
                        recommendService.recommendPlaces(user, userLat, userLon);

                List<Place> recommendedPlaceEntities = recommendation.getRecommendedPlaces().stream()
                        .map(rec -> placeRepository.findById(rec.getPlaceId()).orElse(null))
                        .filter(Objects::nonNull)
                        .toList();

                if (!recommendedPlaceEntities.isEmpty()) {
                    createMissionsForRecommendedPlaces(
                            user, recommendedPlaceEntities, currentDominantEmotionKeyword
                    );
                } else {
                    log.warn("추천 장소가 없어 미션 생성을 건너뜁니다.");
                }
            } catch (Exception e) {
                log.error("미션 생성 중 오류 발생: userId={}", user.getId(), e);
                throw e;
            }
        }
    }

    // 미션 인증
    @Transactional
    public MissionDto.VerifyResponse verifyMission(
            Users user, Long missionId, MissionDto.VerifyRequest request) {

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(MissionErrorCode.MISSION_NOT_FOUND));

        if (!mission.getUser().getId().equals(user.getId())) {
            throw new CustomException(MissionErrorCode.MISSION_NOT_OWNER);
        }

        if (mission.getIsCompleted()) {
            throw new CustomException(MissionErrorCode.MISSION_ALREADY_COMPLETED);
        }

        if (mission.isExpired()) {
            throw new CustomException(MissionErrorCode.MISSION_EXPIRED);
        }

        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new CustomException(MissionErrorCode.LOCATION_UNAVAILABLE);
        }

        Place place = mission.getPlace();
        double distance = DistanceCalculator.calculateDistance(
                request.getLatitude(), request.getLongitude(),
                place.getLatitude(), place.getLongitude());

        if (distance > VERIFICATION_RADIUS_KM) {
            throw new CustomException(MissionErrorCode.LOCATION_TOO_FAR);
        }

        mission.complete();
        missionRepository.save(mission);

        user.addPoints(mission.getPoints());
        userRepository.save(user);

        log.info("미션 인증 완료: userId={}, missionId={}, points={}",
                user.getId(), mission.getId(), mission.getPoints());

        return MissionDto.VerifyResponse.builder()
                .success(true)
                .missionTitle(mission.getTitle())
                .earnedPoints(mission.getPoints())
                .totalPoints(user.getPoints())
                .build();
    }

    private MissionDto.MissionItem convertToMissionItem(Mission mission, LocalDateTime now) {
        long hoursSinceCreated = ChronoUnit.HOURS.between(mission.getCreatedAt(), now);
        boolean isNew = hoursSinceCreated <= NEW_TAG_HOURS && !mission.getIsCompleted();

        return MissionDto.MissionItem.builder()
                .missionId(mission.getId())
                .missionTitle(mission.getTitle())
                .expiresAt(mission.getExpiresAt())
                .points(mission.getPoints())
                .isCompleted(mission.getIsCompleted())
                .isNew(isNew)
                .emotion(mission.getEmotion())
                .build();
    }

    private String generateKakaoMapUrl(Place place) {
        return String.format("https://map.kakao.com/link/map/%s,%s,%s",
                place.getTitle(),
                place.getLatitude(),
                place.getLongitude());
    }
}