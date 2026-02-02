package org.example.localy.service.mission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.MissionErrorCode;
import org.example.localy.entity.Users;
import org.example.localy.entity.place.MissionArchive;
import org.example.localy.repository.place.MissionArchiveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveService {

    private final MissionArchiveRepository missionArchiveRepository;

    @Transactional
    public void archiveMissionPhoto(Users user, String imageUrl, LocalDate targetDate, LocalDate photoStoredDate) {

        // 사진 저장 날짜와 아카이빙 타겟 날짜 일치 확인
        if (!targetDate.equals(photoStoredDate)) {
            log.warn("아카이빙 날짜 불일치: userId={}, target={}, photo={}", user.getId(), targetDate, photoStoredDate);
            throw new CustomException(MissionErrorCode.DATE_MISMATCH);
        }

        // 아카이빙 엔티티 생성 및 저장
        MissionArchive archive = MissionArchive.builder()
                .user(user)
                .imageUrl(imageUrl)
                .archivedDate(targetDate)
                .build();

        missionArchiveRepository.save(archive);

        log.info("캘린더 아카이빙 성공: userId={}, date={}", user.getId(), targetDate);
    }
}