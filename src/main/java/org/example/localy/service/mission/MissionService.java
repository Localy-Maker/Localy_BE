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

    @Transactional
    public List<RecommendDto.MissionItem> createMissionsForRecommendedPlaces(
            Users user, List<Place> recommendedPlaces, String emotionKeyword) {

        log.info("미션 생성 시작: userId={}, emotionKeyword={}, places={}",
                user.getId(), emotionKeyword, recommendedPlaces.size());

        LocalDateTime now = LocalDateTime.now();
        List<Mission> activeMissions = missionRepository.findActiveByUser(user, now); // 중복 체크를 위해 활성 미션 조회

        List<Mission> newMissions = new java.util.ArrayList<>();

        // 1. 추천 장소별 미션 생성 시도
        for (Place place : recommendedPlaces) {

            if (newMissions.size() >= MAX_MISSIONS_PER_REQUEST) {
                break;
            }

            // 이미 이 장소에 대해 활성/미완료 미션이 있는지 확인 (중복 생성 방지)
            boolean alreadyHasMission = activeMissions.stream()
                    .anyMatch(m -> m.getPlace().getId().equals(place.getId()) && !m.getIsCompleted());

            if (alreadyHasMission) {
                log.info("장소 미션이 이미 존재하여 건너뜁니다: placeId={}", place.getId());
                continue;
            }

            // 2. GPT를 호출하여 미션 제목과 설명 생성
            GPTService.MissionCreationResult missionContent =
                    gptService.createMissionContent(
                            place.getTitle(), place.getCategory(), emotionKeyword);

            // 3. Mission 엔티티 생성
            Mission newMission = Mission.builder()
                    .user(user)
                    .place(place)
                    .title(missionContent.getTitle())
                    .description(missionContent.getDescription())
                    .points(DEFAULT_MISSION_POINTS)
                    .emotion(emotionKeyword) // GPT가 뽑은 세부 감정 단어를 저장
                    .isCompleted(false)
                    .createdAt(now)
                    .expiresAt(now.plusHours(24))
                    .build();

            newMissions.add(newMission);
        }

        missionRepository.saveAll(newMissions);
        log.info("미션 생성 완료. 총 {}개의 미션이 새로 생성되었습니다.", newMissions.size());

        // 4. 생성된 미션을 RecommendDto.MissionItem으로 변환
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
    // 미션 홈 조회
    @Transactional(readOnly = true)
    public MissionDto.MissionHomeResponse getMissionHome(Users user) {
        LocalDateTime now = LocalDateTime.now();

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

    // 미션 상세 조회
    @Transactional(readOnly = true)
    public MissionDto.MissionDetailResponse getMissionDetail(
            Users user, Long missionId, Double userLat, Double userLon) {

        if (userLat != null && userLon != null) {
            // 위치 정보가 있을 경우 미션 생성/누적 로직 실행 (감정 변화 체크)
            processMissionGenerationAndAccumulation(user, userLat, userLon);
        } else {
            log.warn("미션 상세 조회 시 위치 정보가 누락되어 미션 생성/누적 로직을 건너뜁니다.");
        }

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(MissionErrorCode.MISSION_NOT_FOUND));

        // 본인 미션 확인
        if (!mission.getUser().getId().equals(user.getId())) {
            throw new CustomException(MissionErrorCode.MISSION_NOT_OWNER);
        }

        Place place = mission.getPlace();

        // 거리 계산
        Double distance = null;
        Boolean canVerify = false;

        if (userLat != null && userLon != null) {
            distance = DistanceCalculator.calculateDistance(
                    userLat, userLon, place.getLatitude(), place.getLongitude());
            canVerify = distance <= VERIFICATION_RADIUS_KM && !mission.getIsCompleted() && !mission.isExpired();
        }

        // 장소 이미지 조회 (최대 3개)
        List<PlaceImage> images = placeImageRepository.findByPlaceOrderByDisplayOrder(place);
        List<String> imageUrls = images.stream()
                .limit(3)
                .map(PlaceImage::getImageUrl)
                .collect(Collectors.toList());

        // 카카오맵 URL 생성
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

        // 1. 현재 활성 미션 및 감정 상태 조회
        List<Mission> activeMissions = missionRepository.findActiveByUser(user, now).stream()
                .filter(m -> !m.getIsCompleted())
                .collect(Collectors.toList());

        RecommendDto.EmotionData currentEmotionData = emotionDataService.getCurrentEmotion(user);

        int currentEmotionScore = currentEmotionData.getEmotionScore();
        String currentDominantEmotionKeyword = currentEmotionData.getDominantEmotion(); // GPT가 뽑은 세부 감정 단어

        boolean needsNewMissionSet = activeMissions.isEmpty();
        boolean hasEmotionChanged = false;

        if (!activeMissions.isEmpty()) {
            // 현재 활성 미션의 감정 키워드와 GPT가 뽑은 세부 감정 단어가 다르면 변화 발생으로 간주
            String existingMissionEmotionKeyword = activeMissions.get(0).getEmotion();

            if (!existingMissionEmotionKeyword.equalsIgnoreCase(currentDominantEmotionKeyword)) {
                hasEmotionChanged = true;
                log.info("미션 감정 키워드 변화 감지: 기존 키워드={}, 현재 키워드={}",
                        existingMissionEmotionKeyword, currentDominantEmotionKeyword);
            } else {
                log.info("감정 키워드 변화 없음. 미션 생성을 건너뜁니다. keyword={}", existingMissionEmotionKeyword);
                return;
            }
        }

        // 2. 미션 생성/누적 조건 확인 및 실행
        if (needsNewMissionSet || hasEmotionChanged) {
            log.info("미션 생성 조건 충족: isNewSet={}, isChanged={}", needsNewMissionSet, hasEmotionChanged);

            // 현재 위치 주변 장소 추천 목록을 가져옵니다.
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
                log.warn("장소 추천 실패: 미션 생성을 건너뜁니다.");
            }
        }
    }

    // 미션 인증
    @Transactional
    public MissionDto.VerifyResponse verifyMission(
            Users user, Long missionId, MissionDto.VerifyRequest request) {

        // 미션 조회
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(MissionErrorCode.MISSION_NOT_FOUND));

        // 본인 미션 확인
        if (!mission.getUser().getId().equals(user.getId())) {
            throw new CustomException(MissionErrorCode.MISSION_NOT_OWNER);
        }

        // 이미 완료된 미션 확인
        if (mission.getIsCompleted()) {
            throw new CustomException(MissionErrorCode.MISSION_ALREADY_COMPLETED);
        }

        // 만료된 미션 확인
        if (mission.isExpired()) {
            throw new CustomException(MissionErrorCode.MISSION_EXPIRED);
        }

        // 위치 정보 확인
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new CustomException(MissionErrorCode.LOCATION_UNAVAILABLE);
        }

        // 거리 확인 (50m 이내)
        Place place = mission.getPlace();
        double distance = DistanceCalculator.calculateDistance(
                request.getLatitude(), request.getLongitude(),
                place.getLatitude(), place.getLongitude());

        if (distance > VERIFICATION_RADIUS_KM) {
            throw new CustomException(MissionErrorCode.LOCATION_TOO_FAR);
        }

        // 미션 완료 처리
        mission.complete();
        missionRepository.save(mission);

        // 포인트 지급
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

    // Mission을 MissionItem으로 변환
    private MissionDto.MissionItem convertToMissionItem(Mission mission, LocalDateTime now) {
        // NEW 태그 판단 (48시간 이내 생성)
        long hoursSinceCreated = ChronoUnit.HOURS.between(mission.getCreatedAt(), now);
        boolean isNew = hoursSinceCreated <= NEW_TAG_HOURS && !mission.getIsCompleted();

        return MissionDto.MissionItem.builder()
                .missionId(mission.getId())
                .missionTitle(mission.getTitle())
                .expiresAt(mission.getExpiresAt())
                .points(mission.getPoints())
                .isCompleted(mission.getIsCompleted())
                .isNew(isNew)
                .emotion(mission.getEmotion()) // 세부 감정 단어를 그대로 사용
                .build();
    }

    // 카카오맵 URL 생성
    private String generateKakaoMapUrl(Place place) {
        return String.format("https://map.kakao.com/link/map/%s,%s,%s",
                place.getTitle(),
                place.getLatitude(),
                place.getLongitude());
    }
}