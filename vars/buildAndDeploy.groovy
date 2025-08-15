// The call method allows the file to be executed like a function
def call(Map config) {
    pipeline {
        agent any

        stages {
            // This stage is the final and correct way to prevent build loops
            stage('Check Commit Message') {
                steps {
                    script {
                        // We must clone the repo here to get the LATEST commit message
                        git url: "${config.gitUrl}", branch: "${config.gitBranch}"
                        
                        // Get the last commit message from the cloned repository
                        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()

                        // If the last commit was made by Jenkins, stop the pipeline
                        if (commitMessage.contains('[skip ci]')) {
                            echo "Commit was made by our CI. Skipping build to prevent a loop."
                            // This command gracefully stops the pipeline and marks it as successful
                            currentBuild.result = 'SUCCESS'
                            return
                        }
                        echo "Commit is from a user. Proceeding with the build."
                    }
                }
            }

            stage('Build and Push Docker Image') {
                steps {
                    script {
                        echo "Building Docker image: build-${BUILD_NUMBER}"
                        def customImage = docker.build("${config.dockerhubUser}/${config.imageRepo}")

                        docker.withRegistry('https://registry.hub.docker.com', 'dockerhub-credentials') {
                            echo "Pushing Docker image..."
                            customImage.push("build-${BUILD_NUMBER}")
                            customImage.push('latest')
                        }
                    }
                }
            }

            stage('Update & Commit Manifests') {
                steps {
                    script {
                        echo "Updating Kubernetes deployment with new image..."
                        def imageName = "${config.dockerhubUser}/${config.imageRepo}"
                        def imageTag = "build-${BUILD_NUMBER}"
                        sh "sed -i 's|image: .*|image: ${imageName}:${imageTag}|' k8s/deployment.yaml"
                        
                        echo "Committing and pushing manifest changes to Git..."
                        withCredentials([usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
                            sh "git config --global user.email 'jenkins@example.com'"
                            sh "git config --global user.name 'Jenkins CI'"
                            sh "git add k8s/deployment.yaml"
                            // We add [skip ci] to our own commit message
                            sh "git commit -m 'CI: Update image to ${imageName}:${imageTag} [skip ci]'"
                            sh "git push https://${GIT_USER}:${GIT_TOKEN}@github.com/${config.githubRepo}.git HEAD:main"
                        }
                        echo "Manifest pushed to Git. ArgoCD will now take over."
                    }
                }
            }
        }
    }
}
