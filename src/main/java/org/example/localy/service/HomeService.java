package org.example.localy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.dto.place.PlaceDto;
import org.example.localy.entity.Users;
import org.example.localy.entity.place.Bookmark;
import org.example.localy.repository.place.BookmarkRepository;
import org.example.localy.service.place.PlaceService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private final PlaceService placeService;
    private final BookmarkRepository bookmarkRepository;

    // HomeResponse에 들어가는 recentBookmarks 용 함수
    public List<PlaceDto.BookmarkItem> getRecentBookmarks(Users user) {

        // 유저 null → 빈 리스트
        if (user == null) return List.of();

        // 최근 북마크 5개 조회
        List<Bookmark> bookmarks =
                bookmarkRepository.findTop5ByUserOrderByCreatedAtDesc(
                        user, PageRequest.of(0, 5)
                );

        // Bookmark → BookmarkItem 변환
        return bookmarks.stream()
                .map(placeService::convertToBookmarkItem)
                .collect(Collectors.toList());
    }
}
