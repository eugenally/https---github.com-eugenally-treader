# Maven 의존성 변경 (pom.xml)

## Oracle JDBC 드라이버 제거

현재 설정에서 다음을 찾아 **제거**하세요:

```xml
<!-- 제거할 부분 -->
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc11</artifactId>
    <version>21.3.0.0</version>
</dependency>
```

또는 다른 버전이 있을 수 있습니다:
```xml
<!-- 이것도 모두 제거 -->
<dependency>
    <groupId>oracle</groupId>
    <artifactId>ojdbc14</artifactId>
    <version>10.2.0.4.0</version>
</dependency>

<dependency>
    <groupId>com.oracle</groupId>
    <artifactId>ojdbc6</artifactId>
    <version>11.2.0.4</version>
</dependency>
```

## MariaDB JDBC 드라이버 추가

위에서 제거한 부분 대신 다음을 **추가**하세요:

```xml
<!-- MariaDB JDBC 드라이버 -->
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <version>3.1.4</version>
</dependency>
```

## 다른 선택사항

### MySQL 드라이버 사용 (MariaDB도 지원)
```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

### Spring Boot 자동 구성 (권장)
Spring Boot를 사용하는 경우 드라이버를 명시적으로 지정할 필요가 없습니다:
```xml
<!-- Spring Boot에서 자동으로 MariaDB 드라이버 감지 -->
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
</dependency>
```

## pom.xml 변경 예시

### Before (Oracle)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.edu.bootstring</groupId>
    <artifactId>treader</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <!-- Oracle JDBC 드라이버 -->
        <dependency>
            <groupId>com.oracle.database.jdbc</groupId>
            <artifactId>ojdbc11</artifactId>
            <version>21.3.0.0</version>
        </dependency>

        <!-- 다른 의존성들... -->
    </dependencies>
</project>
```

### After (MariaDB)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.edu.bootstring</groupId>
    <artifactId>treader</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <!-- MariaDB JDBC 드라이버 -->
        <dependency>
            <groupId>org.mariadb.jdbc</groupId>
            <artifactId>mariadb-java-client</artifactId>
            <version>3.1.4</version>
        </dependency>

        <!-- 다른 의존성들... -->
    </dependencies>
</project>
```

## 변경 후 필수 작업

1. **pom.xml 수정 후 Maven 새로고침:**
   ```bash
   mvn clean install
   ```

2. **IDE에서 Maven 재구성 (IntelliJ IDEA):**
   - 마우스 우클릭 → `Maven` → `Reload Projects`
   - 또는 `View` → `Tool Windows` → `Maven` → 우측 상단 새로고침 버튼

3. **IDE에서 Maven 재구성 (Eclipse):**
   - 프로젝트 우클릭 → `Maven` → `Update Project`
   - 또는 `Project` → `Clean` → 새로 빌드

4. **application.properties 업데이트:**
   ```properties
   # Oracle 설정 제거 또는 주석 처리
   # spring.datasource.url=jdbc:oracle:thin:@localhost:1521:xe
   
   # MariaDB 설정 추가
   spring.datasource.url=jdbc:mysql://localhost:3306/treader_db
   spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
   ```

5. **애플리케이션 재시작:**
   ```bash
   mvn spring-boot:run
   ```

## 버전 호환성

### MariaDB JDBC 드라이버 버전
| 드라이버 버전 | MariaDB | MySQL | Java |
|-------------|---------|-------|------|
| 3.1.4       | 5.5+    | 5.7+  | 8+   |
| 3.0.x       | 5.5+    | 5.7+  | 8+   |
| 2.7.x       | 5.5+    | 5.7+  | 7+   |

### Spring Boot 호환성
- Spring Boot 2.x: MariaDB Driver 3.0+
- Spring Boot 3.x: MariaDB Driver 3.1+

## 트러블슈팅

### 문제: "No suitable driver found"
**해결책:**
```xml
<!-- pom.xml에 MariaDB 드라이버가 있는지 확인 -->
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <version>3.1.4</version>
</dependency>

<!-- 그 후 Maven 클린빌드 -->
<!-- mvn clean install -->
```

### 문제: "Connection refused"
**해결책:**
1. MariaDB 서버 실행 확인: `mysql -u root -p`
2. 데이터베이스 생성 확인: `SHOW DATABASES;`
3. URL과 포트 확인: `jdbc:mysql://localhost:3306/treader_db`

### 문제: "Access denied for user 'root'"
**해결책:**
```properties
# application.properties에서 비밀번호 확인
spring.datasource.username=root
spring.datasource.password=your_password  # 올바른 비밀번호 입력
```

### 문제: "Character encoding UTF-8"
**해결책:**
```properties
# application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/treader_db?useUnicode=true&characterEncoding=utf8mb4
```

## 추가 참고 자료
- [MariaDB Java Driver 공식 문서](https://mariadb.com/docs/server/connect/drivers/java/)
- [MariaDB 다운로드](https://mariadb.org/download/)
- [Spring Boot DataSource 자동구성](https://spring.io/blog/2021/01/30/spring-data-jpa-query-by-example)
