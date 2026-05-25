pipeline {
    agent any

    tools {
        // Updated to match the naming convention suggested by your Jenkins logs
        maven 'maven'
        jdk   'JDK22'
    }

    stages {
        stage('Checkout Source Code') {
            steps {
                checkout scm
            }
        }

        stage('Execute Parallel Regression Suite') {
            steps {
                // Fixed: Changed from log.info to Jenkins native echo step
                echo "Launching Capstone Independent Validation Framework..."
                bat 'mvn clean test -DsuiteXmlFile=testng.xml'
            }
        }
    }

    post {
        always {
            // Fixed: Removed the invalid nested stage block inside post
            allure includeProperties: false,
                   jdk: '',
                   results: [[path: 'allure-results']]
        }
    }
}