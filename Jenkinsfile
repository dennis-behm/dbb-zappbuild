/**
 * Declarative Pipeline script to drive automated verifications of the zAppBuild contributions through the zAppBuild Test Framework
 * 
 */

pipeline {
	
	agent  { label "ztec-201-STC" }
	options { skipDefaultCheckout(true) }
	stages {
		// Checkout the current configuration (branch / PR)
		stage('checkout') {
			steps {
				checkout scm
			}
		}
		
		// Invoke the zAppBuild Testframework. See zAppBuild repository /test for more details on the framework.
		stage('Invoke zAppBuild Testframework') {
			steps {
				script{
					
					// retrieve DBB Build settings from Jenkins Agent Configuration
					def dbbHome = env.DBB_HOME
					def dbbUrl = env.DBB_URL // https://10.3.20.96:10443/dbb
					def dbbCredentialOptions = env.DBB_CREDENTIAL_OPTS //'-i ADMIN -p ADMIN'
					def dbbPropFilesOpts = env.DBB_PROP_FILES // '/var/dbb/dbb-zappbuild-config/build.properties,/var/dbb/dbb-zappbuild-config/datasets.properties'
										
					def llq = env.BRANCH_NAME.take(8).toUpperCase()
					sh "${dbbHome}/bin/groovyz ${WORKSPACE}/test/test.groovy -b ${env.BRANCH_NAME} -a MortgageApplication -q JENKINS.DBB.TEST.BUILD.${llq} -u ${dbbUrl} ${dbbCredentialOptions} --propFiles ${dbbPropFilesOpts} --outDir ${WORKSPACE}/testframework_out --verbose"
				}
			}
		}
	}
}
