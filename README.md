# log_roller

프로세스 로그 보관 프로세스

### 실행 방법
`gradle shadowJar`로 출력 된 jar 경로에 아래 파일 생성
- config.properties
- logback.xml (선택 사항)

`$> java "-Dlogback.configurationFile=logback.xml" -Xmx512M -Xms512M -jar ./log_roller-1.0.0-RELEASE.jar`

### 설정 변경
app/src/main/resources/config.properties 파일 참조