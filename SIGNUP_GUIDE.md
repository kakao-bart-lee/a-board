# 회원가입 및 인증 API 가이드 (Frontend)

이 문서는 A-Board 프로젝트의 이메일 기반 회원가입, 이메일 인증, 그리고 로그인(토큰 발급) 기능에 대한 프론트엔드 개발 가이드입니다.

## 전체 인증 흐름

사용자는 다음 순서에 따라 인증을 완료하고 API를 사용합니다.

1.  **회원가입**: 사용자가 이메일, 비밀번호 등 개인정보를 입력하여 가입을 요청합니다.
2.  **인증 코드 확인**: 시스템은 사용자의 이메일로 인증 코드를 발송합니다. (현재 구현에서는 서버 콘솔에 로그로 출력됩니다.)
    *   만약 이메일을 받지 못했거나 코드가 만료(6시간)된 경우, **인증 코드 재전송**을 요청할 수 있습니다.
3.  **이메일 인증**: 사용자는 이메일로 받은 인증 코드를 입력하여 계정을 활성화합니다.
4.  **로그인**: 활성화된 계정의 이메일과 비밀번호로 로그인을 요청합니다.
5.  **JWT 토큰 획득**: 로그인 성공 시, 서버로부터 JWT(JSON Web Token)를 발급받습니다.
6.  **인증된 요청**: 발급받은 JWT를 사용하여 인증이 필요한 API를 ��출합니다.

---

## API 엔드포인트 상세

### 1. 회원가입

-   **Endpoint**: `POST /auth/signup`
-   **설명**: 새로운 사용자를 시스템에 등록하고 인증 이메일을 발송합니다.
-   **주요 정책**:
    *   **미인증 사용자 재가입**: 만약 입력된 이메일이 이미 존재하지만 **인증되지 않은 상태**라면, 기존 계정 정보는 삭제되고 새로운 정보로 가입 절차가 다시 진행됩니다.
    *   **인증 완료 사용자 가입 시도**: 이미 가입 및 인증을 완료한 이메일로는 다시 가입할 수 없습니다.
-   **Headers**:
    ```json
    {
      "Content-Type": "application/json"
    }
    ```
-   **Request Body**:
    ```json
    {
      "name": "홍길동",
      "email": "user@example.com",
      "password": "password123",
      "gender": "MALE",
      "birthYear": 1995,
      "profileImageUrls": ["url1", "url2"],
      "location": "서울",
      "preferredLanguage": "한국어",
      "aboutMe": "안녕하세요"
    }
    ```
    *   `name`, `email`, `password`, `gender`, `birthYear`는 필수 항목입니다.
-   **Success Response (201 Created)**:
    *   생성된 사용자 정보를 반환합니다. (비밀번호, 인증 코드는 제외)
    ```json
    {
      "id": "a1b2c3d4-e5f6-...",
      "name": "홍길동",
      "email": "user@example.com",
      // ... other fields
      "verified": false
    }
    ```
-   **Error Response**:
    *   `400 Bad Request`: 요청 형식이 잘못되었을 경우.
    *   `500 Internal Server Error`: **인증 완료된** 이메일이 이미 존재할 경우. (향후 `409 Conflict`로 개선될 수 있음)

### 2. 이메일 인증

-   **Endpoint**: `POST /auth/verify`
-   **설명**: 이메일로 받은 인증 코드를 확인하여 사용자 계정을 활성화합니다. 인증 코드는 발급 후 **6시간** 동안 유효합니다.
-   **Headers**:
    ```json
    {
      "Content-Type": "application/json"
    }
    ```
-   **Request Body**:
    ```json
    {
      "email": "user@example.com",
      "code": "123456"
    }
    ```
-   **Success Response (200 OK)**:
    *   Response Body 없이 상태 코드만 반환됩니다.
-   **Error Response**:
    *   `400 Bad Request`: 이메일 또는 인증 코드가 일치하지 않거나, 코드가 만료되었을 경우.

### 3. 인증 이메일 재전송

-   **Endpoint**: `POST /auth/resend-verification`
-   **설명**: 미인증 사용자를 위해 새로운 인증 코드를 이메일로 다시 발송합니다.
-   **주요 정책**:
    *   **쿨타임(Cooldown)**: 마지��� 이메일 발송 후 **60초**가 지나야 다시 요청할 수 있습니다.
-   **Headers**:
    ```json
    {
      "Content-Type": "application/json"
    }
    ```
-   **Request Body**:
    ```json
    {
      "email": "user@example.com"
    }
    ```
-   **Success Response (200 OK)**:
    *   요청이 성공적으로 처리되었음을 의미합니다.
    *   **참고**: 사용자 존재 여부를 노출하지 않기 위해, 해당 이메일이 존재하지 않거나 이미 인증된 경우에도 `200 OK`를 반환합니다.
-   **Error Response**:
    *   `429 Too Many Requests`: 60초 쿨타임이 지나지 않았을 경우.

### 4. 로그인 (토큰 발급)

-   **Endpoint**: `POST /auth/token`
-   **설명**: 이메일과 비밀번호로 사용자를 인증하고 API 접근을 위한 JWT를 발급합니다.
-   **Headers**:
    ```json
    {
      "Content-Type": "application/json"
    }
    ```
-   **Request Body**:
    ```json
    {
      "email": "user@example.com",
      "password": "password123"
    }
    ```
-   **Success Response (200 OK)**:
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOi..."
    }
    ```
-   **Error Response**:
    *   `401 Unauthorized`: 이메일이 존재하지 않거나 비밀번호가 틀렸을 경우.
    *   `403 Forbidden`: 사용자가 이메일 인증을 완료하지 않았을 경우.

---

## JWT 토큰 사용법

로그인 성공 후 받은 토큰은 이후 모든 인증된 API 요청의 `Authorization` 헤더에 포함시켜야 합니다.

-   **저장 위치**:
    *   보안을 고려하여 JavaScript로 접근이 불가능한 `HttpOnly` 쿠키에 저장하는 것이 가장 안전하지만, 클라이언트-서버 구현 복잡도가 증가합니다.
    *   대안으로, 브라우저의 `localStorage`나 `sessionStorage`에 저장할 수 있습니다. (XSS 공격에 취약할 수 있으므로 주의 필요)
-   **요청 헤더 형식**:
    ```
    Authorization: Bearer <your-jwt-token>
    ```
    **예시)**
    ```
    Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOi...
    ```

이 헤더를 포함하여 `GET /posts`와 같은 인증이 필요한 엔드포인트에 요청을 보낼 수 있습니다.