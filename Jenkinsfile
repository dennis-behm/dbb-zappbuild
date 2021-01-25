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
def artifactoryConfig = '/var/ucd/agent/conf/artifactrepository/MortgageApplication.artifactory.properties'
def buztoolLocation = '/var/ucd/agent/bin/buztool.sh'

// UCD
def ucdApplication = 'MortgageApplication'
def ucdProcess = 'deploy'
//def ucdComponent = 'MortgageApplication'
def ucdEnv = 'INT'
def ucdSite = 'ztecEnv'
def ucdUri = 'https://10.3.20.233:8443/'


//system
def groovyz = '/usr/lpp/dbb/v1r0/bin/groovyz'

node (label: 'ztec-201-STC') {

    def workOutoutDir = "${WORKSPACE}/work"

    stage ('Cleanup') {
        // rm
        dir("${WORKSPACE}/work"){deleteDir()}
    }

	stage('Git Checkout') {
		dir (zAppBuild) {
			scmVars = checkout([$class: 'GitSCM', branches: [[name: env.BRANCH_NAME]], doGenerateSubmoduleConfigurations: false, submoduleCfg: [], userRemoteConfigs: [[credentialsId: gitCredId, url: gitUrl]]])
			env.GIT_COMMIT = scmVars.GIT_COMMIT
    			env.DATASET_BRANCH = env.BRANCH_NAME.take(8).toUpperCase()
    			env.COLLECTION_BRANCH = env.BRANCH_NAME.capitalize()
			
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
	    //sh "${groovyz}  ${zAppBuild}/build.groovy --workspace ${WORKSPACE}/${zAppBuild}/samples --hlq JENKINS.ZAPP.CLEAN --workDir ${WORKSPACE}/BUILD-${BUILD_NUMBER} --application MortgageApplication --logEncoding UTF-8 --reset --verbose"
	    sh "${groovyz}  ${zAppBuild}/build.groovy --workspace ${WORKSPACE}/${zAppBuild}/samples --hlq JENKINS.ZAPP.CLEAN.$DATASET_BRANCH --workDir ${WORKSPACE}/BUILD-${BUILD_NUMBER} --application MortgageApplication --logEncoding UTF-8 --fullBuild --verbose --url https://10.3.20.96:10443/dbb --id ADMIN --pw ADMIN"
	
	    //calculating the Buildoutput folder name
	
	    BUILD_OUTPUT_FOLDER = sh (script: "ls ${WORKSPACE}/BUILD-${BUILD_NUMBER}", returnStdout: true).trim()
		
		dir ("${WORKSPACE}/BUILD-${BUILD_NUMBER}/${BUILD_OUTPUT_FOLDER}") {
	    archiveArtifacts allowEmptyArchive: true,
											artifacts: '*.log,*.json,*.html,*.txt',
											excludes: '*clist',
											onlyIfSuccessful: false
	    }
		
	}

	stage("Run IDZ Code Review") {
	    sh "${groovyz} -Dlog4j.configurationFile=/var/dbb/config/log4j2.properties ${WORKSPACE}/dbb/Pipeline/RunIDZCodeReview/RunCodeReview.groovy --workDir ${WORKSPACE}/BUILD-${BUILD_NUMBER}/${BUILD_OUTPUT_FOLDER} --properties /var/dbb/extensions/idz-codereview/codereview.properties"
		
		dir ("${WORKSPACE}/BUILD-${BUILD_NUMBER}/${BUILD_OUTPUT_FOLDER}") {
		crContent = readFile file: "CodeReviewJUNIT.xml"
		 archiveArtifacts allowEmptyArchive: true,
											artifacts: '*.csv,*.xml',
											excludes: '*clist',
											onlyIfSuccessful: false
	    }
	    writeFile file: "${WORKSPACE}/BUILD-${BUILD_NUMBER}/CodeReviewJUNIT.xml", text:crContent.trim()
		junit allowEmptyResults: true, skipPublishingChecks: true, testResults: "BUILD-${BUILD_NUMBER}/CodeReviewJUNIT.xml"
	}
	
	stage("Package") {
//        sh "${groovyz} ${WORKSPACE}/dbb/Pipeline/CreateUCDComponentVersion/dbb-ucd-packaging.groovy --buztool ${buztoolLocation} --workDir ${WORKSPACE}/BUILD-${BUILD_NUMBER}/${BUILD_OUTPUT_FOLDER} --component ${ucdComponent} --prop ${artifactoryConfig}"
        sh "${groovyz} /var/dbb/extensions/ucd-packaging/dbb-ucd-packaging-dennis.groovy --buztool ${buztoolLocation} --workDir ${WORKSPACE}/BUILD-${BUILD_NUMBER}/${BUILD_OUTPUT_FOLDER} --component ${ucdComponent} --prop ${artifactoryConfig}"

		dir ("${WORKSPACE}/BUILD-${BUILD_NUMBER}/${BUILD_OUTPUT_FOLDER}") {
	    archiveArtifacts allowEmptyArchive: true,
											artifacts: 'shiplist.xml',
											excludes: '*clist',
											onlyIfSuccessful: false
	    }
		
	}
	
//	stage("Run Deployment") {
//        sh "${groovyz} /var/dbb/integrations/ucd-deployment/UCDDeploy.groovy "
//	}
	
	stage('Run UCD Deployment') {
         //   steps {
               // script{
               //     if ( hasBuildFiles ) {
                        script{
                          tee("UCD-DEPLOY-${BUILD_NUMBER}.log") {
								step(
									 [$class: 'UCDeployPublisher',
										deploy: [
											deployApp: ucdApplication,
											deployDesc: 'Requested from Jenkins',
											deployEnv: ucdEnv,
											deployOnlyChanged: false,
											deployProc: ucdProcess,
											deployVersions: ucdComponent + ':latest'],
										siteName: ucdSite])
							}
							def regex = java.util.regex.Pattern.compile("Deployment request id is: \'(.*)\'")
							def matcher = regex.matcher(readFile("UCD-DEPLOY-${BUILD_NUMBER}.log"))
							if (matcher.find()) {
								def requestUri = "${ucdUri}/#applicationProcessRequest/${matcher.group(1)}"
								echo "UCD Deployment request: ${requestUri}"
								createSummary icon:"star-gold.png", text: "<a href=\'$requestUri\' target=\'_other\'>UCD Deployment request</a>"
							}
                        }
                //    }
               // }
            //}
        }
	
}