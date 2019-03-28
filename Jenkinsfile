openshift.withCluster() {
    env.NAMESPACE = openshift.project()
    env.POM_FILE = env.BUILD_CONTEXT_DIR ? "${env.BUILD_CONTEXT_DIR}/pom.xml" : "pom.xml"
    env.APP_NAME = env.JOB_NAME - env.JOB_BASE_NAME - '/'
    echo "Starting Pipeline for ${APP_NAME}..."
    //def projectBase = "${env.NAMESPACE}".replaceAll(/-build/, '')
}

def envVars = env.getEnvironment()
def err = null
currentBuild.result = "SUCCESS"


pipeline {
    // Use Jenkins Maven slave/node/agent (an agent is an opinionated slave/node)
    // Jenkins will dynamically provision this as OpenShift Pod
    // All the stages and steps of this Pipeline will be executed on this Pod
    // After Pipeline completes the Pod is killed so every run will have clean workspace
    agent {
        label 'maven'
    }

    options {
        //buildDiscarder(logRotator(numToKeepStr: '1'))
        disableConcurrentBuilds()
        //parallelsAlwaysFailFast()
        skipStagesAfterUnstable()
        //timestamps()
    }

    //triggers {
    //    pollSCM('H */4 * * 1-5')
    //}

    // Pipeline Stages start here and requires at least one stage
    stages {

        // Checkout source code
        // This is required as Pipeline code is originally checkedout to Jenkins Master
        // but this will also pull this same code to this slave
        stage('Checkout') {
            when {
                expression { return env.GIT_BRANCH == "1.x/${env.NAMESPACE}" && currentBuild.result == "SUCCESS" }
            }
            steps {
                //timeout(time: 2, unit: 'MINUTES')
                
                // git checkout
                //sh 'git config --global http.sslVerify false'
                checkout scm
                //git credentialsId: 'GITHUB_SSH', url: "${APPLICATION_SOURCE_REPO}"
                sh 'git status short'

                sh 'ls -la'
                println("env = ${env.getEnvironment()}")
            }
        }

        // Run Maven build, skipping tests
        stage('Build') {
            when {
                expression { return env.GIT_BRANCH == "1.x/${env.NAMESPACE}" && currentBuild.result == "SUCCESS" }
            }

            //timeout(time: 10, unit: 'MINUTES')

            steps {
                sh "./mvnw clean package -DskipTests=true -f ${POM_FILE}"
            }
        }

        // Run Maven unit tests
        stage('Unit Test') {
            when {
                expression { return env.GIT_BRANCH == "1.x/${env.NAMESPACE}" && currentBuild.result == "SUCCESS" }
            }

            //timeout(time: 10, unit: 'MINUTES')

            steps {
                sh "echo 'Skipping unit tests (for now)'"
                //sh "mvn test -f ${POM_FILE}"
            }
        }

        // Build Container Image using the artifacts produced in previous stages
        stage('Build Container Image') {
            when {
                expression { return env.GIT_BRANCH == "1.x/${env.NAMESPACE}" && currentBuild.result == "SUCCESS" }
            }

            //timeout(time: 10, unit: 'MINUTES')

            steps {
                // Copy the resulting artifacts into common directory
                sh """
                    ls target/*
                    mkdir -p oc-build/deployments
                    rm -rf oc-build && mkdir -p oc-build/deployments
                    for t in \$(echo "jar;war;ear" | tr ";" "\\n"); do
                        cp -rfv ./target/*.\$t oc-build/deployments/ 2> /dev/null || echo "No \$t files"
                    done
                """

                // Build container image using local Openshift cluster
                // Giving all the artifacts to OpenShift Binary Build
                // This places your artifacts into right location inside your S2I image
                // if the S2I image supports it.
                script {
                    openshift.withCluster() {
                        openshift.selector("bc", "${APP_NAME}").startBuild("--from-dir=oc-build").logs("-f")
                    }
                }
            }
        }

        stage('Verify Deployment') { // all pods online
            when {
                expression { return env.BRANCH_NAME != env.NAMESPACE }
            }

            //timeout(time: 20, unit: 'MINUTES')

            steps {
                script {
                    openshift.withCluster() {
                        def dcObj = openshift.selector('dc', env.APP_NAME).object()
                        def podSelector = openshift.selector('pod', [deployment: "${APP_NAME}-${dcObj.status.latestVersion}"])
                        podSelector.untilEach {
                            echo "pod: ${it.name()}"
                            return it.object().status.containerStatuses[0].ready
                        }
                    }
                }
            }
        }

    }
}





