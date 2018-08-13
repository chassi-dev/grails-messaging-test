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

./mvnw clean package -DskipTests=true -Dmaven.javadoc.skip=true -B -V

ls -lR

'''
      }
    }
  }
}
