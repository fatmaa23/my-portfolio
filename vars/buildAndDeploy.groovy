// The call method allows the file to be executed like a function
def call(Map config) {
    // --- START: Build Loop Prevention ---
    // Get the last commit message
    def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()

    // If the last commit was made by Jenkins, stop the pipeline
    if (commitMessage.contains('[skip ci]')) {
        echo "Commit was made by Jenkins CI. Skipping build."
        // This command gracefully stops the pipeline
        currentBuild.result = 'SUCCESS'
        return
    }
    // --- END: Build Loop Prevention ---

    pipeline {
        agent any

        environment {
            IMAGE_NAME = "${config.dockerhubUser}/${config.imageRepo}"
            IMAGE_TAG = "build-${BUILD_NUMBER}"
        }

        stages {
            stage('Cloning Git Repository') {
                steps {
                    echo "Cloning the repository..."
                    git url: "${config.gitUrl}", branch: "${config.gitBranch}"
                }
            }

            stage('Build and Push Docker Image') {
                steps {
                    script {
                        echo "Building Docker image: ${IMAGE_NAME}:${IMAGE_TAG}"
                        def customImage = docker.build(IMAGE_NAME, ".")

                        docker.withRegistry('https://registry.hub.docker.com', 'dockerhub-credentials') {
                            echo "Pushing Docker image: ${IMAGE_NAME}:${IMAGE_TAG}"
                            customImage.push(IMAGE_TAG)
                            customImage.push('latest')
                        }
                    }
                }
            }

            stage('Update & Commit Manifests') {
                steps {
                    script {
                        echo "Updating Kubernetes deployment with new image..."
                        sh "sed -i 's|image: .*|image: ${IMAGE_NAME}:${IMAGE_TAG}|' k8s/deployment.yaml"
                        
                        echo "Committing and pushing manifest changes to Git..."
                        withCredentials([usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
                            sh "git config --global user.email 'fatmaa@gmail.com'"
                            sh "git config --global user.name 'Fatma Ahmed'"
                            sh "git add k8s/deployment.yaml"
                            sh "git commit -m 'CI: Update image to ${IMAGE_NAME}:${IMAGE_TAG} [skip ci]'"
                            sh "git push https://${GIT_USER}:${GIT_TOKEN}@github.com/${config.githubRepo}.git HEAD:main"
                        }
                        echo "Manifest pushed to Git. ArgoCD will now take over."
                    }
                }
            }
        }
    }
}
