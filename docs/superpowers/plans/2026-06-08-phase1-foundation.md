# InterviewAI Phase 1 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the foundation skeleton — Spring Boot backend with user auth (JWT), Vue3 frontend with routing, and verify SpringAI ↔ DeepSeek connectivity.

**Architecture:** Spring Boot 3.x monolith backend (Java 17, Maven) with layered architecture (controller → service → mapper). Vue3 SPA frontend with Element Plus. JWT stored in Redis for stateless auth. MySQL for persistence.

**Tech Stack:** Spring Boot 3.x, MyBatis-Plus 3.5.x, Spring Security 6.x, jjwt, Redis, Vue3 + TS + Element Plus + Vue Router + Pinia + Axios

---

## File Map

### Backend (interview-ai-backend/)
```
pom.xml
src/main/java/com/interviewai/
├── InterviewAiApplication.java
├── common/
│   ├── Result.java              — Unified API response wrapper
│   ├── ResultCode.java          — Status code constants
│   └── GlobalExceptionHandler.java — @RestControllerAdvice
├── entity/
│   ├── User.java
│   ├── Resume.java
│   ├── ResumeAnalysis.java
│   ├── InterviewSession.java
│   ├── InterviewMessage.java
│   ├── Question.java
│   └── JobDescription.java
├── mapper/
│   ├── UserMapper.java (plus 6 others)
│   └── ...
├── dto/
│   ├── RegisterRequest.java
│   └── LoginRequest.java
├── vo/
│   ├── LoginVO.java
│   └── UserProfileVO.java
├── service/
│   ├── UserService.java
│   └── impl/UserServiceImpl.java
├── controller/
│   ├── UserController.java
│   └── AiController.java
├── config/
│   ├── SecurityConfig.java
│   ├── JwtAuthFilter.java
│   └── SpringAiConfig.java
└── common/
    └── JwtUtils.java
src/main/resources/
├── application.yml
├── schema.sql
└── data.sql
```

### Frontend (interview-ai-frontend/)
```
package.json
vite.config.ts
tsconfig.json
tsconfig.app.json
tsconfig.node.json
index.html
src/
├── App.vue
├── main.ts
├── style.css
├── router/index.ts
├── api/
│   ├── request.ts          — Axios instance + interceptors
│   └── user.ts             — User API calls
├── stores/user.ts          — Pinia user store
├── types/user.ts           — TS interfaces
├── views/
│   ├── Login.vue
│   ├── Register.vue
│   ├── Dashboard.vue
│   └── Layout.vue          — App layout shell
└── vite-env.d.ts
```

---

### Task 1: Initialize Spring Boot Backend Project

**Files:**
- Create: `D:/claude-code-workspace/AI求职助手/interview-ai-backend/pom.xml`
- Create: `D:/claude-code-workspace/AI求职助手/interview-ai-backend/src/main/java/com/interviewai/InterviewAiApplication.java`
- Create: `D:/claude-code-workspace/AI求职助手/interview-ai-backend/src/main/resources/application.yml`

- [ ] **Step 1: Create directory structure**

```bash
mkdir -p interview-ai-backend/src/main/java/com/interviewai/{common,entity,mapper,dto,vo,service/impl,controller,config}
mkdir -p interview-ai-backend/src/main/resources
mkdir -p interview-ai-backend/src/test/java/com/interviewai
```

- [ ] **Step 2: Write pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.0</version>
        <relativePath/>
    </parent>

    <groupId>com.interviewai</groupId>
    <artifactId>interview-ai-backend</artifactId>
    <version>1.0.0</version>
    <name>InterviewAI Backend</name>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.9</mybatis-plus.version>
        <jjwt.version>0.12.6</jjwt.version>
        <spring-ai.version>1.0.0-M6</spring-ai.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Spring AI - OpenAI (for DeepSeek) -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
            <version>${spring-ai.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <repositories>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
            <snapshots><enabled>false</enabled></snapshots>
        </repository>
    </repositories>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Write InterviewAiApplication.java**

```java
package com.interviewai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.interviewai.mapper")
public class InterviewAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(InterviewAiApplication.class, args);
    }
}
```

- [ ] **Step 4: Write application.yml**

```yaml
server:
  port: 8080

spring:
  application:
    name: interview-ai

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/interview_ai?useUnicode=true&characterEncoding=utf-8mb4&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true
    username: root
    password: ${MYSQL_PASSWORD:root}

  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      database: 0
      timeout: 3000ms

  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY:sk-your-key-here}
      base-url: https://api.deepseek.com/v1
      chat:
        options:
          model: deepseek-chat
        enabled: true

mybatis-plus:
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

jwt:
  secret: ${JWT_SECRET:interview-ai-jwt-secret-key-change-in-production-2026}
  expiration: 604800000  # 7 days in ms

logging:
  level:
    com.interviewai: debug
```

- [ ] **Step 5: Verify Maven compiles**

```bash
cd interview-ai-backend
mvn compile -q
```

Expected: BUILD SUCCESS

---

### Task 2: Common Classes — Unified Response & Exception Handling

**Files:**
- Create: `interview-ai-backend/src/main/java/com/interviewai/common/ResultCode.java`
- Create: `interview-ai-backend/src/main/java/com/interviewai/common/Result.java`
- Create: `interview-ai-backend/src/main/java/com/interviewai/common/GlobalExceptionHandler.java`
- Create: `interview-ai-backend/src/main/java/com/interviewai/common/BizException.java`

- [ ] **Step 1: Write ResultCode.java**

```java
package com.interviewai.common;

import lombok.Getter;

/**
 * 统一响应状态码。
 * 设计思路：按模块分段，方便排查问题来源。
 * 1xxx 用户模块, 2xxx 简历模块, 3xxx 面试模块, 4xxx 题库模块, 9xxx 通用错误
 */
@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数有误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 用户模块 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "用户名或密码错误"),
    USER_ALREADY_EXISTS(1003, "用户名已被注册"),
    USER_DISABLED(1004, "账号已被禁用"),

    // 简历模块 2xxx
    RESUME_NOT_FOUND(2001, "简历不存在"),
    RESUME_UPLOAD_FAILED(2002, "简历上传失败"),
    RESUME_PARSE_FAILED(2003, "简历解析失败"),

    // 面试模块 3xxx
    INTERVIEW_NOT_FOUND(3001, "面试场次不存在"),
    INTERVIEW_QUOTA_EXCEEDED(3002, "今日面试次数已用完"),
    INTERVIEW_SESSION_EXPIRED(3003, "面试会话已过期"),

    // 题库模块 4xxx
    QUESTION_NOT_FOUND(4001, "题目不存在"),

    // AI 相关 8xxx
    AI_SERVICE_ERROR(8001, "AI服务调用失败，请稍后重试"),
    AI_TIMEOUT(8002, "AI服务响应超时");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 2: Write Result.java**

```java
package com.interviewai.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一API响应体。
 * 所有Controller返回值都用它包裹，前端只和这一个数据结构打交道。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(ResultCode code) {
        return new Result<>(code.getCode(), code.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode code, String msg) {
        return new Result<>(code.getCode(), msg, null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }
}
```

- [ ] **Step 3: Write BizException.java**

```java
package com.interviewai.common;

import lombok.Getter;

/**
 * 业务异常。在Service层抛出，由GlobalExceptionHandler统一处理。
 * 这比到处写try-catch + return Result.fail()更干净。
 */
@Getter
public class BizException extends RuntimeException {
    private final ResultCode resultCode;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
```

- [ ] **Step 4: Write GlobalExceptionHandler.java**

```java
package com.interviewai.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常拦截。把所有异常翻译成统一的Result格式。
 * 不在Controller里写try-catch，都交给这里处理。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getResultCode(), e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", msg);
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.fail(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.INTERNAL_ERROR, "服务器开小差了，请稍后重试");
    }
}
```

---

### Task 3: Entity Classes (7 tables)

**Files:** Create 7 entity files under `entity/`

- [ ] **Step 1: Write User.java**

```java
package com.interviewai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String avatarUrl;
    private String role;        // USER / ADMIN
    private Integer status;     // 1正常 0禁用
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: Write Resume.java**

```java
package com.interviewai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("resume")
public class Resume {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String fileName;
    private String fileUrl;
    private String rawText;
    private String structuredData;  // JSON string
    private String status;          // PROCESSING / COMPLETED / FAILED
    private Integer version;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: Write ResumeAnalysis.java**

```java
package com.interviewai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("resume_analysis")
public class ResumeAnalysis {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resumeId;
    private Integer skillScore;
    private Integer descriptionQuality;
    private Integer keywordCoverage;
    private Integer formatScore;
    private Integer overallScore;
    private String suggestions;     // JSON
    private String strengths;       // JSON
    private String weaknesses;      // JSON
    private String optimizedResume;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: Write InterviewSession.java**

```java
package com.interviewai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("interview_session")
public class InterviewSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long resumeId;
    private String mode;            // RESUME_DEEP / TECH_SPECIAL / MIXED
    private String techTags;        // JSON array
    private String status;          // IN_PROGRESS / COMPLETED / ABORTED
    private Integer totalQuestions;
    private Integer answeredCount;
    private Double totalScore;
    private String strengths;       // JSON
    private String weaknesses;      // JSON
    private String radarData;       // JSON
    private Integer duration;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
```

- [ ] **Step 5: Write InterviewMessage.java**

```java
package com.interviewai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("interview_message")
public class InterviewMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Integer sequence;
    private String role;            // AI / USER
    private String content;
    private Long questionId;
    private Integer score;
    private String feedback;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 6: Write Question.java**

```java
package com.interviewai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String answer;
    private String tags;            // JSON array
    private Integer difficulty;     // 1简单 2中等 3困难
    private String embeddingId;
    private Integer usageCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 7: Write JobDescription.java**

```java
package com.interviewai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("job_description")
public class JobDescription {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resumeId;
    private Long userId;
    private String jdText;
    private Integer matchScore;
    private String skillGap;        // JSON
    private String suggestions;     // JSON
    private String status;          // PROCESSING / COMPLETED / FAILED
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 8: Write AutoFillMetaObjectHandler for create/update time**

```java
package com.interviewai.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充 created_at / updated_at。
 * 不用在每个Service里手动set时间。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
```

---

### Task 4: Mapper Interfaces

**Files:** Create 7 mapper interfaces under `mapper/`

- [ ] **Step 1: Write UserMapper.java**

```java
package com.interviewai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewai.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

- [ ] **Step 2: Write remaining 6 mappers (all follow same pattern)**

```java
// ResumeMapper.java
package com.interviewai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewai.entity.Resume;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}

// ResumeAnalysisMapper.java
package com.interviewai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewai.entity.ResumeAnalysis;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeAnalysisMapper extends BaseMapper<ResumeAnalysis> {
}

// InterviewSessionMapper.java
package com.interviewai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewai.entity.InterviewSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewSessionMapper extends BaseMapper<InterviewSession> {
}

// InterviewMessageMapper.java
package com.interviewai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewai.entity.InterviewMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewMessageMapper extends BaseMapper<InterviewMessage> {
}

// QuestionMapper.java
package com.interviewai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewai.entity.Question;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}

// JobDescriptionMapper.java
package com.interviewai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewai.entity.JobDescription;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobDescriptionMapper extends BaseMapper<JobDescription> {
}
```

---

### Task 5: JWT & Security Utilities

**Files:**
- Create: `interview-ai-backend/src/main/java/com/interviewai/common/JwtUtils.java`

- [ ] **Step 1: Write JwtUtils.java**

```java
package com.interviewai.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT生成和校验工具。
 * 密钥通过配置注入，便于不同环境使用不同密钥。
 */
@Component
public class JwtUtils {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtUtils(@Value("${jwt.secret}") String secret,
                    @Value("${jwt.expiration}") long expiration) {
        // jjwt 0.12+ 要求密钥至少256位，不足则用SHA-256派生
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserIdFromToken(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    public boolean isTokenExpired(String token) {
        try {
            parseToken(token);
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
```

---

### Task 6: DTOs & VOs

**Files:**
- Create: `interview-ai-backend/src/main/java/com/interviewai/dto/RegisterRequest.java`
- Create: `interview-ai-backend/src/main/java/com/interviewai/dto/LoginRequest.java`
- Create: `interview-ai-backend/src/main/java/com/interviewai/vo/LoginVO.java`
- Create: `interview-ai-backend/src/main/java/com/interviewai/vo/UserProfileVO.java`

- [ ] **Step 1: Write RegisterRequest.java**

```java
package com.interviewai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度3-20个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度6-30个字符")
    private String password;
}
```

- [ ] **Step 2: Write LoginRequest.java**

```java
package com.interviewai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

- [ ] **Step 3: Write LoginVO.java**

```java
package com.interviewai.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVO {
    private String token;
    private Long userId;
    private String username;
    private String nickname;
}
```

- [ ] **Step 4: Write UserProfileVO.java**

```java
package com.interviewai.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatarUrl;
    private LocalDateTime createdAt;
}
```

---

### Task 7: UserService — Business Logic

**Files:**
- Create: `interview-ai-backend/src/main/java/com/interviewai/service/UserService.java`
- Create: `interview-ai-backend/src/main/java/com/interviewai/service/impl/UserServiceImpl.java`

- [ ] **Step 1: Write UserService.java interface**

```java
package com.interviewai.service;

import com.interviewai.dto.LoginRequest;
import com.interviewai.dto.RegisterRequest;
import com.interviewai.vo.LoginVO;
import com.interviewai.vo.UserProfileVO;

public interface UserService {
    void register(RegisterRequest request);
    LoginVO login(LoginRequest request);
    UserProfileVO getProfile(Long userId);
    void updateProfile(Long userId, String nickname, String email);
}
```

- [ ] **Step 2: Write UserServiceImpl.java**

```java
package com.interviewai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interviewai.common.BizException;
import com.interviewai.common.JwtUtils;
import com.interviewai.common.ResultCode;
import com.interviewai.dto.LoginRequest;
import com.interviewai.dto.RegisterRequest;
import com.interviewai.entity.User;
import com.interviewai.mapper.UserMapper;
import com.interviewai.service.UserService;
import com.interviewai.vo.LoginVO;
import com.interviewai.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void register(RegisterRequest request) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername()); // 默认昵称=用户名
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);

        log.info("新用户注册: {}", request.getUsername());
    }

    @Override
    public LoginVO login(LoginRequest request) {
        // 查用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BizException(ResultCode.USER_PASSWORD_ERROR);
        }
        if (user.getStatus() == 0) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 生成JWT，存入Redis
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        String redisKey = "access_token:" + user.getId();
        redisTemplate.opsForValue().set(redisKey, token, 7, TimeUnit.DAYS);

        log.info("用户登录成功: {}", request.getUsername());
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .build();
    }

    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return UserProfileVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public void updateProfile(Long userId, String nickname, String email) {
        User user = new User();
        user.setId(userId);
        user.setNickname(nickname);
        user.setEmail(email);
        userMapper.updateById(user);
    }
}
```

---

### Task 8: Security Configuration

**Files:**
- Create: `interview-ai-backend/src/main/java/com/interviewai/config/SecurityConfig.java`
- Create: `interview-ai-backend/src/main/java/com/interviewai/config/JwtAuthFilter.java`

- [ ] **Step 1: Write JwtAuthFilter.java**

```java
package com.interviewai.config;

import com.interviewai.common.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器。
 * 从Header提取token → 校验签名和过期 → 比对Redis中的有效token → 注入Security上下文。
 * 为什么存Redis：可以实现强制下线——删除Redis中的token即可让该用户的JWT失效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null && !jwtUtils.isTokenExpired(token)) {
            Long userId = jwtUtils.getUserIdFromToken(token);

            // 检查Redis中是否有此token（支持强制下线）
            String cachedToken = redisTemplate.opsForValue().get("access_token:" + userId);
            if (token.equals(cachedToken)) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 2: Write SecurityConfig.java**

```java
package com.interviewai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/user/register", "/api/v1/user/login").permitAll()
                .requestMatchers("/api/v1/ai/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:80"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

---

### Task 9: UserController

**Files:**
- Create: `interview-ai-backend/src/main/java/com/interviewai/controller/UserController.java`

- [ ] **Step 1: Write UserController.java**

```java
package com.interviewai.controller;

import com.interviewai.common.Result;
import com.interviewai.dto.LoginRequest;
import com.interviewai.dto.RegisterRequest;
import com.interviewai.service.UserService;
import com.interviewai.vo.LoginVO;
import com.interviewai.vo.UserProfileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        LoginVO vo = userService.login(request);
        return Result.ok(vo);
    }

    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(Authentication authentication,
                                      @RequestParam(required = false) String nickname,
                                      @RequestParam(required = false) String email) {
        Long userId = (Long) authentication.getPrincipal();
        userService.updateProfile(userId, nickname, email);
        return Result.ok();
    }
}
```

---

### Task 10: AiController — DeepSeek Smoke Test

**Files:**
- Create: `interview-ai-backend/src/main/java/com/interviewai/controller/AiController.java`

- [ ] **Step 1: Write AiController.java**

```java
package com.interviewai.controller;

import com.interviewai.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI连通性测试。
 * Phase 1 只验证能否正常调用DeepSeek API。
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final ChatClient chatClient;

    @GetMapping("/ping")
    public Result<Map<String, String>> ping() {
        String reply = chatClient.prompt()
                .user("请用一句话介绍你自己")
                .call()
                .content();
        return Result.ok(Map.of("reply", reply));
    }
}
```

---

### Task 11: SQL Scripts — Schema & Seed Data

**Files:**
- Create: `interview-ai-backend/src/main/resources/schema.sql`
- Create: `interview-ai-backend/src/main/resources/data.sql`

- [ ] **Step 1: Write schema.sql (7 tables)**

```sql
-- InterviewAI 数据库建表脚本
-- 注意：username 是登录标识，email 仅用于个人资料展示

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100),
    avatar_url VARCHAR(255),
    role VARCHAR(20) DEFAULT 'USER',
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS resume (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500),
    raw_text TEXT,
    structured_data JSON,
    status VARCHAR(20) DEFAULT 'PROCESSING',
    version INT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS resume_analysis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_id BIGINT NOT NULL UNIQUE,
    skill_score INT,
    description_quality INT,
    keyword_coverage INT,
    format_score INT,
    overall_score INT,
    suggestions JSON,
    strengths JSON,
    weaknesses JSON,
    optimized_resume TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resume_id (resume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS interview_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    resume_id BIGINT,
    mode VARCHAR(30) NOT NULL,
    tech_tags JSON,
    status VARCHAR(20) DEFAULT 'IN_PROGRESS',
    total_questions INT DEFAULT 10,
    answered_count INT DEFAULT 0,
    total_score DECIMAL(5,1),
    strengths JSON,
    weaknesses JSON,
    radar_data JSON,
    duration INT,
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS interview_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    sequence INT NOT NULL,
    role VARCHAR(10) NOT NULL,
    content TEXT NOT NULL,
    question_id BIGINT,
    score INT,
    feedback TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    answer TEXT,
    tags JSON,
    difficulty TINYINT DEFAULT 1,
    embedding_id VARCHAR(100),
    usage_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS job_description (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    jd_text TEXT NOT NULL,
    match_score INT,
    skill_gap JSON,
    suggestions JSON,
    status VARCHAR(20) DEFAULT 'PROCESSING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resume_id (resume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: Write data.sql (10 representative questions — Phase 1 minimal seed, full 100 comes later)**

Due to size, write 10 questions covering different tags. Full 100-question seed will be added in Phase 2 (question bank module). For Phase 1, we need enough data to verify the table works:

```sql
-- 预置面试题种子数据（Phase 1 精简版，10道样本题）
-- 完整100题将在 Phase 2 题库模块完善

INSERT IGNORE INTO question (title, content, answer, tags, difficulty) VALUES
('HashMap的底层实现原理', '请详细说明Java中HashMap的数据结构和put操作的执行流程。', 'HashMap采用数组+链表+红黑树的数据结构。put操作：1.计算key的hashCode并进行高位扰动；2.根据hash值计算数组下标；3.如果该位置为空直接插入；4.如果该位置有值，判断是否为红黑树节点，是则按树的方式插入，否则遍历链表，长度超过8且数组长度≥64时转为红黑树；5.插入后判断是否需要扩容。', '["Java","集合","数据结构"]', 2),
('Spring IoC容器的初始化过程', '描述Spring IoC容器从创建到完成Bean注入的完整流程。', 'IoC容器初始化分为三个大步骤：1.Resource定位——找到配置文件或扫描包；2.BeanDefinition的载入和解析——将配置信息转为BeanDefinition对象；3.BeanDefinition的注册——将BeanDefinition注册到容器中。完成注册后，通过getBean()触发Bean的实例化、属性注入和初始化回调。', '["Spring","IoC","框架"]', 2),
('MySQL索引的最左前缀原则', '什么是索引的最左前缀原则？在实际开发中如何利用它优化查询？', '最左前缀原则指联合索引中，查询条件必须从索引的最左边列开始匹配，且不能跳过中间的列，索引才能被有效使用。例如联合索引(a,b,c)，查询条件为a=? AND c=?时只能用到a列的索引。实际开发中应把区分度高的列放在联合索引前面，查询条件要包含最左列。', '["MySQL","索引","数据库"]', 2),
('Redis缓存穿透、击穿、雪崩的区别和解决方案', '请分别说明这三种缓存问题的定义和各自的解决思路。', '缓存穿透：查询不存在的数据，请求穿透缓存打到DB。解决：布隆过滤器、缓存空值（短TTL）。缓存击穿：热点key过期瞬间大量请求打到DB。解决：互斥锁、逻辑过期（不设物理过期，后台异步刷新）。缓存雪崩：大量key同时过期。解决：过期时间加随机值、多级缓存、限流降级。', '["Redis","缓存","中间件"]', 3),
('synchronized和ReentrantLock的区别', '请从实现机制、功能特性、性能等角度对比这两种锁。', 'synchronized是JVM层面的关键字，自动加锁释放锁，非公平锁，不可中断。ReentrantLock是JDK层面的API，需要手动unlock（通常放finally中），支持公平锁/非公平锁、可中断、可超时、可绑定多个Condition。性能方面JDK6后synchronized做了大量优化（偏向锁、轻量级锁），两者差距已很小。优先用synchronized，需要高级特性时用ReentrantLock。', '["Java","并发","JUC"]', 2),
('JVM垃圾回收算法有哪些', '列举主要的垃圾回收算法及其适用场景。', '标记-清除：最基础，产生内存碎片。标记-复制：将内存分为两块，存活对象复制到另一块，无碎片但浪费空间，适合新生代（对象死亡率高）。标记-整理：存活对象向一端移动，无碎片，适合老年代。分代收集：新生代用复制算法，老年代用标记-清除或标记-整理。', '["JVM","GC","性能"]', 1),
('Spring Boot自动配置原理', 'Spring Boot是如何实现自动配置的？@SpringBootApplication注解做了什么？', '@SpringBootApplication包含@EnableAutoConfiguration，它通过@Import导入AutoConfigurationImportSelector。该Selector会从META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports文件中读取所有自动配置类，然后根据@Conditional系列条件注解判断是否生效。条件包括：classpath中是否有某个类、是否存在某个Bean、配置属性值等。', '["Spring","SpringBoot","框架"]', 2),
('分布式锁的实现方案对比', '请对比基于Redis和基于ZooKeeper的分布式锁实现，说明各自的优缺点。', 'Redis方案：使用SET NX EX命令，加上唯一标识防止误删。优点是性能高、部署简单；缺点是锁有过期风险（需看门狗机制续期）、主从切换可能丢锁。Redisson的RedLock算法可以缓解。ZooKeeper方案：使用临时顺序节点+Watch机制。优点是可靠性高、自动释放、可重入；缺点是性能较低、部署维护成本高。选型：对性能要求高选Redis，对一致性要求极高选ZK。', '["Redis","分布式","中间件"]', 3),
('TCP三次握手和四次挥手', '详细描述TCP连接建立和断开的过程，为什么握手是三次而挥手是四次？', '三次握手：1.客户端发送SYN=1，seq=x；2.服务端回复SYN=1，ACK=1，seq=y，ack=x+1；3.客户端发送ACK=1，seq=x+1，ack=y+1。为什么三次？防止已失效的连接请求到达服务端导致资源浪费。四次挥手：1.客户端FIN；2.服务端ACK；3.服务端FIN；4.客户端ACK后等待2MSL。为什么四次？因为TCP是全双工的，服务端收到FIN后可能还有数据要发送，ACK和FIN不能合并。', '["网络","TCP/IP","计算机基础"]', 1),
('微服务架构中服务间通信方式', '请介绍微服务中常用的同步和异步通信方式，以及各自的适用场景。', '同步通信：REST/HTTP（简单通用、跨语言）、gRPC（高性能、强类型、适合内部服务间调用）。异步通信：消息队列如RabbitMQ（解耦、削峰、可靠性高）、Kafka（高吞吐、适合日志/事件流）。选型原则：需要立即响应用同步，可以延迟处理的用异步；核心业务链路用同步+熔断兜底，非核心用异步削峰。', '["微服务","架构","SpringCloud"]', 2);
```

---

### Task 12: Initialize Vue3 Frontend Project

**Files:** Scaffold via `npm create vite`, then configure

- [ ] **Step 1: Scaffold Vue3 + TS project**

```bash
cd "D:/claude-code-workspace/AI求职助手"
npm create vite@latest interview-ai-frontend -- --template vue-ts
cd interview-ai-frontend
npm install
```

- [ ] **Step 2: Install additional dependencies**

```bash
npm install vue-router@4 pinia axios element-plus @element-plus/icons-vue echarts
npm install -D @types/node
```

- [ ] **Step 3: Write vite.config.ts**

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

- [ ] **Step 4: Configure tsconfig.app.json (add path alias)**

```json
{
  "compilerOptions": {
    "tsBuildInfoFile": "./node_modules/.tmp/tsconfig.app.tsbuildinfo",
    "target": "ES2020",
    "useDefineForEmits": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "verbatimModuleSyntax": true,
    "noEmit": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "jsx": "preserve",
    "strict": true,
    "noUnusedLocals": false,
    "noUnusedParameters": false,
    "noFallthroughCasesInSwitch": true,
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.tsx", "src/**/*.vue", "env.d.ts"]
}
```

---

### Task 13: Frontend — Types, Axios, API

**Files:**
- Create: `interview-ai-frontend/src/types/user.ts`
- Create: `interview-ai-frontend/src/api/request.ts`
- Create: `interview-ai-frontend/src/api/user.ts`

- [ ] **Step 1: Write types/user.ts**

```typescript
export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
}

export interface LoginVO {
  token: string
  userId: number
  username: string
  nickname: string
}

export interface UserProfile {
  id: number
  username: string
  nickname: string
  email: string
  avatarUrl: string
  createdAt: string
}

// 统一后端响应结构
export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
}
```

- [ ] **Step 2: Write api/request.ts — Axios wrapper**

```typescript
import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types/user'

const request: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
})

// 请求拦截器：自动加Token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      // 401 → 清空token跳转登录
      if (res.code === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.msg))
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    ElMessage.error('网络错误，请检查网络连接')
    return Promise.reject(error)
  }
)

export default request
```

- [ ] **Step 3: Write api/user.ts**

```typescript
import request from './request'
import type { ApiResponse, LoginRequest, LoginVO, RegisterRequest, UserProfile } from '@/types/user'

export const userApi = {
  register(data: RegisterRequest) {
    return request.post<ApiResponse<null>>('/user/register', data)
  },

  login(data: LoginRequest) {
    return request.post<ApiResponse<LoginVO>>('/user/login', data)
  },

  getProfile() {
    return request.get<ApiResponse<UserProfile>>('/user/profile')
  },

  updateProfile(nickname?: string, email?: string) {
    return request.put<ApiResponse<null>>('/user/profile', null, {
      params: { nickname, email },
    })
  },
}
```

---

### Task 14: Frontend — Router & Pinia Store

**Files:**
- Create: `interview-ai-frontend/src/router/index.ts`
- Create: `interview-ai-frontend/src/stores/user.ts`

- [ ] **Step 1: Write router/index.ts**

```typescript
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/views/Layout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/Dashboard.vue'),
          meta: { title: '仪表盘' },
        },
      ],
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { guest: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue'),
      meta: { guest: true },
    },
  ],
})

// 路由守卫：未登录跳转登录页，已登录访问guest页跳转首页
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.guest && token) {
    next('/dashboard')
  } else if (!to.meta.guest && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
```

- [ ] **Step 2: Write stores/user.ts — Pinia**

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { userApi } from '@/api/user'
import type { UserProfile } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  const profile = ref<UserProfile | null>(null)
  const isLoggedIn = ref(!!localStorage.getItem('token'))

  async function fetchProfile() {
    try {
      const res = await userApi.getProfile()
      profile.value = res.data.data
    } catch {
      // 获取失败不做处理，由拦截器统一处理401
    }
  }

  function logout() {
    localStorage.removeItem('token')
    profile.value = null
    isLoggedIn.value = false
  }

  return { profile, isLoggedIn, fetchProfile, logout }
})
```

---

### Task 15: Frontend — Login & Register Pages

**Files:**
- Create: `interview-ai-frontend/src/views/Login.vue`
- Create: `interview-ai-frontend/src/views/Register.vue`

- [ ] **Step 1: Write Login.vue**

```vue
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await userApi.login(form)
    const { token } = res.data.data
    localStorage.setItem('token', token)
    userStore.isLoggedIn = true
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="title">InterviewAI</h1>
      <p class="subtitle">AI 求职备战平台</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password
            prefix-icon="Lock" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="footer-link">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.title {
  text-align: center;
  font-size: 28px;
  color: #303133;
  margin-bottom: 4px;
}
.subtitle {
  text-align: center;
  color: #909399;
  margin-bottom: 32px;
  font-size: 14px;
}
.login-btn {
  width: 100%;
}
.footer-link {
  text-align: center;
  font-size: 14px;
  color: #909399;
}
.footer-link a {
  color: #667eea;
  text-decoration: none;
}
</style>
```

- [ ] **Step 2: Write Register.vue**

```vue
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度3-20个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度6-30个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userApi.register({
      username: form.username,
      password: form.password,
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-container">
    <div class="register-card">
      <h1 class="title">创建账号</h1>
      <p class="subtitle">加入 InterviewAI，开启 AI 驱动的面试备战</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password
            @keyup.enter="handleRegister" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="register-btn" @click="handleRegister">
            注 册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="footer-link">
        已有账号？<router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.register-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.title {
  text-align: center;
  font-size: 24px;
  color: #303133;
  margin-bottom: 4px;
}
.subtitle {
  text-align: center;
  color: #909399;
  margin-bottom: 32px;
  font-size: 14px;
}
.register-btn {
  width: 100%;
}
.footer-link {
  text-align: center;
  font-size: 14px;
  color: #909399;
}
.footer-link a {
  color: #667eea;
  text-decoration: none;
}
</style>
```

---

### Task 16: Frontend — Layout Shell & Dashboard Placeholder

**Files:**
- Write: `interview-ai-frontend/src/App.vue` (replace scaffolded)
- Create: `interview-ai-frontend/src/views/Layout.vue`
- Create: `interview-ai-frontend/src/views/Dashboard.vue`
- Write: `interview-ai-frontend/src/main.ts` (update)

- [ ] **Step 1: Update main.ts**

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
```

- [ ] **Step 2: Update App.vue**

```vue
<script setup lang="ts">
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
if (userStore.isLoggedIn) {
  userStore.fetchProfile()
}
</script>

<template>
  <router-view />
</template>
```

- [ ] **Step 3: Write Layout.vue**

```vue
<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout-container">
    <el-header class="app-header">
      <div class="header-left">
        <span class="logo">InterviewAI</span>
      </div>
      <div class="header-right">
        <span class="username">{{ userStore.profile?.nickname || '用户' }}</span>
        <el-button text type="danger" @click="handleLogout">退出</el-button>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.layout-container {
  min-height: 100vh;
  background: #f5f7fa;
}
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
}
.logo {
  font-size: 20px;
  font-weight: 700;
  color: #667eea;
}
.username {
  margin-right: 12px;
  color: #606266;
}
</style>
```

- [ ] **Step 4: Write Dashboard.vue (placeholder)**

```vue
<script setup lang="ts">
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<template>
  <div class="dashboard">
    <h2>欢迎回来，{{ userStore.profile?.nickname || '用户' }}</h2>
    <p class="desc">你的 AI 求职备战平台已就绪。后续版本将在此展示面试统计和趋势图表。</p>
    <el-row :gutter="20" style="margin-top: 24px">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>总面试次数</template>
          <div class="stat-num">0</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>平均得分</template>
          <div class="stat-num">--</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>最高得分</template>
          <div class="stat-num">--</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>连续练习天数</template>
          <div class="stat-num">0</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}
.dashboard h2 {
  margin: 0;
  font-size: 22px;
}
.desc {
  color: #909399;
  margin-top: 8px;
}
.stat-num {
  font-size: 32px;
  font-weight: 700;
  color: #667eea;
}
</style>
```

---

### Task 17: Cleanup & Final Verification

- [ ] **Step 1: Remove Vite scaffold boilerplate files**

```bash
cd interview-ai-frontend/src
rm -f components/HelloWorld.vue style.css
# Keep style.css if needed, but clean defaults
```

- [ ] **Step 2: Ensure index.html title**

```html
<title>InterviewAI - AI求职备战平台</title>
```

- [ ] **Step 3: Verify frontend compiles**

```bash
cd interview-ai-frontend
npm run build
```

Expected: No errors.

- [ ] **Step 4: Verify backend compiles**

```bash
cd interview-ai-backend
mvn compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit (if git)**

```bash
cd "D:/claude-code-workspace/AI求职助手"
git init
git add -A
git commit -m "feat: Phase 1 foundation — project skeleton, user auth, AI smoke test"
```

---

## Self-Review Summary

**Spec Coverage:**
- ✅ Spring Boot project skeleton → Task 1
- ✅ MySQL schema (7 tables) → Task 11
- ✅ MyBatis-Plus entities & mappers → Task 3, 4
- ✅ User register/login with JWT → Task 5, 6, 7, 8, 9
- ✅ Vue3 frontend skeleton → Task 12, 13, 14
- ✅ Login/Register pages → Task 15
- ✅ Layout + Dashboard → Task 16
- ✅ SpringAI + DeepSeek smoke test → Task 10

**Type Consistency Check:**
- `RegisterRequest.username/password` matches usage in `UserServiceImpl.register()`
- `LoginRequest.username/password` matches usage in `UserServiceImpl.login()`
- `LoginVO` fields match what `login()` returns and what `api/user.ts` expects
- JWT `Authentication.getPrincipal()` returns `Long userId` — consistent across controller and service
- Frontend types match backend VO structures
