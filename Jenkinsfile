pipeline {
  agent any
  stages {
    stage('build') {
      steps {
        sh '''java -Xmx32m -version

#./mvnw install -DskipTests=true -Dmaven.javadoc.skip=true -B -V

'''
      }
    }
  }
}