# API Documentation — Attendance Service

This document outlines the API endpoints, request/response models, and cURL examples for the Attendance service.

---

## Base Configuration

* **Base URL (via Nginx)**: `http://localhost/api/v1/attendance`
* **Content-Type**: `application/json`
* **Authorization Header Required**: `Authorization: Bearer <JWT_TOKEN>`

### Role Permissions
| Method | Endpoint | Allowed Roles |
| :--- | :--- | :--- |
| `POST` | `/` | `ADMIN`, `SUPERVISOR` |
| `POST` | `/bulk` | `ADMIN`, `SUPERVISOR` |
| `GET` | `/{id}` | `ADMIN`, `SUPERVISOR` |
| `GET` | `/` | `ADMIN`, `SUPERVISOR` |
| `GET` | `/range` | `ADMIN`, `SUPERVISOR` |
| `PUT` | `/{id}` | `ADMIN`, `SUPERVISOR` |
| `DELETE` | `/{id}` | `ADMIN` (only) |

---

## Data Models

### 1. Request Payload (`AttendanceRequest`)
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `employeeId` | String | Yes | Unique ID (UUID) of the employee. |
| `timestamp` | ISO DateTime | Yes | Date and time of the attendance (e.g. `2026-06-24T08:00:00`). |
| `calendarId` | String | No | External reference calendar identifier if applicable. |
| `noOfTrees` | Integer | No | The count of rubber trees assigned or tapped. |
| `noWork` | String | No | Reason code if no work was performed. Expected values: `none` (default), `rain`, `ill`, `no_loads`, `holiday`. |

### 2. Response Payload (`AttendanceResponse`)
| Field | Type | Description |
| :--- | :--- | :--- |
| `attendanceId` | String (UUID) | Unique identifier for the recorded attendance record. |
| `employeeId` | String (UUID) | Linked employee ID. |
| `employeeName` | String | Cached/joined name of the employee. |
| `calendarId` | String | External reference calendar ID (null if not specified). |
| `timestamp` | ISO DateTime | When the attendance occurred. |
| `noOfTrees` | Integer | Tapped trees count (null if not specified). |
| `noWork` | String | Reason code for no work (`none`, `rain`, `ill`, etc.). |
| `createdAt` | ISO DateTime | Timestamp when this record was persisted. |

---

## Endpoints

### 1. Record Attendance (Single)
Creates a single daily attendance record for an employee.

* **Endpoint**: `/`
* **Method**: `POST`

**Request Example**:
```json
{
  "employeeId": "e5c26b9a-4127-4a6c-941f-cb7127e1f40d",
  "timestamp": "2026-06-24T07:30:00",
  "noOfTrees": 150,
  "noWork": "none"
}
```

**Response Example (HTTP 201 Created)**:
```json
{
  "attendanceId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "employeeId": "e5c26b9a-4127-4a6c-941f-cb7127e1f40d",
  "employeeName": "Sunil Perera",
  "calendarId": null,
  "timestamp": "2026-06-24T07:30:00",
  "noOfTrees": 150,
  "noWork": "none",
  "createdAt": "2026-06-24T07:32:05"
}
```

**cURL Example**:
```bash
curl -X POST http://localhost/api/v1/attendance \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"employeeId":"e5c26b9a-4127-4a6c-941f-cb7127e1f40d", "timestamp":"2026-06-24T07:30:00", "noOfTrees":150, "noWork":"none"}'
```

---

### 2. Record Attendance (Bulk)
Enables supervisors to submit attendance logs for multiple workers at once.

* **Endpoint**: `/bulk`
* **Method**: `POST`

**Request Example (`AttendanceBulkRequest`)**:
```json
{
  "attendances": [
    {
      "employeeId": "e5c26b9a-4127-4a6c-941f-cb7127e1f40d",
      "timestamp": "2026-06-24T07:30:00",
      "noOfTrees": 150,
      "noWork": "none"
    },
    {
      "employeeId": "f7d37c0b-5238-5b7d-052g-dc8238f2g50e",
      "timestamp": "2026-06-24T07:30:00",
      "noOfTrees": 0,
      "noWork": "rain"
    }
  ]
}
```

**Response Example (HTTP 201 Created)**:
Returns a JSON array of the created `AttendanceResponse` records.

---

### 3. Get Attendance Detail
Retrieve details of a specific attendance record.

* **Endpoint**: `/{id}`
* **Method**: `GET`

**Response Example (HTTP 200 OK)**:
```json
{
  "attendanceId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "employeeId": "e5c26b9a-4127-4a6c-941f-cb7127e1f40d",
  "employeeName": "Sunil Perera",
  "calendarId": null,
  "timestamp": "2026-06-24T07:30:00",
  "noOfTrees": 150,
  "noWork": "none",
  "createdAt": "2026-06-24T07:32:05"
}
```

---

### 4. List All Attendance (Paginated)
Fetch all attendance records in the database, paginated.

* **Endpoint**: `/`
* **Method**: `GET`
* **Query Parameters**:
  * `page` (Integer) — Zero-based page index (defaults to `0`).
  * `size` (Integer) — Elements per page (defaults to `20`).
  * `sort` (String) — Sort criteria format: `property(,asc\|desc)` (e.g. `timestamp,desc`).

**Response Example (HTTP 200 OK)**:
```json
{
  "content": [
    {
      "attendanceId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "employeeId": "e5c26b9a-4127-4a6c-941f-cb7127e1f40d",
      "employeeName": "Sunil Perera",
      "timestamp": "2026-06-24T07:30:00",
      "noOfTrees": 150,
      "noWork": "none",
      "createdAt": "2026-06-24T07:32:05"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### 5. Filter Attendance by Date Range
Fetch all attendance records matching a specified datetime range (non-paginated list).

* **Endpoint**: `/range`
* **Method**: `GET`
* **Query Parameters** (Required, ISO-formatted):
  * `from` — Start datetime (e.g., `2026-06-20T00:00:00`)
  * `to` — End datetime (e.g., `2026-06-25T23:59:59`)

**cURL Example**:
```bash
curl -X GET "http://localhost/api/v1/attendance/range?from=2026-06-20T00:00:00&to=2026-06-25T23:59:59" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

---

### 6. Update Attendance
Modify an existing attendance entry.

* **Endpoint**: `/{id}`
* **Method**: `PUT`

**Request Example**:
```json
{
  "employeeId": "e5c26b9a-4127-4a6c-941f-cb7127e1f40d",
  "timestamp": "2026-06-24T07:30:00",
  "noOfTrees": 180,
  "noWork": "none"
}
```

**Response Example (HTTP 200 OK)**:
Returns the updated `AttendanceResponse` model containing the changes.

---

### 7. Delete Attendance
Removes an attendance record. **Only users with the `ADMIN` role can execute this action**.

* **Endpoint**: `/{id}`
* **Method**: `DELETE`

**Response Example (HTTP 204 No Content)**:
*(Empty Body)*

---

## Common Error Codes
* **HTTP 400 Bad Request**: If validation constraints fail (e.g., empty `employeeId` or missing `timestamp`), or if `noWork` value does not match one of the allowed parameters.
* **HTTP 401 Unauthorized**: Missing or expired JWT token.
* **HTTP 403 Forbidden**: Trying to access `DELETE` endpoint with `ROLE_SUPERVISOR` context.
* **HTTP 404 Not Found**: If the specified record `id` or `employeeId` does not exist in the database.
