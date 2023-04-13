package com.comtec.log_roller.component;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
    private String backupDirPart = properties.getString("logfile.backup.dir.part");

    private boolean extensionFilter = properties.getBoolean("logfile.extension.filter");
    private String extensionType = properties.getString("logfile.extension.type");

    private int originCopyDay = properties.getInt("logfile.origin.copy.day");
    private int originHoldDay = properties.getInt("logfile.origin.hold.day");

    /**
     * 파일 핸들러 싱글턴 객체 반환
     * 
     * @return
     */
    public static FileHandler getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 원본 로그 파일 복제
     */
    public void copyFile() {
        log.info("copyFile(): executed.");

        // 백업 디렉토리가 없는 경우 생성한다
        if (!new File(backupDir).exists()) {
            new File(backupDir).mkdirs();
        }

        List<File> sourceFiles = Collections.emptyList();

        for (String dir : getSplitedStrings(originDir, ";")) {
            File directory = new File(getSlashEndedString(dir));
            sourceFiles = Stream
                    .concat(sourceFiles.stream(), getFilesToHandle(directory, originCopyDay).stream())
                    .toList();
        }

        // 백업 파일 생성
        sourceFiles.stream().forEach(file -> {

            File targetFile = new File(getBackupDir(file) + file.getName());

            log.debug("{} {}", file.getName(), targetFile.getAbsolutePath());

            try {
                Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            } catch (FileAlreadyExistsException e) {
                log.debug("copyFile(): {} File already exists", file.getName());
            } catch (IOException e) {
                log.error("copyFile(): {}", e.toString());
            }
        });

        log.info("copyFile(): copy {} log file(s)", sourceFiles.size());
        log.info("copyFile(): copied files {}", sourceFiles.toString());
    }

    /**
     * 원본 로그 파일 삭제
     */
    public void deleteFile() {
        log.info("deleteFile(): executed.");

        List<File> deleteTargetFiles = Collections.emptyList();

        for (String dir : getSplitedStrings(originDir, ";")) {
            File directory = new File(getSlashEndedString(dir));
            deleteTargetFiles = Stream
                    .concat(deleteTargetFiles.stream(), getFilesToHandle(directory, originHoldDay).stream())
                    .toList();
        }

        deleteTargetFiles.stream().forEach(file -> {
            file.delete();
        });

        log.info("deleteFile(): delete {} log file(s)", deleteTargetFiles.size());
        log.info("deleteFile(): deleted files {}", deleteTargetFiles.toString());
    }

    /**
     * 백업 파일 최종 수정일 기준, 백업 디렉토리 생성 후 백업 디렉토리명 반환
     * 
     * @param backupFile
     * @return
     */
    public String getBackupDir(File backupFile) {
        String rootDirPath = getSlashEndedString(backupDir);

        Calendar lastModifiedDate = Calendar.getInstance();
        lastModifiedDate.setTimeInMillis(backupFile.lastModified());
        String year = "" + lastModifiedDate.get(Calendar.YEAR);
        String month = "" + lastModifiedDate.get(Calendar.MONTH);
        String date = "" + lastModifiedDate.get(Calendar.DATE);

        log.debug("last modified, year:{}, month:{}, day:{}", year, month, date);

        String backupDirPath = switch (backupDirPart.toUpperCase()) {
            case "YEAR" -> rootDirPath + year;
            case "MONTH" -> rootDirPath + year + "/" + month;
            case "DATE" -> rootDirPath + year + "/" + month + "/" + date;
            default -> rootDirPath + year + "/" + month + "/" + date;
        };

        backupDirPath = getSlashEndedString(backupDirPath);

        File backupDir = new File(backupDirPath);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        return backupDirPath;
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
