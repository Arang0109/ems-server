다음 7단계를 순서대로 실행하세요. 각 단계가 실패하면 즉시 중단하고 결과를 보고하세요.

커밋 메시지 인자: $ARGUMENTS

---

## Step 1. 테스트 실행

PowerShell에서 프로젝트 루트에서 실행하세요:

```powershell
.\gradlew.bat test
```

- **BUILD SUCCESSFUL** → Step 2로 진행합니다.
- **BUILD FAILED** → 즉시 중단합니다. 실패한 테스트 클래스명과 메서드명을 나열하고, `build/reports/tests/test/index.html` 경로를 안내하세요. 커밋/푸시를 절대 진행하지 마세요.

---

## Step 2. 변경 내용 확인

```powershell
git status
```

- 변경 파일이 없으면 ("nothing to commit, working tree clean") → "커밋할 변경 사항이 없습니다. 이미 최신 상태입니다." 출력 후 종료합니다.
- 변경 파일이 있으면 → Step 3으로 진행합니다.

---

## Step 3. .env 파일 안전 검사

`git status` 결과에서 `.env`, `.env.local`, `.env.production` 등 `.env*` 패턴의 파일이 포함되어 있는지 확인합니다.

해당 파일이 있으면 **즉시 중단**하고 다음을 출력하세요:
"위험: `.env` 파일이 변경 목록에 포함되어 있습니다. 보안 정보 노출 방지를 위해 커밋하지 않습니다."

---

## Step 4. 커밋 메시지 결정

- **`$ARGUMENTS`가 입력된 경우**: 해당 텍스트를 그대로 커밋 메시지로 사용합니다.
- **`$ARGUMENTS`가 비어 있는 경우**: `git diff HEAD` 결과를 분석하여 커밋 메시지를 자동 생성합니다.
  - 형식: `type: 변경 내용 요약 (한국어)`
  - type 종류: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `style`
  - 사용자 확인 없이 바로 진행합니다.

---

## Step 5. Staging

```powershell
git add .
git reset HEAD -- .env
git reset HEAD -- .env.local
git reset HEAD -- .env.production
```

`.env` 파일이 없어도 reset 명령은 에러 없이 실행됩니다.

---

## Step 6. Commit

PowerShell 환경에서 Co-Authored-By 태그를 포함해 커밋합니다. 백틱(`` ` ``)으로 줄바꿈을 표현하세요:

```powershell
git commit -m "<커밋 메시지>`n`nCo-Authored-By: <현재 세션의 모델명> <noreply@anthropic.com>"
```

---

## Step 7. Push

현재 브랜치명을 확인한 후 push합니다:

```powershell
git push origin <현재-브랜치명>
```

- **성공** → 아래 형식으로 최종 결과를 출력합니다.
- **실패 (non-fast-forward)** → `git pull --rebase origin <브랜치명>` 후 재시도를 권고합니다.

---

## 최종 결과 출력

```
테스트 & 푸시 완료
─────────────────────────────
브랜치   : <브랜치명>
커밋 메시지: <커밋 메시지>
변경 파일 : <스테이징된 파일 목록>
원격      : origin/<브랜치명>
─────────────────────────────
```
