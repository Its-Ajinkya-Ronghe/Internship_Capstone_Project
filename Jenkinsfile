pipeline {
    agent any

    tools {
        maven 'maven'
        jdk   'JDK22'
    }

    stages {
        // 🐳 STAGE 1: Spin up infrastructure with fallback checks
        stage('Start Selenium Grid') {
            steps {
                echo "Initializing Docker Container Layer..."
                // Using modern v2 'docker compose' structure wrapped in a try-catch string error handler
                bat """
                    docker compose down --remove-orphans || echo "Docker environment not detected or busy, continuing baseline..."
                    docker compose up -d || echo "Skipping container virtualization, executing locally..."
                """
                echo "Settling environment matrix..."
                bat 'ping -n 10 127.0.0.1 > nul'
            }
        }

        // 🚀 STAGE 2: Execute parallel verification tests (Auto-routes to local if Grid is down)
        stage('Execute Parallel Regression Suite') {
            steps {
                echo "Launching Capstone Independent Validation Framework..."
                // If docker failed to bind to port 4444, our TestNG suite defaults to standalone execution cleanly
                bat 'mvn clean test -U -DsuiteXmlFile=testng.xml -Dmaven.test.failure.ignore=true'
            }
        }

        // 🛑 STAGE 3: Safe teardown sequence
        stage('Stop Selenium Grid') {
            steps {
                echo "Cleaning up container footprints..."
                bat 'docker compose down || echo "Teardown skipped."'
            }
        }

        // 📊 STAGE 4: Run backend performance load tests
        stage('Performance Load Testing (JMeter)') {
            steps {
                echo "🚀 Starting Apache JMeter Non-GUI Backend Load Execution..."

                bat '''
                if exist performance-testing\\results rmdir /s /q performance-testing\\results
                mkdir performance-testing\\results
                '''

                bat 'jmeter -n -t performance-testing/NoteEngine_LoadSuite.jmx -l performance-testing/results/log.jtl -e -o performance-testing/results/dashboard-report'

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

            // Compiles Allure results cleanly from the execution path logs
            allure includeProperties: false,
                   jdk: '',
                   results: [[path: 'target/allure-results']]
        }
    }
}