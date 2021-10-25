pipeline {
	agent  { label "ztec-201-STC" }
	options { skipDefaultCheckout(true) }
	stages {
		stage('init') {
			steps {
				sh "echo 'Hello World'"
				
				checkout scm
			}
		}
	}
}
