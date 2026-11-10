package com.barofit.report;

import com.barofit.testsupport.BarofitTestPostgres;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 실제 PostgreSQL 동작을 검증하되 개발자의 로컬 DB에는 쓰지 않는다. barofit-common의 공유 컨테이너를 report 스키마로 마이그레이션해서 쓴다
 * (컨테이너 자체는 다른 테스트와 공유하지만 report 스키마는 이 그룹만 쓴다).
 *
 * <p>스키마를 통째로 지우고 재마이그레이션하는 {@link ReportSchemaMigrationTest}는 이 공유 컨테이너를 쓰면 안 되므로 독립된 컨테이너를 그대로
 * 쓴다.
 */
public abstract class ReportPostgresSupport {
  private static final PostgreSQLContainer<?> POSTGRES = migrate();

  private static PostgreSQLContainer<?> migrate() {
    PostgreSQLContainer<?> container = BarofitTestPostgres.container();
    org.flywaydb.core.Flyway.configure()
        .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
        .schemas("report")
        .locations("classpath:db/migration/report")
        .load()
        .migrate();
    return container;
  }

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            POSTGRES.getJdbcUrl()
                + (POSTGRES.getJdbcUrl().contains("?") ? "&" : "?")
                + "currentSchema=report");
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.flyway.schemas", () -> "report");
    registry.add("spring.flyway.default-schema", () -> "report");
    registry.add("spring.flyway.locations", () -> "classpath:db/migration/report");
  }
}
