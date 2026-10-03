# jenkins-ansible-cicd (minjun)

클클 10기 Jenkins & Ansible 스터디 실습 레포.

## 할당 자원

| 항목 | 값 |
|---|---|
| Nginx 포트 | 18006 |
| App 포트 | 20006 (blue) / 21006 (green) |
| 컨테이너 | `minjun-app-blue`, `minjun-app-green` |
| Nginx 설정 | `/etc/nginx/conf.d/minjun.conf`, `upstream minjun_backend` |
| Jenkins | Folder `minjun`, Agent 라벨 `ansible-control` |

## 파이프라인 (1주차)

`Checkout → Build → Test → Package → Deploy → Verify`

- **Build**: Agent에서 `docker build`. 버전(`app/VERSION`)과 커밋 SHA를 이미지에 고정
- **Test**: 호스트 포트를 열지 않고 `docker exec`로 `/health`, `/version` 검사
- **Package**: 레지스트리가 없어서 `docker save`로 파일화 (workspace 안에서만)
- **Deploy**: Ansible로 App 3대에 같은 이미지 배포 → 내 nginx conf만 교체
- **Verify**: LB에서 18006으로 12회 호출, 버전 일치와 3대 분산 확인

## 공용 환경에서 지킨 것

- 모든 이름에 `minjun` 접두사. 남의 컨테이너·conf는 읽지도 수정하지도 않음
- nginx 변경은 `flock` 락 안에서 `백업 → 교체 → nginx -t → reload`. 실패 시 내 변경만 원복
- 같은 내용이면 reload 하지 않음 (공용 nginx를 불필요하게 건드리지 않기)
- 이미지 아카이브·테스트 컨테이너는 빌드 후 정리

## 버전 올리기

```bash
echo v2 > app/VERSION && git commit -am "bump v2" && git push
```
Jenkins에서 빌드하면 `/version`이 v2로 바뀐다.
