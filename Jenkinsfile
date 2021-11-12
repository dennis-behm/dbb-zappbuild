pipeline {
	agent  { label "ztec-201-STC" }
	options { skipDefaultCheckout(true) }
	stages {
		stage('checkout') {
			steps {
				checkout scm
			}
		}
		
		stage('Invoke zAppBuild Testframework') {
			steps {
				script{
					
					// retrieve settings from Jenkins Agent Configuration
					def dbbHome = env.DBB_HOME
					def dbbUrl = env.DBB_URL // https://10.3.20.96:10443/dbb
					def dbbCredentialOptions = env.DBB_CREDENTIAL_OPTS //'-i ADMIN -p ADMIN'
					def dbbPropFilesOpts = env.DBB_PROP_FILES // '/var/dbb/dbb-zappbuild-config/build.properties,/var/dbb/dbb-zappbuild-config/datasets.properties'
										
					def llq = env.BRANCH_NAME.take(8).toUpperCase()
					sh "${dbbHome}/bin/groovyz ${WORKSPACE}/test/test.groovy -b ${env.BRANCH_NAME} -a MortgageApplication -q JENKINS.DBB.TEST.BUILD.${llq} -u ${dbbUrl} ${dbbCredentialOptions} --propFiles ${dbbPropFilesOpts} --outDir ${WORKSPACE}/testframework_out --verbose"
				}
			}
			post { always {
				dir ("${WORKSPACE}/testframework_out") {
					archiveArtifacts allowEmptyArchive: true,
										artifacts: '*.log,*.json,*.html,*.txt',
										excludes: '*clist',
										onlyIfSuccessful: false
					}
				}
			}
		}
	}
}
