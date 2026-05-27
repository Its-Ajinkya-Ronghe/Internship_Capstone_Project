pipeline {
    agent any

    tools {
        maven 'maven'
        jdk   'JDK22'
    }

    stages {
        // 🐳 STAGE 1: Spin up infrastructure before running any code compilation
        stage('Start Selenium Grid') {
            steps {
                echo "Starting Selenium Grid Hub and Node containers via Docker Compose..."
                bat 'docker-compose down' // Guard: Clear any lingering dead containers first
                bat 'docker-compose up -d'
                echo "Waiting for Grid infrastructure matrix to settle..."
                bat 'ping -n 15 127.0.0.1 > nul' // 15s safe spin-up delay
            }
        }

        // 🚀 STAGE 2: Compile and execute the full test framework on the live Grid
        stage('Execute Parallel Regression Suite on Grid') {
            steps {
                echo "Launching Capstone Independent Validation Framework against Selenium Grid..."
                // 🌟 FIXED: Added -U to force-download missing WebDriverManager jars and resolve the compilation failure
                bat 'mvn clean test -U -DsuiteXmlFile=testng.xml -DuseGrid=true -DgridUrl=http://localhost:4444/wd/hub -Dmaven.test.failure.ignore=true'
            }
        }

        // 🛑 STAGE 3: Tear down container network safely right after tests complete
        stage('Stop Selenium Grid') {
            steps {
                echo "Tearing down Docker environment..."
                bat 'docker-compose down'
            }
        }

        // 📊 STAGE 4: Run the flawless backend performance load tests
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

            // Clean teardown backup to ensure no ports stick open if a test fails early
            bat 'docker-compose down'

            // Compiles Allure results from the execution cycles
            allure includeProperties: false,
                   jdk: '',
                   results: [[path: 'target/allure-results']]
        }
    }
}