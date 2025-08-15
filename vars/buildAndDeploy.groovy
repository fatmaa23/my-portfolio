// The call method allows the file to be executed like a function
def call(Map config) {
    pipeline {
        agent any

        stages {
            // This stage is the final and correct way to prevent build loops
            stage('Check Commit and Prevent Loop') {
                steps {
                    script {
                        // Jenkins automatically checks out the code that triggered the build.
                        // We just need to read the last commit message from that checkout.
                        def commitMessage = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()

                        // If the last commit was made by our CI, stop the pipeline
                        if (commitMessage.contains('[skip ci]')) {
                            echo "CI commit detected. Skipping build to prevent loop."
                            currentBuild.result = 'SUCCESS'
                            return
                        }
                        echo "User commit detected. Proceeding with build."
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
