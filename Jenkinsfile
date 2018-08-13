pipeline {
  agent {
    node {
      label 'maven'
    }

  }
  stages {
    stage('build') {
      steps {
        sh '''

java -Xmx32m -version

#./mvnw install -DskipTests=true -Dmaven.javadoc.skip=true -B -V


ls -l

mvn help:all-profiles 



'''
      }
    }
  }
}