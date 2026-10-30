pipeline {
    agent any

    triggers {
        // GitHub webhook trigger; Poll SCM every 2 minutes as a fallback
        githubPush()
        pollSCM('H/2 * * * *')
    }

    environment {
        DOCKERHUB_USER = 'sasank2305'
        IMAGE = "${DOCKERHUB_USER}/calculator"
        TAG = "${BUILD_NUMBER}"
        EMAIL = 'sasanklingareddy@gmail.com'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t $IMAGE:$TAG -t $IMAGE:latest .'
            }
        }

        stage('Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
                    sh 'echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin'
                    sh 'docker push $IMAGE:$TAG'
                    sh 'docker push $IMAGE:latest'
                }
            }
        }

        stage('Deploy') {
            steps {
                // up -d only recreates containers whose image changed, so re-runs are safe
                sh 'docker compose up -d --no-build'
                sh '''
                    for i in $(seq 1 20); do
                        if curl -sf http://localhost:9090/actuator/health; then
                            echo " -> healthy"; exit 0
                        fi
                        sleep 3
                    done
                    echo "Health check failed"; exit 1
                '''
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
        }
        success {
            mail to: "${EMAIL}",
                 subject: "SUCCESS: ${JOB_NAME} #${BUILD_NUMBER}",
                 body: "Build succeeded and deployed image ${IMAGE}:${TAG}.\n${BUILD_URL}"
        }
        failure {
            mail to: "${EMAIL}",
                 subject: "FAILURE: ${JOB_NAME} #${BUILD_NUMBER}",
                 body: "Build failed. Check console output:\n${BUILD_URL}console"
        }
    }
}
