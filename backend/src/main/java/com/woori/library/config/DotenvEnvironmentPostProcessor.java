package com.woori.library.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * 레포 루트의 {@code .env} 파일을 읽어 Environment에 등록한다.
 *
 * <p>{@code org.springframework.boot.env.EnvironmentPostProcessor}(구 패키지)는 Spring Boot 4.0부터
 * deprecated + 사실상 미호출 상태다 — 이 프로젝트가 쓰는 4.1.0에서는 {@code org.springframework.boot}
 * 패키지로 옮겨간 이 인터페이스만 {@code META-INF/spring.factories}를 통해 실제로 호출된다.
 * 이미 설정된 OS 환경변수/시스템 프로퍼티를 덮어쓰지 않도록 가장 낮은 우선순위로 추가한다.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envFile = Path.of(System.getProperty("user.dir"), "..", ".env").normalize();
        if (!Files.isRegularFile(envFile)) {
            return;
        }

        Properties properties = new Properties();
        try {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                properties.setProperty(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
            }
        } catch (IOException e) {
            throw new IllegalStateException(".env 파일을 읽는 중 오류가 발생했습니다: " + envFile, e);
        }

        environment.getPropertySources().addLast(new PropertiesPropertySource("dotenv", properties));
    }
}
