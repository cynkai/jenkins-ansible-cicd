// minjun - Week 2: Build / Test / Package / Rolling Deploy / Verify
//
// 2주차 변경
//  - Deploy → Rolling Deploy (ansible/rolling.yml, 서버 한 대씩 drain → 교체 → 복귀)
//  - STRATEGY 파라미터: drain(방식 A, 기본) / no-drain(방식 C, 비교 실험용)
//    choice만 쓴다. 3주차에 다른 사람이 실행하므로 자유 입력(string)은 명령 주입 경로가 된다.
//
// 공용 인프라 전제:
//  - Agent 라벨: ansible-control (스터디장 connection-test에서 검증된 라벨)
//  - SSH: 공용 Credential 'SSH_KeyPair-cc10-cicd.pem' (user: ubuntu)
//  - App 서버 docker / LB nginx 작업은 become(sudo)으로 수행
//
// sh 블록은 전부 작은따옴표('''). Groovy가 아니라 셸이 실행 시점에 변수를 읽게 해서
// 비밀 값(키 경로 등)이 명령 문자열에 박히지 않게 한다.

pipeline {
  agent { label 'ansible-control' }

  parameters {
    choice(name: 'STRATEGY', choices: ['drain', 'no-drain'],
           description: 'drain: 한 대씩 Nginx에서 빼고 교체 (A) / no-drain: 빼지 않고 교체, Nginx 재시도에 맡김 (C, 비교 실험)')
  }

  options {
    disableConcurrentBuilds()                      // 내 배포끼리 겹치지 않게
    buildDiscarder(logRotator(numToKeepStr: '20'))
    timeout(time: 20, unit: 'MINUTES')
  }

  environment {
    APP_IMAGE = 'minjun-app'
    ARCHIVE   = 'build/minjun-app.tar.gz'
    SSH_CRED  = 'SSH_KeyPair-cc10-cicd.pem'
  }

  stages {

    stage('Checkout') {
      steps {
        checkout scm
        script {
          env.GIT_SHA     = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
          env.APP_VERSION = readFile('app/VERSION').trim()
          env.IMAGE_TAG   = "${env.APP_VERSION}-${env.GIT_SHA}"
          // 파라미터를 추가한 첫 빌드는 params가 비어 있을 수 있어 기본값을 둔다
          env.STRATEGY    = params.STRATEGY ?: 'drain'
          currentBuild.displayName = "#${env.BUILD_NUMBER} ${env.IMAGE_TAG} ${env.STRATEGY}"
        }
        sh 'echo "version=$APP_VERSION sha=$GIT_SHA tag=$IMAGE_TAG"'
      }
    }

    stage('Build') {
      steps {
        sh '''
          docker build \
            --build-arg APP_VERSION="$APP_VERSION" \
            --build-arg GIT_SHA="$GIT_SHA" \
            -t "$APP_IMAGE:$IMAGE_TAG" .
        '''
      }
    }

    stage('Test') {
      steps {
        sh 'bash scripts/test.sh "$APP_IMAGE:$IMAGE_TAG" "$APP_VERSION"'
      }
    }

    stage('Package') {
      steps {
        // 레지스트리가 없으니 이미지를 파일로 만들어 Ansible이 복사한다.
        // 파일은 내 workspace 안에만 둔다 (공용 Agent 규칙).
        sh '''
          mkdir -p build
          docker save "$APP_IMAGE:$IMAGE_TAG" | gzip > "$ARCHIVE"
          ls -lh "$ARCHIVE"
        '''
      }
    }

    stage('Rolling Deploy') {
      steps {
        withCredentials([sshUserPrivateKey(credentialsId: env.SSH_CRED,
                                           keyFileVariable: 'SSH_KEY',
                                           usernameVariable: 'SSH_USER')]) {
          dir('ansible') {
            // bash로 실행해야 pipefail이 된다 (tee 뒤에서 ansible 실패가 묻히지 않게)
            sh '''#!/bin/bash
              set -euo pipefail
              if [ "$STRATEGY" = "no-drain" ]; then DRAIN=false; else DRAIN=true; fi
              echo "strategy=$STRATEGY rolling_drain=$DRAIN"

              ansible-playbook -i inventory.ini rolling.yml \
                -u "$SSH_USER" --private-key "$SSH_KEY" \
                -e app_version="$APP_VERSION" \
                -e git_sha="$GIT_SHA" \
                -e image_archive="$WORKSPACE/$ARCHIVE" \
                -e rolling_drain="$DRAIN" \
                | tee "$WORKSPACE/build/rolling.log"

              echo "nginx reloads: $(grep -c '"msg": "NGINX_RELOADED' "$WORKSPACE/build/rolling.log" || true)"
            '''
          }
        }
      }
    }

    stage('Verify') {
      steps {
        withCredentials([sshUserPrivateKey(credentialsId: env.SSH_CRED,
                                           keyFileVariable: 'SSH_KEY',
                                           usernameVariable: 'SSH_USER')]) {
          dir('ansible') {
            sh '''
              ansible-playbook -i inventory.ini verify.yml \
                -u "$SSH_USER" --private-key "$SSH_KEY" \
                -e app_version="$APP_VERSION"
            '''
          }
        }
      }
    }
  }

  post {
    always {
      // 배포 로그는 발표·비교용으로 남긴다 (handler 실행 = reload 기록 포함)
      archiveArtifacts artifacts: 'build/rolling.log', allowEmptyArchive: true
      // 공용 Agent 디스크를 채우지 않도록 정리
      sh '''
        rm -f "$ARCHIVE" || true
        docker image rm "$APP_IMAGE:$IMAGE_TAG" >/dev/null 2>&1 || true
      '''
    }
  }
}
