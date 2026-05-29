# Tasks: 工具上传功能优化 - 多文件支持

**Input**: Design documents from `/specs/002-multi-file-upload/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic configuration

- [x] T001 [P] Create `uploads/tools/` directory for file storage in `backend/`
- [x] T002 [P] Configure file upload limits in `backend/src/main/resources/application.yml`
- [x] T003 [P] Add file type allowlist configuration for supported extensions

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Backend infrastructure for file management

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Create ToolFile entity model in `backend/src/main/java/com/iaihub/toolbox/model/ToolFile.java`
- [x] T005 Create ToolFileRepository in `backend/src/main/java/com/iaihub/toolbox/repository/ToolFileRepository.java`
- [x] T006 Create ToolFileDTO in `backend/src/main/java/com/iaihub/toolbox/dto/ToolFileDTO.java`
- [x] T007 [P] Create ToolFileService in `backend/src/main/java/com/iaihub/toolbox/service/ToolFileService.java`
- [x] T008 [P] Create ToolFileController in `backend/src/main/java/com/iaihub/toolbox/controller/ToolFileController.java`
- [x] T009 Create file upload API endpoint: `POST /api/v1/tools/{toolId}/files`
- [x] T010 Create file list API endpoint: `GET /api/v1/tools/{toolId}/files`
- [x] T011 Create file download API endpoint: `GET /api/v1/tools/{toolId}/files/{fileId}`
- [x] T012 Create file delete API endpoint: `DELETE /api/v1/tools/{toolId}/files/{fileId}`
- [x] T013 Modify ToolService to cleanup files when deleting tool in `backend/src/main/java/com/iaihub/toolbox/service/ToolService.java`

**Checkpoint**: Foundational ready - file storage backend is complete

---

## Phase 3: User Story 1 - 多文件选择上传 (Priority: P1) 🎯 MVP

**Goal**: 用户可以在工具上传页面一次性选择多个文件进行上传

**Independent Test**: 在上传页面选择多个文件并提交，验证文件是否正确上传到对应的工具文件夹

### Implementation for User Story 1

- [x] T014 [P] Add ToolFile related TypeScript types in `frontend/src/types/index.ts`
- [x] T015 [P] Add file upload API methods in `frontend/src/services/api.ts`
- [x] T016 [US1] Modify UploadPage.vue to add el-upload component for multi-file selection
- [x] T017 [US1] Implement file list display showing selected files with name and size
- [x] T018 [US1] Implement remove file from list functionality
- [x] T019 [US1] Integrate file upload with tool creation flow (create tool first, then upload files)

**Checkpoint**: User Story 1 should be fully functional - multiple file selection and upload works

---

## Phase 4: User Story 2 - README文档与文件关联存储 (Priority: P1)

**Goal**: 将用户填写的README内容和上传的工具文件存储在同一文件夹中

**Independent Test**: 上传工具后，检查该工具文件夹中是否同时包含所有上传的文件和README文档

### Implementation for User Story 2

- [ ] T020 [US2] Modify backend to save README content as readme.md in tool folder
- [ ] T021 [US2] Add `readme` field to file upload API form data
- [ ] T022 [US2] Modify UploadPage.vue to include readme content in upload request

**Checkpoint**: User Story 2 should be fully functional - README stored alongside uploaded files

---

## Phase 5: User Story 3 - 上传进度与状态反馈 (Priority: P2)

**Goal**: 用户可以实时看到文件上传的进度和最终结果状态

**Independent Test**: 上传多个文件时，观察进度指示器是否正确显示

### Implementation for User Story 3

- [ ] T023 [US3] Implement upload progress tracking in frontend using axios onUploadProgress
- [ ] T024 [US3] Display progress bar during file upload in UploadPage.vue
- [ ] T025 [US3] Add success/failure status messages using ElMessage

**Checkpoint**: User Story 3 should be fully functional - user sees upload progress and status

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T026 [P] Add file size validation (max 50MB per file, 200MB total)
- [ ] T027 [P] Add file type validation against allowlist
- [ ] T028 [P] Add error handling for duplicate filenames
- [ ] T029 Handle empty README case (decide whether to create empty readme.md)
- [ ] T030 [P] Run quickstart.md validation to verify implementation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Can run in parallel with US1
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - Can run in parallel with US1/US2

### Within Each User Story

- Models before services
- Services before controllers
- Controllers before frontend integration
- Story complete before moving to next priority

### Parallel Opportunities

- T001, T002, T003 can run in parallel (Setup phase)
- T004, T005, T006 can run in parallel (Entity, Repository, DTO)
- T007, T008 can run in parallel (Service, Controller)
- T014, T015 can run in parallel (Frontend types, API)
- T016, T017, T018 can run in parallel (US1 frontend components)
- T026, T027, T028 can run in parallel (Polish tasks)

---

## Parallel Example: User Story 1

```bash
# Launch all frontend tasks for User Story 1 together:
Task: "Add ToolFile related TypeScript types in frontend/src/types/index.ts"
Task: "Add file upload API methods in frontend/src/services/api.ts"

# Then after both complete:
Task: "Modify UploadPage.vue to add el-upload component for multi-file selection"
Task: "Implement file list display showing selected files with name and size"
Task: "Implement remove file from list functionality"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Polish phase → Final validation

---

## File Paths Summary

### Backend New Files

| File | Path |
|------|------|
| ToolFile.java | `backend/src/main/java/com/iaihub/toolbox/model/ToolFile.java` |
| ToolFileRepository.java | `backend/src/main/java/com/iaihub/toolbox/repository/ToolFileRepository.java` |
| ToolFileDTO.java | `backend/src/main/java/com/iaihub/toolbox/dto/ToolFileDTO.java` |
| ToolFileService.java | `backend/src/main/java/com/iaihub/toolbox/service/ToolFileService.java` |
| ToolFileController.java | `backend/src/main/java/com/iaihub/toolbox/controller/ToolFileController.java` |

### Backend Modified Files

| File | Path |
|------|------|
| ToolService.java | `backend/src/main/java/com/iaihub/toolbox/service/ToolService.java` |
| application.yml | `backend/src/main/resources/application.yml` |

### Frontend Modified Files

| File | Path |
|------|------|
| UploadPage.vue | `frontend/src/pages/UploadPage.vue` |
| api.ts | `frontend/src/services/api.ts` |
| index.ts | `frontend/src/types/index.ts` |

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
