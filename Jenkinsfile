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
					def llq = env.BRANCH_NAME.take(8).toUpperCase()
					sh "${groovyz} ${WORKSPACE}/dbb-zappbuild/test/test.groovy -b ${env.BRANCH_NAME}-a MortgageApplication -q JENKINS.DBB.TEST.BUILD.${llq} -u https://10.3.20.96:10443/dbb -i ADMIN -p ADMIN --propFiles /var/dbb/dbb-zappbuild-config/build.properties,/var/dbb/dbb-zappbuild-config/datasets.properties --outDir ${WORKSPACE}/testframework_out --verbose"
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
