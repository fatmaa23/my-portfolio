// The call method allows the file to be executed like a function
def call(Map config) {
    pipeline {
        agent any

        // Define environment variables used throughout the pipeline
        environment {
            // Use Jenkins credentials for Docker Hub
            DOCKER_HUB_CREDS = credentials('dockerhub-credentials') 
            // Define the Docker image name and tag
            IMAGE_NAME = "${config.dockerhubUser}/${config.imageRepo}"
            IMAGE_TAG = "build-${BUILD_NUMBER}"
        }

        stages {
            stage('Cloning Git Repository') {
                steps {
                    echo "Cloning the repository..."
                    // This step is implicitly handled by Jenkins when checking out the Jenkinsfile
                    // but we can add explicit checkout if needed.
                    git url: "${config.gitUrl}", branch: "${config.gitBranch}"
                }
            }

            stage('Build Docker Image') {
                steps {
                    script {
                        echo "Building Docker image: ${IMAGE_NAME}:${IMAGE_TAG}"
                        // Build the Docker image using the Dockerfile in the repo
                        docker.build(IMAGE_NAME, ".")
                    }
                }
            }

            stage('Push Docker Image to Docker Hub') {
                steps {
                    script {
                        // Log in to Docker Hub and push the image
                        docker.withRegistry('https://registry.hub.docker.com', DOCKER_HUB_CREDS) {
                            echo "Pushing Docker image: ${IMAGE_NAME}:${IMAGE_TAG}"
                            docker.image(IMAGE_NAME).push(IMAGE_TAG)
                            
                            // Also push the 'latest' tag for simplicity with Kubernetes
                            docker.image(IMAGE_NAME).push('latest')
                        }
                    }
                }
            }

            stage('Update Kubernetes Manifests') {
                steps {
                    script {
                        echo "Updating Kubernetes deployment with new image..."
                        // Use a simple shell command to replace the image placeholder
                        // sed is a powerful stream editor for this task
                        sh "sed -i 's|image: .*|image: ${IMAGE_NAME}:latest|' k8s/deployment.yaml"
                    }
                }
            }
            
            stage('Commit and Push Manifest Changes') {
                // This stage will only run if the build is on the 'main' branch
                when {
                    branch 'main'
                }
                steps {
                    script {
                        echo "Committing and pushing manifest changes to Git..."
                        withCredentials([usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
                            sh "git config --global user.email 'fatmaa@example.com'"
                            sh "git config --global user.name 'Fatma'"
                            sh "git add k8s/deployment.yaml"
                            sh "git commit -m 'CI: Update image to ${IMAGE_NAME}:latest [skip ci]'"
                            // Use the credentials to push back to the repository
                            sh "git push https://${GIT_USER}:${GIT_TOKEN}@github.com/${config.githubRepo}.git HEAD:main"
                        }
                        echo "Manifest pushed to Git. ArgoCD will now take over."
                    }
                }
            }
        }
    }
}