pipeline {
    agent any

    tools {
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
                echo "Launching Capstone Independent Validation Framework..."

                // 🌟 THE UNSTABLE FIX: Appended -Dmaven.test.failure.ignore=true
                // This stops Maven from throwing a hard exit code 1, allowing Jenkins to
                // proceed gracefully to the report generation step without hard-crashing the build.
                bat 'mvn clean test -DsuiteXmlFile=testng.xml -Dmaven.test.failure.ignore=true'
            }
        }
    }

    post {
        always {
            // 🌟 THE ALLURE REPORT FIX: Updated path to 'target/allure-results'
            // Points the Allure plugin precisely to the target folder where Maven compiles
            // the failure bytecode data streams and screenshots during execution cycles.
            allure includeProperties: false,
                   jdk: '',
                   results: [[path: 'target/allure-results']]
        }
    }
}