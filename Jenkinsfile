// Declarative CI/CD pipeline for the Calculator REST API.
//
//   Checkout -> Build -> Test -> Docker Build -> Push -> Deploy
//
// One-time Jenkins setup (plugins, Docker Hub credential, e-mail) is described
// in README.md.

pipeline {
    agent any

    options {
        skipDefaultCheckout(true)        // checkout happens explicitly in the Checkout stage
        disableConcurrentBuilds()        // never run two deployments at once
        buildDiscarder(logRotator(numToKeepStr: '15'))
        timeout(time: 30, unit: 'MINUTES')
    }

    triggers {
        githubPush()                     // GitHub webhook (needs Jenkins reachable from GitHub, e.g. via ngrok)
        pollSCM('H/2 * * * *')          // fallback: check GitHub for new commits every ~2 minutes
    }

    environment {
        IMAGE_NAME      = 'calculator-api'
        IMAGE_TAG       = "${env.BUILD_NUMBER}"            // every build gets its own image tag
        PLATFORMS       = 'linux/amd64,linux/arm64'       // Intel and Apple Silicon
        DOCKERHUB_CREDS = 'dockerhub-creds'                // ID of the Jenkins "Username with password" credential
        PROXY_PORT      = '8085'                           // matches docker-compose.yml; Jenkins itself uses 8080
        PATH            = "/opt/homebrew/bin:${env.PATH}"  // mvn, docker, curl on macOS/Homebrew
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    def scmVars = checkout scm
                    env.GIT_COMMIT_SHORT = scmVars.GIT_COMMIT.take(7)
                }
                sh 'git log -1 --pretty=format:"%h %an: %s"'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B -ntp clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/calculator-api.jar', fingerprint: true
                }
            }
        }

        stage('Test') {
            steps {
                // A failing test makes mvn exit non-zero, which fails this stage and the build.
                sh 'mvn -B -ntp test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build --platform "$PLATFORMS" -t "$IMAGE_NAME:$IMAGE_TAG" -t "$IMAGE_NAME:latest" .'
            }
        }

        stage('Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: env.DOCKERHUB_CREDS,
                                                  usernameVariable: 'DOCKERHUB_USER',
                                                  passwordVariable: 'DOCKERHUB_TOKEN')]) {
                    // Single quotes: the shell expands the secrets, so Groovy never interpolates them.
                    sh '''
                        echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USER" --password-stdin
                        for tag in "$IMAGE_TAG" latest; do
                            docker tag "$IMAGE_NAME:$IMAGE_TAG" "$DOCKERHUB_USER/$IMAGE_NAME:$tag"
                            docker push "$DOCKERHUB_USER/$IMAGE_NAME:$tag"
                        done
                    '''
                }
            }
            post {
                always {
                    sh 'docker logout || true'
                }
            }
        }

        stage('Deploy') {
            steps {
                // IMAGE_TAG (above) makes Compose run exactly the image built in this pipeline.
                // --wait blocks until every container is healthy and fails the stage if one is not.
                sh '''
                    docker compose build proxy
                    docker compose up -d --no-build --wait --wait-timeout 180 --remove-orphans
                    docker compose ps
                '''
                // Smoke test through the proxy, the way a client would call it
                sh '''
                    curl -fsS "http://localhost:$PROXY_PORT/actuator/health"; echo
                    curl -fsS "http://localhost:$PROXY_PORT/api/v1/add?a=2&b=3"; echo
                '''
            }
        }
    }

    post {
        success {
            script { notifyByEmail('SUCCESS') }
        }
        failure {
            script { notifyByEmail('FAILURE') }
        }
        cleanup {
            cleanWs()
        }
    }
}

// Recipient comes from the NOTIFY_EMAIL environment variable set in
// Manage Jenkins -> System -> Global properties, so no address is committed to Git.
def notifyByEmail(String status) {
    if (!env.NOTIFY_EMAIL) {
        echo "NOTIFY_EMAIL is not set in Jenkins; skipping the ${status} e-mail"
        return
    }
    emailext(
        to: env.NOTIFY_EMAIL,
        subject: "${status}: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        mimeType: 'text/html',
        attachLog: status == 'FAILURE',
        body: """
            <h3>${status}: ${env.JOB_NAME} #${env.BUILD_NUMBER}</h3>
            <table>
              <tr><td>Job</td><td>${env.JOB_NAME}</td></tr>
              <tr><td>Build</td><td>#${env.BUILD_NUMBER}</td></tr>
              <tr><td>Commit</td><td>${env.GIT_COMMIT_SHORT ?: 'n/a'}</td></tr>
              <tr><td>Image</td><td>${env.IMAGE_NAME}:${env.IMAGE_TAG}</td></tr>
              <tr><td>Duration</td><td>${currentBuild.durationString.replace(' and counting', '')}</td></tr>
            </table>
            <p><a href="${env.BUILD_URL}console">Console output</a></p>
        """
    )
}
