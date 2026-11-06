# labs — 3주차 개념 실습 (2026-09-14)

스터디 3주차(Declarative Pipeline · Multibranch · Shared Libraries) 내용을 따로 만들어 본 작은 실습입니다.
원래는 별도 로컬 저장소 두 개였고, 커밋 기록을 유지한 채 이 폴더로 옮겼습니다.

## shared-library

Jenkins Shared Library의 `vars/` 전역 스텝 두 개.

| 스텝 | 내용 |
|---|---|
| `sayHello(name = 'World')` | 기본 사용법. 파이프라인에서 `sayHello('minjun')`처럼 호출 |
| `dangerousStep()` | 라이브러리 코드가 `Jenkins.instance`(내부 API)에 직접 접근하는 예. 신뢰된(global) 라이브러리는 샌드박스 밖에서 실행되므로 이런 접근이 가능하다는 점을 확인하려고 만들었습니다 |

## multibranch-demo

Multibranch Pipeline이 브랜치마다 다르게 동작하는지 확인한 데모입니다.
이 폴더에는 `main`의 `Jenkinsfile`만 옮겼고, 원래 저장소에는 브랜치가 세 개 있었습니다.

| 브랜치 | 내용 | 확인하려던 동작 |
|---|---|---|
| `main` | `Jenkinsfile` (Info → Test → Deploy) | Deploy 스테이지는 `when { branch 'main' }`이라 main에서만 실행 |
| `feature/login` | `README.md` 한 줄 추가 | 잡이 생성되고 Test까지만 실행, Deploy는 건너뜀 |
| `docs-only` | `Jenkinsfile` 삭제, `docs.md` 추가 | `Jenkinsfile`이 없어 Multibranch가 잡을 만들지 않음 |
