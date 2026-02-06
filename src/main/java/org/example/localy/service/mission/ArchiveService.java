package org.example.localy.service.mission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.MissionErrorCode;
import org.example.localy.dto.mission.MissionArchiveDto;
import org.example.localy.entity.Users;
import org.example.localy.entity.place.MissionArchive;
import org.example.localy.repository.place.MissionArchiveRepository;
import org.example.localy.repository.place.MissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveService {

    private final MissionArchiveRepository missionArchiveRepository;
    private final MissionRepository missionRepository;

    @Transactional(readOnly = true)
    public MissionArchiveDto.DetailResponse getDayDetail(Users user, LocalDate date) {
        // 해당 일자 미션 완료 개수 (인증 완료 기준)
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);
        long missionCount = missionRepository.countCompletedMissionsByDate(user, start, end);

        // 저장된 사진 목록
        List<MissionArchiveDto.PhotoItem> photos = missionArchiveRepository.findByUserAndArchivedDate(user, date).stream()
                .map(a -> MissionArchiveDto.PhotoItem.builder()
                        .archiveId(a.getId())
                        .imageUrl(a.getImageUrl())
                        .isThumbnail(a.getIsThumbnail())
                        .build())
                .collect(Collectors.toList());

        return MissionArchiveDto.DetailResponse.builder()
                .userId(user.getId())
                .date(date)
                .completedMissionCount(missionCount)
                .photos(photos)
                .build();
    }

    @Transactional
    public void uploadArchivePhoto(Users user, MissionArchiveDto.UploadRequest request) {
        // 갤러리 저장 날짜와 달력 선택 날짜 일치 확인
        if (!request.getTargetDate().equals(request.getPhotoStoredDate())) {
            throw new CustomException(MissionErrorCode.DATE_MISMATCH);
        }

        MissionArchive archive = MissionArchive.builder()
                .user(user)
                .imageUrl(request.getImageUrl())
                .archivedDate(request.getTargetDate())
                .build();

        missionArchiveRepository.save(archive);
    }

    @Transactional
    public void setThumbnail(Users user, Long archiveId) {
        MissionArchive target = missionArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new CustomException(MissionErrorCode.MISSION_NOT_FOUND));

        if (!target.getUser().getId().equals(user.getId())) {
            throw new CustomException(MissionErrorCode.MISSION_NOT_OWNER);
        }

        // 해당 날짜의 기존 썸네일 해제 후 새 썸네일 지정
        missionArchiveRepository.resetThumbnails(user, target.getArchivedDate());
        target.setIsThumbnail(true);
    }
}