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
                // Runs TestNG suites via Maven, ignoring hard failures to allow reporting steps to process
                bat 'mvn clean test -DsuiteXmlFile=testng.xml -Dmaven.test.failure.ignore=true'
            }
        }

        // 🌟 NEW STAGE: Standalone Performance Engineering Layer (Section 3.5)
        stage('Performance Load Testing (JMeter)') {
            steps {
                echo "🚀 Starting Apache JMeter Non-GUI Backend Load Execution..."

                // 🛠️ Pre-execution Workspace Guard: Clean up any old execution run artifacts safely
                bat '''
                if exist performance-testing\\results rmdir /s /q performance-testing\\results
                mkdir performance-testing\\results
                '''

                // 🏃 Execute JMeter in Non-GUI (CLI) mode to simulate concurrent stress profiles
                bat 'jmeter -n -t performance-testing/NoteEngine_LoadSuite.jmx -l performance-testing/results/log.jtl -e -o performance-testing/results/dashboard-report'

                // 🔓 Jenkins Dashboard Content Security Policy (CSP) Bypass Fix:
                // Allows interactive Javascript charts and CSS graphics to load cleanly on your local machine.
                script {
                    System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "")
                }

                // 📊 Archive and publish the interactive graphical load testing summary dashboard
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'performance-testing/results/dashboard-report',
                    reportFiles: 'index.html',
                    reportName: 'JMeter Load Performance Dashboard'
                ])
            }
        }
    }

    post {
        always {
            echo "Archiving test reporting assets and compiling telemetry artifacts..."
            // Compiles Allure results from Maven execution cycles
            allure includeProperties: false,
                   jdk: '',
                   results: [[path: 'target/allure-results']]
        }
    }
}