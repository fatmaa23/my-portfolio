// Import the shared library we configured in Jenkins
@Library('my-shared-library') _

pipeline {
    agent any

    environment {
        // Change 'your-dockerhub-username' to your actual username
        DOCKERHUB_USERNAME = "fatmaa23" 
        IMAGE_NAME = "my-portfolio"
    }

    stages {
        stage('Checkout Code') {
            steps {
                echo 'Checking out code from Git...'
                git branch: 'main', url: 'https://github.com/fatmaa23/my-portfolio.git'
            }
        }

        stage('Build and Push Image') {
            steps {
                // Call our custom step from the shared library!
                buildAndPushDockerImage(
                    dockerhubUser: DOCKERHUB_USERNAME,
                    imageName: IMAGE_NAME
                )
            }
        }
    }

    post {
        always {
            echo 'Pipeline finished.'
            // The docker logout is now handled inside the shared library logic for better encapsulation
            sh "docker logout"
        }
    }
}