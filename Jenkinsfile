// Jenkins declarative pipeline: build, test, package, build an image and deploy to staging.
// Needs Jenkins tools named "jdk21" and "maven3", and Docker on the agent for the last two stages.
pipeline {
    agent any

    tools {
        jdk 'jdk21'
        maven 'maven3'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        IMAGE = "accounts-payable-service:${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build and test') {
            steps {
                sh 'mvn -B clean verify'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Docker image') {
            when { branch 'main' }
            steps {
                sh "docker build -t ${IMAGE} -t accounts-payable-service:latest ."
            }
        }

        stage('Deploy to staging') {
            when { branch 'main' }
            steps {
                // Starts the service and PostgreSQL with the stored-function profile
                sh 'docker compose up -d --build'
            }
        }
    }

    post {
        failure {
            echo 'Build failed: check the test report above.'
        }
    }
}
