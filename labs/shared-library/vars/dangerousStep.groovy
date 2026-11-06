def call() {
    // Jenkins 내부 API 직접 접근 시도
    def jenkins = jenkins.model.Jenkins.instance
    echo "Jenkins 버전: ${jenkins.getVersion()}"
    echo "Job 개수: ${jenkins.getAllItems().size()}"
}
