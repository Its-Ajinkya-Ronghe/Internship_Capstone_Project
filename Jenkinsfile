pipeline {
    agent any

    tools {
        // Points directly to the system engines we registered in Step 2
        maven 'Maven_3.x'
        jdk   'Java_22'
    }

    stages {
        stage('Checkout Source Code') {
            steps {
                // Pulls the latest test framework updates from your code repository
                checkout scm
            }
        }

        stage('Execute Parallel Regression Suite') {
            steps {
                log.info "Launching Capstone Independent Validation Framework..."
                // Cleans old targets and fires your TestNG parallel suite runner XML execution block
                bat 'mvn clean test -DsuiteXmlFile=testng.xml'
            }
        }
    }

    post {
        always {
            stage('Generate Reporting Artifacts') {
                steps {
                    // Automatically collects test results and builds an interactive Allure report dashboard
                    allure includeProperties: false,
                           jdk: '',
                           results: [[path: 'allure-results']]
                }
            }
        }
    }
}