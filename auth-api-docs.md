# API Documentation — Authentication Service

This document outlines the API endpoints, request/response models, and instructions for connecting a frontend login page to the Estate ERP authentication service.

All requests should be routed through the Nginx reverse proxy running on port `80` (or `443` in production).

---

## Base Configuration

* **Local Base URL (via Nginx)**: `http://localhost/api/v1/auth`
* **Content-Type**: `application/json`
* **Authorization Scheme**: `Bearer <JWT_TOKEN>` (passed via the `Authorization` request header for secure endpoints)

---

## 1. User Login

Authenticates user credentials and generates a temporary JWT access token.

* **Endpoint**: `/login`
* **Method**: `POST`
* **Authentication**: None (Public)

### Request Payload (`LoginRequest`)
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `username` | String | Yes | Non-empty | User's system username |
| `password` | String | Yes | Non-empty | User's plaintext password |

**Example Request Body**:
```json
{
  "username": "admin",
  "password": "password123"
}
```

### Response Payload (`LoginResponse`)
| Field | Type | Description |
| :--- | :--- | :--- |
| `token` | String | The JWT authentication token (used for subsequent requests) |
| `tokenType` | String | Always `Bearer` |
| `expiresIn` | Long | Expiration time of the token in milliseconds (e.g., 24 hours = `86400000`) |
| `username` | String | Username of the authenticated user |
| `role` | String | The user's role (e.g., `ROLE_ADMIN`, `ROLE_SUPERVISOR`) |

**Example Response Body (HTTP 200 OK)**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTcwMDAwMDAwMCwiZXhwIjoxNzAwMDg2NDAwfQ.xyz...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "admin",
  "role": "ROLE_ADMIN"
}
```

### Errors
* **HTTP 401 Unauthorized**: If username or password is incorrect.
* **HTTP 400 Bad Request**: If request fields are empty or invalid.

**cURL Example**:
```bash
curl -X POST http://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin", "password":"password123"}'
```

---

## 2. Get Current User Profile (`/me`)

Retrieves profile details of the currently logged-in user context.

* **Endpoint**: `/me`
* **Method**: `GET`
* **Authentication**: **Required** (`Authorization: Bearer <token>`)

### Request Headers
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG...
```

### Response Payload (`UserResponse`)
| Field | Type | Description |
| :--- | :--- | :--- |
| `userId` | String (UUID) | Unique identifier for the user account |
| `username` | String | System username |
| `email` | String | Contact email address |
| `role` | String | Assigned authorization role |
| `employeeId` | String (UUID) | ID of the linked employee record (null if administrative-only user) |
| `createdAt` | DateTime | Timestamp when the user account was created |

**Example Response Body (HTTP 200 OK)**:
```json
{
  "userId": "d748f2b8-a6d1-443b-87b6-12a1f1b2c3d4",
  "username": "admin",
  "email": "admin@madukotawatte.com",
  "role": "ROLE_ADMIN",
  "employeeId": null,
  "createdAt": "2026-06-20T12:00:00"
}
```

### Errors
* **HTTP 401 Unauthorized**: If the JWT token is missing, expired, or invalid.

**cURL Example**:
```bash
curl -X GET http://localhost/api/v1/auth/me \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

---

## 3. Register User

Creates a new system user profile. **Only accessible by administrators**.

* **Endpoint**: `/register`
* **Method**: `POST`
* **Authentication**: **Required** (`Authorization: Bearer <token>` + **Admin Role**)

### Request Payload (`RegisterRequest`)
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `username` | String | Yes | 3 to 50 chars | Target username |
| `email` | String | Yes | Valid email format | Contact email address |
| `password` | String | Yes | Min 8 chars | Target password |
| `role` | String | No | Must match database role | Defaults to `ROLE_SUPERVISOR` if omitted |
| `employeeId` | String | No | UUID of existing employee | Optional link to an employee profile |

**Example Request Body**:
```json
{
  "username": "supervisor_john",
  "email": "john@madukotawatte.com",
  "password": "securepassword123",
  "role": "ROLE_SUPERVISOR",
  "employeeId": "e5c26b9a-4127-4a6c-941f-cb7127e1f40d"
}
```

### Response Payload (`UserResponse`)
Returns the created user's profile metadata. (Matches the format of `/me` endpoint).

### Errors
* **HTTP 400 Bad Request**: If fields are missing or constraints are violated.
* **HTTP 409 Conflict**: If the username or email is already taken.
* **HTTP 403 Forbidden**: If the authenticated user does not have the `ROLE_ADMIN` role.

---

## 4. Change Password

Updates the password of the currently authenticated user.

* **Endpoint**: `/change-password`
* **Method**: `PUT`
* **Authentication**: **Required** (`Authorization: Bearer <token>`)

### Request Payload (`ChangePasswordRequest`)
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `currentPassword` | String | Yes | Non-empty | User's active password |
| `newPassword` | String | Yes | Min 8 chars | Desired replacement password |

**Example Request Body**:
```json
{
  "currentPassword": "password123",
  "newPassword": "newsecurepassword456"
}
```

### Response (HTTP 204 No Content)
Empty response body indicating success.

### Errors
* **HTTP 400 Bad Request**: If the current password is incorrect or the new password fails constraints.
* **HTTP 401 Unauthorized**: Missing or invalid session token.

---

## Frontend Integration Lifecycle (React Example)

When building the login page in your React/Vite/Next.js frontend:

1. **Submit Credentials**: Use `fetch` or `axios` to submit `username` and `password` to `http://localhost/api/v1/auth/login`.
2. **Store Session**: On a `200 OK` response, store the returned `token` in `localStorage` or `sessionStorage`.
3. **Configure API Client**: Set up your Axios/Fetch interceptor to attach the header:
   ```javascript
   headers['Authorization'] = `Bearer ${storedToken}`;
   ```
4. **Load Profile**: Call `GET http://localhost/api/v1/auth/me` to get the logged-in user's role and details to manage routing or state.
