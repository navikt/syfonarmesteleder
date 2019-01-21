#!/usr/bin/env groovy

pipeline {
    agent any

    environment {
        APPLICATION_NAME = 'syfonarmesteleder'
        DISABLE_SLACK_MESSAGES = false
        ZONE = 'fss'
        DOCKER_SLUG='syfo'
        FASIT_ENVIRONMENT='q1'
    }

    stages {
        stage('initialize') {
            steps {
                init action: 'gradle'
            }
        }
        stage('build') {
            steps {
                sh './gradlew build -x test'
            }
        }
        stage('run tests (unit & intergration)') {
            steps {
                sh './gradlew test'
            }
        }
        stage('Create uber jar') {
            steps {
                sh './gradlew shadowJar'
                slackStatus status: 'passed'
            }
        }
        stage('deploy') {
            steps {
                dockerUtils action: 'createPushImage'
                nais action: 'validate'
                nais action: 'upload'
                deployApp action: 'jiraPreprod'
            }
        }
    }
    post {
        always {
            postProcess action: 'always'
        }
        success {
            postProcess action: 'success'
        }
        failure {
            postProcess action: 'failure'
        }
    }
}
