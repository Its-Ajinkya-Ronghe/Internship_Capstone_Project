pipeline {
    agent any

    tools {
        maven 'maven'
        jdk   'JDK22'
    }

    stages {
        // 🚀 STAGE 1: Execute Parallel Verification Suite natively on the host runner environment
        stage('Execute Parallel Regression Suite') {
            steps {
                echo "Launching Capstone Independent Validation Framework locally..."
                // Runs TestNG suites smoothly using clean native local machine thread virtualization
                bat 'mvn clean test -U -DsuiteXmlFile=testng.xml -Dmaven.test.failure.ignore=true'
            }
        }

        // 📊 STAGE 2: Run backend performance load testing layers
        stage('Performance Load Testing (JMeter)') {
            steps {
                echo "🚀 Starting Apache JMeter Non-GUI Backend Load Execution..."

                // Pre-execution Workspace Guard: Clean up old run metrics directories safely
                bat '''
                if exist performance-testing\\results rmdir /s /q performance-testing\\results
                mkdir performance-testing\\results
                '''

                // Execute JMeter in Non-GUI mode cleanly with our 0% error-rate mock suite properties
                bat 'jmeter -n -t performance-testing/NoteEngine_LoadSuite.jmx -l performance-testing/results/log.jtl -e -o performance-testing/results/dashboard-report'

                // Archive and publish the load testing summary dashboard inside Jenkins layout panels
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

            // Compiles Allure results cleanly from the execution path target charts
            allure includeProperties: false,
                   jdk: '',
                   results: [[path: 'target/allure-results']]
        }
    }
}