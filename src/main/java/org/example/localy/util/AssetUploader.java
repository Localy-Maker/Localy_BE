package org.example.localy.util;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * SVG/PNG 에셋을 S3에 업로드하는 독립 실행 유틸리티.
 *
 * 실행: ./gradlew uploadAssets
 *
 * AWS 자격증명 사전 설정 필요:
 *   aws configure
 *   또는 환경변수 AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY
 *
 * 업로드 후 S3 버킷 퍼블릭 읽기 정책 필요:
 *   {
 *     "Version": "2012-10-17",
 *     "Statement": [{"Sid":"PublicRead","Effect":"Allow","Principal":"*",
 *       "Action":"s3:GetObject","Resource":"arn:aws:s3:::localy-localhost/*"}]
 *   }
 */
public class AssetUploader {

    private static final String BUCKET = "localy-localhost";
    private static final Region REGION = Region.AP_NORTHEAST_2;
    private static final String S3_BASE = "https://" + BUCKET + ".s3." + REGION.id() + ".amazonaws.com";
    private static final String ASSETS_DIR = System.getProperty("assetsDir",
            System.getProperty("user.home") + "/Downloads/localy-drive-download");

    // 캐릭터 SVG 파일명 → 감정 코드 매핑 (순서 대로 Vector.svg=happy, Vector-1.svg=sadness, ...)
    private static final Map<String, String> CHARACTER_FILE_MAP = new LinkedHashMap<>();

    // 감정 폴더명(한글) → 감정 코드
    private static final Map<String, String> EMOTION_CODE_MAP = new LinkedHashMap<>();

    // 기본 아이템 폴더명 → S3 카테고리 경로
    private static final Map<String, String> CATEGORY_DIR_MAP = new LinkedHashMap<>();

    // 카테고리 → S3 파일 접두사
    private static final Map<String, String> ITEM_PREFIX_MAP = new LinkedHashMap<>();

    static {
        // Vector=#ff6868(빨강→분노), -1=#e0d0ff(라벤더→우울), -2=#d9d9d9(회색→중립),
        // -3=#c8e1ff(파랑→슬픔), -4=#afecb2(연초록→불안), -5=#fff06c(노랑→행복)
        CHARACTER_FILE_MAP.put("character_Vector.svg",   "anger");
        CHARACTER_FILE_MAP.put("character_Vector-1.svg", "depressed");
        CHARACTER_FILE_MAP.put("character_Vector-2.svg", "neutral");
        CHARACTER_FILE_MAP.put("character_Vector-3.svg", "sadness");
        CHARACTER_FILE_MAP.put("character_Vector-4.svg", "anxiety");
        CHARACTER_FILE_MAP.put("character_Vector-5.svg", "happy");

        EMOTION_CODE_MAP.put("행복", "happy");
        EMOTION_CODE_MAP.put("슬픔", "sadness");
        EMOTION_CODE_MAP.put("분노", "anger");
        EMOTION_CODE_MAP.put("불안", "anxiety");
        EMOTION_CODE_MAP.put("우울", "depressed");
        EMOTION_CODE_MAP.put("중립", "neutral");

        CATEGORY_DIR_MAP.put("아이템-모자",    "hat");
        CATEGORY_DIR_MAP.put("아이템-배경",    "background");
        CATEGORY_DIR_MAP.put("아이템-악세서리", "accessory");
        CATEGORY_DIR_MAP.put("아이템-기타",    "etc");

        ITEM_PREFIX_MAP.put("hat",        "hat");
        ITEM_PREFIX_MAP.put("background", "bg");
        ITEM_PREFIX_MAP.put("accessory",  "acc");
        ITEM_PREFIX_MAP.put("etc",        "etc");
    }

    public static void main(String[] args) throws IOException {
        System.out.println("S3 버킷: " + BUCKET + " (" + REGION.id() + ")");
        System.out.println("에셋 경로: " + ASSETS_DIR);

        S3Client s3 = S3Client.builder()
                .region(REGION)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        uploadCharacters(s3);
        uploadBaseItems(s3);
        uploadEmotionItems(s3);

        System.out.println("\n✅ 업로드 완료!");
        s3.close();
    }

    private static void uploadCharacters(S3Client s3) throws IOException {
        System.out.println("\n[캐릭터 업로드]");
        Path charDir = Paths.get(ASSETS_DIR, "캐릭터-확대.그림자ver");

        for (Map.Entry<String, String> entry : CHARACTER_FILE_MAP.entrySet()) {
            Path file = charDir.resolve(entry.getKey());
            if (!Files.exists(file)) {
                System.out.println("  ⚠ 파일 없음: " + entry.getKey());
                continue;
            }
            String key = "images/characters/" + entry.getValue() + ".svg";
            String url = upload(s3, file, key);
            System.out.println("  ✓ " + entry.getValue() + ": " + url);
        }
    }

    private static void uploadBaseItems(S3Client s3) throws IOException {
        System.out.println("\n[기본 아이템 업로드]");
        Path baseDir = Paths.get(ASSETS_DIR, "아이템(기본)");

        for (Map.Entry<String, String> catEntry : CATEGORY_DIR_MAP.entrySet()) {
            Path folder = baseDir.resolve(catEntry.getKey());
            if (!Files.isDirectory(folder)) {
                System.out.println("  ⚠ 폴더 없음: " + catEntry.getKey());
                continue;
            }
            List<Path> files = getSortedFiles(folder);
            String category = catEntry.getValue();
            String prefix   = ITEM_PREFIX_MAP.get(category);

            for (int i = 0; i < files.size(); i++) {
                Path file = files.get(i);
                String ext = getExtension(file);
                String key = String.format("images/items/base/%s/%s_%02d%s", category, prefix, i + 1, ext);
                String url = upload(s3, file, key);
                System.out.printf("  ✓ [%s] %02d (%s): %s%n", category, i + 1, file.getFileName(), url);
            }
        }
    }

    private static void uploadEmotionItems(S3Client s3) throws IOException {
        System.out.println("\n[감정별 아이템 업로드]");
        Map<String, String> emotionDirMap = new LinkedHashMap<>();
        emotionDirMap.put("모자",    "hat");
        emotionDirMap.put("배경",    "background");
        emotionDirMap.put("악세서리", "accessory");
        emotionDirMap.put("기타",    "etc");

        for (Map.Entry<String, String> emotionEntry : EMOTION_CODE_MAP.entrySet()) {
            String emotionKo   = emotionEntry.getKey();
            String emotionCode = emotionEntry.getValue();
            Path emotionDir    = Paths.get(ASSETS_DIR, emotionKo);

            if (!Files.isDirectory(emotionDir)) {
                System.out.println("  ⚠ 폴더 없음: " + emotionKo);
                continue;
            }

            for (Map.Entry<String, String> catEntry : emotionDirMap.entrySet()) {
                Path folder = emotionDir.resolve(emotionKo + "-" + catEntry.getKey());
                if (!Files.isDirectory(folder)) continue;

                List<Path> files   = getSortedFiles(folder);
                String category    = catEntry.getValue();
                String prefix      = ITEM_PREFIX_MAP.get(category);

                for (int i = 0; i < files.size(); i++) {
                    Path file = files.get(i);
                    String ext = getExtension(file);
                    String key = String.format("images/items/%s/%s/%s_%02d%s", emotionCode, category, prefix, i + 1, ext);
                    upload(s3, file, key);
                }
                System.out.printf("  ✓ %s/%s: %d개 업로드 완료%n", emotionCode, category, files.size());
            }
        }
    }

    private static String upload(S3Client s3, Path file, String key) throws IOException {
        String contentType = key.endsWith(".svg") ? "image/svg+xml" : "image/png";
        byte[] bytes = Files.readAllBytes(file);

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .contentType(contentType)
                .contentLength((long) bytes.length)
                .build();

        s3.putObject(req, RequestBody.fromBytes(bytes));
        return S3_BASE + "/" + key;
    }

    private static List<Path> getSortedFiles(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        }
    }

    private static String getExtension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot).toLowerCase() : ".svg";
    }
}
