def call (Map configMap){
    //map name.get("key_name")
    def component = configMap.get("component")
    echo "component is : $component"
    pipeline {
        agent { node { label 'Agent-1' } }
        environment{
            //here if you create any variable you will have global access, since it is environment no need of def
            packageVersion = ''
        }
        stages {
            stage('Get version'){
                steps{
                    script{
                        def packageJson = readJSON(file: 'package.json')
                        packageVersion = packageJson.version
                        echo "version: ${packageVersion}"
                    }
                }
            }
            stage('Install depdencies') {
                steps {
                    sh 'npm install'
                }
            }
            stage('Unit test') {
                steps {
                    echo "unit testing is done here"
                }
            }
            //sonar-scanner command expect sonar-project.properties should be available
            stage('Sonar Scan') {
                steps {
                    echo "Sonar scan done"
                }
            }
            stage('Build') {
                steps {
                    sh 'ls -ltr'
                    sh "zip -r ${component}.zip ./* --exclude=.git --exclude=.zip"
                }
            }
            stage('SAST') {
                steps {
                    echo "SAST Done"
                    echo "package version: $packageVersion"
                }
            }
            //install pipeline utility steps plugin, if not installed
            stage('Publish Artifact') {
                steps {
                    nexusArtifactUploader(
                        nexusVersion: 'nexus3',
                        protocol: 'http',
                        nexusUrl: '44.197.180.151:8081/',
                        groupId: 'com.roboshop',
                        version: "$packageVersion",
                        repository: "${component}",
                        credentialsId: 'nexus-auth',
                        artifacts: [
                            [artifactId: "${component}",
                            classifier: '',
                            file: "${component}.zip",
                            type: 'zip']
                        ]
                    )
                }
            }

            stage('Docker Build') {
                steps {
                    script{
                        sh """
                            docker build -t gspvsr/${component}:${packageVersion} .
                        """
                    }
                        
                }
            }
            //just makesure you login inside agent
            stage('Docker Push') {
                steps {
                    script{
                        sh """
                            docker push gspvsr/${component}:${packageVersion}
                        """
                    }
                        
                }
            }

            //just makesure you login inside agent
            stage('EKS Deploy') {
                steps {
                    script{
                        sh """
                            cd helm
                            sed -i 's/IMAGE_VERSION/$packageVersion/g' values.yaml
                            helm install ${component} .
                        """
                    }
                        
                }
            }


            //here I need to configure downstram job. I have to pass package version for deployment
            // This job will wait until downstrem job is over
            // by default when a non-master branch CI is done, we can go for DEV development
        //     stage('Deploy') {
        //         steps {
        //             script{
        //                 echo "Deployment"
        //                 def params = [
        //                     string(name: 'version', value: "$packageVersion")
        //                     string(name: 'environment', value: "dev")
        //                 ]
        //                 build job: "../${component}-deploy", wait: true, parameters: params
        //             }
        //         }
        //     }
        }

        post{
            always{
                echo 'cleaning up workspace'
                //deleteDir()
            }
        }
    }
}