package com.comtec.log_roller.component;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.comtec.log_roller.config.ConfigurationProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileHandler {

    private static ConfigurationProperties properties = ConfigurationProperties.getInstance();

    private String originDir = properties.getString("logfile.origin.dir");
    private String backupDir = properties.getString("logfile.backup.dir");

    private boolean extensionFilter = properties.getBoolean("logfile.extension.filter");
    private String extensionType = properties.getString("logfile.extension.type");

    private int originDay = properties.getInt("logfile.origin.hold.day");
    private int backupDay = properties.getInt("logfile.backup.hold.day");

    /**
     * 파일 핸들러 싱글턴 객체 반환
     * 
     * @return
     */
    public static FileHandler getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 원본 로그 파일 삭제
     */
    public void moveFile() {
        log.info("moveFile(): executed.");

        // 백업 디렉토리가 없는 경우 생성한다
        if (!new File(backupDir).exists()) {
            new File(backupDir).mkdirs();
        }

        List<File> sourceFiles = Collections.emptyList();

        for (String dir : getSplitedStrings(originDir, ";")) {
            File directory = new File(getSlashEndedString(dir));
            sourceFiles = Stream
                    .concat(sourceFiles.stream(), getFilesToHandle(directory, originDay).stream())
                    .toList();
        }

        // 백업 파일 생성
        sourceFiles.stream().forEach(file -> {

            File targetFile = new File(getSlashEndedString(backupDir) + file.getName());

            log.info("{} {}", file.getName(), targetFile.getAbsolutePath());

            file.renameTo(targetFile);
        });

        log.info("moveFile(): move {} log file(s)", sourceFiles.size());
        log.info("moveFile(): moved files {}", sourceFiles.toString());
    }

    /**
     * 백업된 로그 파일 삭제
     */
    public void deleteFile() {
        log.info("deleteFile(): executed.");

        List<File> deleteTargetFiles = Collections.emptyList();

        for (String dir : getSplitedStrings(backupDir, ";")) {
            File directory = new File(getSlashEndedString(dir));
            deleteTargetFiles = Stream
                    .concat(deleteTargetFiles.stream(), getFilesToHandle(directory, backupDay).stream())
                    .toList();
        }

        deleteTargetFiles.stream().forEach(file -> {
            file.delete();
        });

        log.info("deleteFile(): delete {} log file(s)", deleteTargetFiles.size());
        log.info("deleteFile(): deleted files {}", deleteTargetFiles.toString());
    }

    /**
     * 문자열 분리 메소드
     * 
     * @param origin
     * @param delimiter
     * @return
     */
    private List<String> getSplitedStrings(String origin, String delimiter) {
        return Arrays.asList(origin.split(delimiter));
    }

    /**
     * 입력 <b>문자열 끝에 슬래쉬(/) 기호를 붙인 문자열을 반환</b>
     * 
     * @param origin
     * @return
     */
    private String getSlashEndedString(String origin) {
        return !origin.endsWith("/") ? origin + "/" : origin;
    }

    /**
     * 디렉토리 내의 <b>필터링 대상 파일들을 반환<b>
     * 
     * @param directory
     * @return
     */
    private List<File> getFilesToHandle(File directory, int filekeepday) {

        return directory.isDirectory()
                ? Arrays.asList(directory.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        // 최종 수정 일 체크
                        Calendar lastModified = Calendar.getInstance();
                        lastModified.setTimeInMillis(pathname.lastModified());

                        // 프로퍼티 설정 값 체크
                        Calendar keepingDate = Calendar.getInstance();
                        keepingDate.add(Calendar.DATE, -filekeepday);

                        boolean result = lastModified.compareTo(keepingDate) < 0;

                        // 파일 확장자 체크
                        if (extensionFilter) {
                            result &= getSplitedStrings(extensionType, ";")
                                    .contains(
                                            getFileExtension(pathname.getName())
                                                    .orElse(""));
                        }

                        return result;
                    }
                }))
                : Collections.emptyList(); // directory 파라미터가 디렉토리가 아닌 경우 빈 리스트 반환
    }

    /**
     * <b>파일의 확장자를 반환</b><br>
     * <i>확장자가 없는 파일은 Option.empty가 반환됨<i>
     * 
     * @param filename
     * @return
     */
    private Optional<String> getFileExtension(String filename) {
        List<String> splitedFilename = getSplitedStrings(filename, "\\.");

        return splitedFilename.isEmpty()
                ? Optional.empty()
                : Optional.of(splitedFilename.get(splitedFilename.size() - 1));
    }

    /**
     * 생성자
     */
    private FileHandler() {
    }

    /**
     * 싱글턴 객체 구현을 위한 홀더
     * 스레드 경합, lazy loading 기능을 함
     */
    private static class SingletonHolder {
        private static final FileHandler INSTANCE = new FileHandler();
    }
}
