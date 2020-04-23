def call(String private_branch, String public_branch) {
    try {
        ansiColor('xterm') {
            String ANSI_GREEN = "\u001B[32m"
            String ANSI_NORMAL = "\u001B[0m"
            String ANSI_BOLD = "\u001B[1m"

                
            jobName = sh(returnStdout: true, script: "echo $JOB_NAME").split('/')[-1].trim()
            module = sh(returnStdout: true, script: "echo $JOB_NAME").split('/')[-2].trim()
            envDir = sh(returnStdout: true, script: "echo $JOB_NAME").split('/')[-3].trim()
            jobType = sh(returnStdout: true, script: "echo $JOB_NAME").split('/')[-4].trim()
        
            stage('Write data') {
                
                sh """
                    mkdir -p ${JENKINS_HOME}/summary/${envDir}
                    touch -a ${JENKINS_HOME}/summary/${envDir}/summary.txt
                """

                if (jobType == "Deploy") {
                    if (module == "Core" || module == "Kubernetes") {
                        image_tag = sh(returnStdout: true, script: 'jq -r .image_tag metadata.json').trim()
                        sh """
                            sed -i "s/${module}-${jobName}.*//g" ${JENKINS_HOME}/summary/${envDir}/summary.txt
                            sed -i "/^\\\$/d" ${JENKINS_HOME}/summary/${envDir}/summary.txt
                            echo "${jobType} : ${module}-${jobName} : ${image_tag} : privatebranch:${private_branch} : publicbranch:${public_branch}">> $JENKINS_HOME/summary/${envDir}/summary.txt
                        """
                    } else {
                        artifact_version = sh(returnStdout: true, script: 'jq -r .artifact_version metadata.json').trim()
                        sh """
                            sed -i "s/${module}-${jobName}.*//g" ${JENKINS_HOME}/summary/${envDir}/summary.txt
                            sed -i "/^\\\$/d" ${JENKINS_HOME}/summary/${envDir}/summary.txt
                            echo "${jobType}  :   ${module}-${jobName} :   ${artifact_version} : privatebranch:${private_branch} : publicbranch:${public_branch}" >> $JENKINS_HOME/summary/${envDir}/summary.txt
                        """
                    }
                }

                if (jobType == "Provision"  || jobType == "OpsAdministration") {
                     sh """
                        sed -i "s/${module}-${jobName}.*//g" ${JENKINS_HOME}/summary/${envDir}/summary.txt
                        sed -i "/^\\\$/d" ${JENKINS_HOME}/summary/${envDir}/summary.txt
                        echo "${jobType}  :   ${module}-${jobName} : privatebranch:${private_branch} : publicbranch:${public_branch}" >> $JENKINS_HOME/summary/${envDir}/summary.txt
                    """    
                }  
            }

            stage('Archive artifacts') {
                sh "cp ${JENKINS_HOME}/summary/${envDir}/summary.txt ."
                sh "sort summary.txt -o summary.txt && cat summary.txt"
                archiveArtifacts artifacts: 'summary.txt', fingerprint: true, onlyIfSuccessful: true
                currentBuild.description = "${module}-${jobName}"

            }
        }
    }

    catch (err){
        throw err
    }
}