//Global vars

// git
def gitUrl = 'git@github.ibm.com:zDevOps-Acceleration/dbb-zappbuild.git'
def gitCredId = 'drice-us'

// application
def GitBranch = 'development-aug2020-ztec201'
def zAppBuild = 'dbb-zappbuild-vanilla'

//dbb script from public github
def dbbGitRepo = 'https://github.com/IBM/dbb'
def dbbGitBranch = 'master'

// ucd configuration
def ucdComponent = 'MortgageApplication'
def artifactoryConfig = '/var/ucd/v6.2.5/agents/agentDev/conf/artifactrepository/MortgageApplication.artifactory.properties'

//system
def groovyz = '/var/dbb/v1.0.9.ifix1/bin/groovyz'

node (label: 'ztec-201-STC') {
    
    def workOutoutDir = "${WORKSPACE}/work"
    
    stage ('Cleanup') {
        // rm 
        dir("${WORKSPACE}/work"){deleteDir()}
    }
    
	stage('Git Checkout') {
		dir (zAppBuild) {
			checkout([$class: 'GitSCM', branches: [[name: GitBranch]], doGenerateSubmoduleConfigurations: false, submoduleCfg: [], userRemoteConfigs: [[credentialsId: gitCredId, url: gitUrl]]])
		}
		
		dir("dbb") {
                        sh(script: 'rm -f .git/info/sparse-checkout', returnStdout: true)
                        def scmVars =
                            checkout([$class: 'GitSCM', branches: [[name: dbbGitBranch]],
                              doGenerateSubmoduleConfigurations: false,
                              extensions: [
                                       [$class: 'SparseCheckoutPaths',  sparseCheckoutPaths:[[$class:'SparseCheckoutPath', path:'Pipeline']]]
                                    ],
                              submoduleCfg: [],
                            userRemoteConfigs: [[
                                url: dbbGitRepo,
                            ]]])
                    }
		
	}
	
	
	stage("Build") {
	    sh "${groovyz}  ${zAppBuild}/build.groovy --workspace ${WORKSPACE}/${zAppBuild}/samples --hlq DBEHM.ZAPP.CLEAN --workDir ${WORKSPACE}/BUILD-${BUILD_NUMBER} --application MortgageApplication --logEncoding UTF-8 --fullBuild --verbose"
		dir ("${WORKSPACE}/work") {
	    archiveArtifacts allowEmptyArchive: true, 
											artifacts: '*.log,*.json,*.html',  
											excludes: '*clist', 
											onlyIfSuccessful: false
	    }
		
	}

	stage("Run IDZ Code Review") {
		BUILD_OUTPUT_FOLDER = sh (script: "ls ${WORKSPACE}/BUILD-${BUILD_NUMBER}", returnStdout: true).trim()
	    sh "${groovyz} ${WORKSPACE}/dbb/Pipeline/RunIDZCodeReview/RunCodeReview.groovy --workDir ${WORKSPACE}/BUILD-${BUILD_NUMBER}/${BUILD_OUTPUT_FOLDER} --properties /var/dbb/extensions/idz-codereview/codereview.properties"
		
		dir ("${WORKSPACE}/work") {
	    archiveArtifacts allowEmptyArchive: true, 
											artifacts: '*.csv,*.xml',  
											excludes: '*clist', 
											onlyIfSuccessful: false
	    }
	}
	
	stage("Package") {
        sh "${groovyz} ${WORKSPACE}/dbb/Pipeline/CreateUCDComponentVersion/dbb-ucd-packaging.groovy --buztool /var/ucd/v6.2.5/agents/agentDev/bin/buztool.sh --workDir ${WORKSPACE}/work --component ${ucdComponent} --ar ${artifactoryConfig}"

		dir ("${WORKSPACE}/work") {
	    archiveArtifacts allowEmptyArchive: true, 
											artifacts: 'shiplist.xml',  
											excludes: '*clist', 
											onlyIfSuccessful: false
	    }
		
	}
	
//	stage("Run Deployment") {
//        sh "${groovyz} /var/dbb/integrations/ucd-deployment/UCDDeploy.groovy "
//	}
	
}
